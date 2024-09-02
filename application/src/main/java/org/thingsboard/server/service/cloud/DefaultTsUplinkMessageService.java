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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@Slf4j
@Service
public class DefaultTsUplinkMessageService extends BaseUplinkMessageService implements TsUplinkMessageService {

    private static final String QUEUE_TS_KV_START_TS_ATTR_KEY = "queueTsKvStartTs";
    private static final String QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY = "queueTsKvSeqIdOffset";

    @Autowired
    private GeneralUplinkMessageService generalUplinkMessageService;

    @Override
    protected PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        return cloudEventService.findTsKvCloudEvents(tenantId, seqIdStart, seqIdEnd, pageLink);
    }

    @Override
    protected String getTableName() {
        return "ts_kv_cloud_event";
    }

    @Override
    protected boolean newMessagesAvailableInGeneralQueue(TenantId tenantId) {
        try {
            Long cloudEventsQueueSeqIdStart = getLongAttrByKey(tenantId, QUEUE_SEQ_ID_OFFSET_ATTR_KEY).get();
            TimePageLink cloudEventsPageLink = generalUplinkMessageService.newCloudEventsAvailable(tenantId, cloudEventsQueueSeqIdStart);
            return cloudEventsPageLink != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void updateQueueStartTsSeqIdOffset(TenantId tenantId, Long newStartTs, Long newSeqId) {
        updateQueueStartTsSeqIdOffset(tenantId, QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, newStartTs, newSeqId);
    }

    @Override
    protected ListenableFuture<Long> getQueueStartTs(TenantId tenantId) {
        return getLongAttrByKey(tenantId, QUEUE_TS_KV_START_TS_ATTR_KEY);
    }

    @Override
    protected ListenableFuture<Long> getQueueSeqIdStart(TenantId tenantId) {
        return getLongAttrByKey(tenantId, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY);
    }

}
