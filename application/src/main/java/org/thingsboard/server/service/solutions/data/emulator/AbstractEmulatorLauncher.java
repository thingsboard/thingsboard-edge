/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public abstract class AbstractEmulatorLauncher<T extends GroupEntity<?>> {

    protected static final TbQueueCallback EMPTY_CALLBACK = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {

        }

        @Override
        public void onFailure(Throwable t) {

        }
    };
    protected final T entity;
    protected final EmulatorDefinition emulatorDefinition;
    protected final ExecutorService oldTelemetryExecutor;
    protected final TbClusterService tbClusterService;
    protected final PartitionService partitionService;
    protected final TbQueueProducerProvider tbQueueProducerProvider;
    protected final TbServiceInfoProvider serviceInfoProvider;
    protected final TelemetrySubscriptionService tsSubService;
    protected final long publishFrequency;

    private final Emulator emulator;
    private ScheduledFuture<?> scheduledFuture;

    public AbstractEmulatorLauncher(T entity, EmulatorDefinition emulatorDefinition, ExecutorService oldTelemetryExecutor, TbClusterService tbClusterService,
                                    PartitionService partitionService,
                                    TbQueueProducerProvider tbQueueProducerProvider,
                                    TbServiceInfoProvider serviceInfoProvider,
                                    TelemetrySubscriptionService tsSubService) throws Exception {
        this.entity = entity;
        this.emulatorDefinition = emulatorDefinition;
        this.oldTelemetryExecutor = oldTelemetryExecutor;
        this.tbClusterService = tbClusterService;
        this.partitionService = partitionService;
        this.tbQueueProducerProvider = tbQueueProducerProvider;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tsSubService = tsSubService;
        this.publishFrequency = TimeUnit.SECONDS.toMillis(emulatorDefinition.getPublishFrequencyInSeconds());
        if (StringUtils.isEmpty(emulatorDefinition.getClazz())) {
            emulator = new BasicEmulator();
        } else {
            emulator = (Emulator) Class.forName(emulatorDefinition.getClazz()).getDeclaredConstructor().newInstance();
        }
        emulator.init(emulatorDefinition);
    }

    public void launch() {
        final long latestTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(emulatorDefinition.getPublishPeriodInDays()) - publishFrequency;
        oldTelemetryExecutor.submit(() -> {
            try {
                if (emulator instanceof SimpleEmulator) {
                    if (latestTs < (System.currentTimeMillis() - publishFrequency)) {
                        pushOldTelemetry(latestTs);
                    }
                } else if (emulator instanceof CustomEmulator) {
                    Pair<Long, ObjectNode> telemetry = ((CustomEmulator) emulator).getNextValue();
                    while (telemetry != null) {
                        publishTelemetry(telemetry.getFirst(), telemetry.getSecond());
                        telemetry = ((CustomEmulator) emulator).getNextValue();
                    }
                }

                postProcessEntity(entity);
            } catch (Exception e) {
                log.warn("[{}] Failed to upload telemetry for device: ", entity.getName(), e);
            }
        });
    }

    protected void postProcessEntity(T entity) {
    }

    private void pushOldTelemetry(long latestTs) throws InterruptedException {
        for (long ts = latestTs; ts < System.currentTimeMillis(); ts += publishFrequency) {
            publishTelemetry(ts);
        }
    }

    private void publishTelemetry(long ts) throws InterruptedException {
        ObjectNode values = ((SimpleEmulator) emulator).getValue(ts);
        publishTelemetry(ts, values);
        Thread.sleep(emulatorDefinition.getPublishPauseInMillis());
    }

    public void stop() {
        scheduledFuture.cancel(true);
    }

    private void publishTelemetry(long ts, ObjectNode value) {
        String msgData = JacksonUtil.toString(value);
        log.debug("[{}] Publishing telemetry: {}", entity.getName(), msgData);
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("ts", Long.toString(ts));
        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), entity.getId(), md, TbMsgDataType.JSON, msgData);
        tbClusterService.pushMsgToRuleEngine(entity.getTenantId(), entity.getId(), tbMsg, null);
    }
}
