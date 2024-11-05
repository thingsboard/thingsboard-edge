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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaTSUplinkMessageService extends KafkaUplinkMessageService implements TsUplinkMessageService {

    @Autowired
    private GeneralUplinkMessageService generalUplinkMessageService;

    @Override
    protected PageData<CloudEvent> findCloudEvents(TenantId tenantId) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findTsKvCloudEvents(tenantId, null, null, null);

        cloudEventService.commit(true);

        return cloudEvents;
    }

    @Override
    protected boolean newMessagesAvailableInGeneralQueue(TenantId tenantId) {
        TimePageLink timePageLink = generalUplinkMessageService.newCloudEventsAvailable(tenantId, 0L);

        return timePageLink != null;
    }
}
