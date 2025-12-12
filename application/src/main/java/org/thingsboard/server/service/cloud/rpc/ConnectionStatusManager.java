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
package org.thingsboard.server.service.cloud.rpc;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionStatusManager {

    private final TelemetrySubscriptionService tsSubService;
    private final EdgeInfoHolder edgeInfoHolder;

    public void updateConnectivityStatus(boolean activityState) {
        TenantId tenantId = edgeInfoHolder.getTenantId();

        if (tenantId != null) {
            save(tenantId, DefaultDeviceStateService.ACTIVITY_STATE, activityState);
            if (activityState) {
                save(tenantId, DefaultDeviceStateService.LAST_CONNECT_TIME, System.currentTimeMillis());
            } else {
                save(tenantId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, System.currentTimeMillis());
            }
        }
    }

    private void save(TenantId tenantId, String key, long value) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(tenantId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new LongDataEntry(key, value))
                .notifyDevice(true)
                .callback(new AttributeSaveCallback(key, value))
                .build());
    }

    private void save(TenantId tenantId, String key, boolean value) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(tenantId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new BooleanDataEntry(key, value))
                .notifyDevice(true)
                .callback(new AttributeSaveCallback(key, value))
                .build());
    }

    private record AttributeSaveCallback(String key, Object value) implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            log.trace("Successfully updated attribute [{}] with value [{}]", key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update attribute [{}] with value [{}]", key, value, t);
        }

    }
}
