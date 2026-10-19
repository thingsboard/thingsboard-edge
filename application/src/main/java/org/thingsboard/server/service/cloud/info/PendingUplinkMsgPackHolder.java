// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.info;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@Data
public class PendingUplinkMsgPackHolder {

    @Value("${cloud.uplink_pack_timeout_sec:60}")
    private long uplinkPackTimeoutSec;

    private final ConcurrentMap<Integer, UplinkMsg> pendingMsgMap;
    private CountDownLatch latch;

    public PendingUplinkMsgPackHolder() {
        this.pendingMsgMap = new ConcurrentHashMap<>();
    }

    public void markAsProcessed(Integer uplinkMsgId) {
        pendingMsgMap.remove(uplinkMsgId);
        latch.countDown();
    }

    public void countDown() {
        latch.countDown();
    }

    public boolean awaitBatchCompletion() throws InterruptedException {
        return latch.await(uplinkPackTimeoutSec, TimeUnit.SECONDS);
    }

    public void startPendingBatch() {
        latch = new CountDownLatch(pendingMsgMap.size());
    }

    public LinkedBlockingQueue<UplinkMsg> getQueue() {
        return new LinkedBlockingQueue<>(getValues());
    }

    public Collection<UplinkMsg> getValues() {
        return pendingMsgMap.values();
    }

    public void setNewPack(List<UplinkMsg> uplinkMsgPack) {
        pendingMsgMap.clear();
        uplinkMsgPack.forEach(msg -> pendingMsgMap.put(msg.getUplinkMsgId(), msg));
    }

    public void clear() {
        pendingMsgMap.clear();
    }

    public int getQueueSize() {
        return pendingMsgMap.size();
    }

    public boolean isQueueEmpty() {
        return pendingMsgMap.isEmpty();
    }
}
