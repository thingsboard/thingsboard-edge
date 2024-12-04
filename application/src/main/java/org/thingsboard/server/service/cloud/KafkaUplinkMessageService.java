/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.cloud.CloudEventService;

@Slf4j
public abstract class KafkaUplinkMessageService extends BaseUplinkMessageService {
    @Autowired
    protected CloudEventService cloudEventService;

    public void processHandleMessages(TenantId tenantId) {
        PageData<CloudEvent> newCloudEvents = findNewCloudEvents(tenantId);

        if (newCloudEvents.getTotalElements() != 0) {
            processCloudEvents(tenantId, newCloudEvents);
        }
    }

    private PageData<CloudEvent> findNewCloudEvents(TenantId tenantId) {
        boolean hasNewGeneralCloudEvent =
                this.getClass() == KafkaGeneralUplinkMessageService.class &&
                        ((KafkaGeneralUplinkMessageService) this).newCloudEvents.getTotalElements() != 0;

        return hasNewGeneralCloudEvent ? ((KafkaGeneralUplinkMessageService) this).newCloudEvents : findCloudEvents(tenantId);
    }

    public void processCloudEvents(TenantId tenantId, PageData<CloudEvent> newCloudEvents) {
        PageData<CloudEvent> cloudEvents = null;

        do {
            cloudEvents = cloudEvents == null ? newCloudEvents : findCloudEvents(tenantId);
            sendCloudEvents(cloudEvents);
        } while (isProcessContinue(tenantId, cloudEvents));
    }

    private boolean isProcessContinue(TenantId tenantId, PageData<CloudEvent> cloudEvents) {
        return super.isProcessContinue(tenantId) && !cloudEvents.getData().isEmpty();
    }

    protected abstract PageData<CloudEvent> findCloudEvents(TenantId tenantId);

}
