/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud.info;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

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

    // todo: atomic booleans?
    private volatile boolean generalProcessInProgress;
    private volatile boolean initialized;
    private volatile boolean initInProgress;
    private volatile boolean syncInProgress;
    private volatile boolean sendingInProgress;
    private volatile boolean isRateLimitViolated;
    private volatile boolean performInitialSyncRequired = true;

    private final Lock uplinkSendLock;

    public EdgeInfoHolder() {
        this.uplinkSendLock = new ReentrantLock();
    }

    public void lockSend() {
        uplinkSendLock.lock();
    }

    public void unlockSend() {
        uplinkSendLock.unlock();
    }
}
