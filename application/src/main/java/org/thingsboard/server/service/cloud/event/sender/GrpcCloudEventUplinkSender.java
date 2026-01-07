/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud.event.sender;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.dao.edge.stats.CloudStatsCounterService;
import org.thingsboard.server.dao.edge.stats.CloudStatsKey;
import org.thingsboard.server.dao.eventsourcing.InterruptSendUplinkEvent;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.cloud.event.UplinkMsgMapper;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.cloud.info.PendingUplinkMsgPackHolder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.GrpcClientManager;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class GrpcCloudEventUplinkSender implements CloudEventUplinkSender, CloudEventUplinkInterrupter {

    private static final int MAX_SEND_UPLINK_ATTEMPTS = 3;

    private final EdgeInfoHolder edgeInfo;
    private final PendingUplinkMsgPackHolder pendingMsgs;
    private final UplinkMsgMapper uplinkMsgMapper;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    private final CloudStatsCounterService statsCounterService;
    private final GrpcClientManager grpcClientManager;

    private Future<?> sendUplinkFuture;
    private SettableFuture<Boolean> sendUplinkFutureResult;
    private ExecutorService uplinkExecutor;

    public void init() {
        uplinkExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("cloud-manager-uplink"));
    }

    @PreDestroy
    public void shutdown() {
        if (uplinkExecutor != null) {
            uplinkExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Boolean> sendCloudEvents(List<CloudEvent> cloudEvents, boolean isGeneralMsg) {
        edgeInfo.lockSend();
        try {
            if (!isGeneralMsg && edgeInfo.isGeneralProcessInProgress()) {
                return Futures.immediateFuture(true);
            }
            interruptPreviousSendUplinkMsgsTask();
            sendUplinkFutureResult = SettableFuture.create();

            cloudEvents = EdgeMsgConstructorUtils.mergeAndFilterUplinkDuplicates(cloudEvents);

            List<UplinkMsg> uplinkMsgPack = uplinkMsgMapper.convertCloudEventsToUplink(cloudEvents);

            if (uplinkMsgPack.isEmpty()) {
                return Futures.immediateFuture(false);
            }

            processMsgPack(uplinkMsgPack, isGeneralMsg);
        } finally {
            edgeInfo.unlockSend();
        }
        return sendUplinkFutureResult;
    }

    @Override
    @EventListener(InterruptSendUplinkEvent.class)
    public void interruptPreviousSendUplinkMsgsTask() {
        if (sendUplinkFutureResult != null) {
            try {
                // wait before interrupting sending previous uplink pack
                sendUplinkFutureResult.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            if (!sendUplinkFutureResult.isDone()) {
                log.debug("[{}] Stopping send uplink future now!", edgeInfo.getTenantId());
                sendUplinkFutureResult.set(true);
            }
        }
        if (sendUplinkFuture != null && !sendUplinkFuture.isCancelled()) {
            sendUplinkFuture.cancel(true);
            sendUplinkFuture = null;
        }
    }

    private void processMsgPack(List<UplinkMsg> uplinkMsgPack, boolean isGeneralMsg) {
        pendingMsgs.setNewPack(uplinkMsgPack);

        sendUplinkFuture = uplinkExecutor.submit(() -> {
            try {
                int attempt = 1;
                boolean success;
                do {
                    log.trace("[{}] uplink msg(s) are going to be send.", pendingMsgs.getQueueSize());

                    long startTime = System.currentTimeMillis();
                    success = sendUplinkMsgPack() && pendingMsgs.isQueueEmpty();

                    if (!success) {
                        String batchPrefix = isGeneralMsg ? "General" : "Timeseries";
                        log.warn("Failed to deliver {} batch (size: {}) on attempt {}", batchPrefix, pendingMsgs.getQueueSize(), attempt);
                        log.trace("Entities in failed batch: {}", pendingMsgs.getValues());
                        try {
                            Thread.sleep(cloudEventStorageSettings.getSleepIntervalBetweenBatches());

                            if (edgeInfo.clearRateLimitViolationIfSet()) {
                                TimeUnit.SECONDS.sleep(60);
                            }
                        } catch (InterruptedException e) {
                            log.error("Error during sleep between batches or on rate limit violation", e);
                        }
                    } else {
                        log.info("Sending of [{}] uplink msg(s) took {} ms.", uplinkMsgPack.size(), System.currentTimeMillis() - startTime);
                    }

                    attempt++;

                    if (isGeneralMsg && attempt > MAX_SEND_UPLINK_ATTEMPTS) {
                        log.warn("Failed to deliver the batch: after {} attempts. Next messages are going to be discarded {}",
                                MAX_SEND_UPLINK_ATTEMPTS, pendingMsgs.getValues());
                        statsCounterService.recordEvent(CloudStatsKey.UPLINK_MSGS_PERMANENTLY_FAILED, edgeInfo.getTenantId(), pendingMsgs.getQueueSize());
                        sendUplinkFutureResult.set(false);
                        return;
                    }
                } while (!success);
                sendUplinkFutureResult.set(false);
            } catch (Exception e) {
                sendUplinkFutureResult.set(true);
                log.error("Error during send uplink msg", e);
            }
        });
    }

    private boolean sendUplinkMsgPack() {
        edgeInfo.setSendingInProgress(true);
        LinkedBlockingQueue<UplinkMsg> orderedPendingMsgQueue = pendingMsgs.getQueue();
        try {
            pendingMsgs.startPendingBatch();
            orderedPendingMsgQueue.forEach(grpcClientManager::sendUplinkMsg);

            return pendingMsgs.awaitBatchCompletion();
        } catch (Exception e) {
            log.error("Interrupted while waiting for latch, isGeneralProcessInProgress={}", edgeInfo.isGeneralProcessInProgress(), e);
            for (UplinkMsg value : pendingMsgs.getValues()) {
                log.warn("Message not send due to exception: {}", value);
            }
            return false;
        } finally {
            edgeInfo.setSendingInProgress(false);
        }
    }
}
