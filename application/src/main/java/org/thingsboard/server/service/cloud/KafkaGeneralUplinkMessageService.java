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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaGeneralUplinkMessageService extends KafkaUplinkMessageService implements GeneralUplinkMessageService {
    public PageData<CloudEvent> newCloudEvents = new PageData<>();

    @Override
    protected PageData<CloudEvent> findCloudEvents(TenantId tenantId) {
        return cloudEventService.findCloudEvents(tenantId, null, null, null);
    }

    @Override
    public TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findCloudEvents(tenantId, null, null, null);

        newCloudEvents = cloudEvents;

        return cloudEvents.getTotalElements() != 0 ? new TimePageLink(0) : null;
    }

    @Override
    public ListenableFuture<Long> getQueueStartTs(TenantId tenantId) {
        SettableFuture<Long> futureToSet = SettableFuture.create();

        futureToSet.set(0L);

        return futureToSet;
    }

    @Override
    protected boolean newMessagesAvailableInGeneralQueue(TenantId tenantId) {
        return false;
    }
}
