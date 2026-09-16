// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.info;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Component
@Data
public class EdgeInfoHolder {

    @Value("${cloud.routingKey}")
    private String routingKey;
    @Value("${cloud.secret}")
    private String routingSecret;
    @Value("${cloud.reconnect_timeout}")
    private long reconnectTimeoutMs;

    private TenantId tenantId;
    private CustomerId customerId;
    private EdgeSettings settings;

    private volatile boolean generalProcessInProgress;
    private volatile boolean initialized;
    private volatile boolean initInProgress;
    private volatile boolean syncInProgress;
    private volatile boolean sendingInProgress;
    private AtomicBoolean isRateLimitViolated = new AtomicBoolean(false);
    private volatile boolean performInitialSyncRequired = true;

    private final Lock uplinkSendLock;

    public EdgeInfoHolder() {
        this.uplinkSendLock = new ReentrantLock();
    }

    public void resetProcessingFlags() {
        this.generalProcessInProgress = false;
        this.initialized = false;
        this.initInProgress = false;
        this.syncInProgress = false;
        this.sendingInProgress = false;
    }

    public void lockSend() {
        uplinkSendLock.lock();
    }

    public void unlockSend() {
        uplinkSendLock.unlock();
    }

    public void setRateLimitViolated(boolean rateLimitViolated) {
        isRateLimitViolated.set(rateLimitViolated);
    }

    public boolean clearRateLimitViolationIfSet() {
        return isRateLimitViolated.compareAndSet(true, false);
    }
}
