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
package org.thingsboard.server.service.cloud.event.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.eventsourcing.ResetQueueOffsetEvent;
import org.thingsboard.server.service.cloud.info.EdgeInfoHolder;

import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_START_TS_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY;
import static org.thingsboard.server.common.data.edge.EdgeConstants.QUEUE_TS_KV_START_TS_ATTR_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresResetQueueOffsetEventHandler {

    private final AttributesService attributesService;
    private final EdgeInfoHolder edgeInfo;

    @EventListener(ResetQueueOffsetEvent.class)
    public void onEvent() {
        updateQueueStartTsSeqIdOffset(edgeInfo.getTenantId(), QUEUE_START_TS_ATTR_KEY, QUEUE_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
        updateQueueStartTsSeqIdOffset(edgeInfo.getTenantId(), QUEUE_TS_KV_START_TS_ATTR_KEY, QUEUE_TS_KV_SEQ_ID_OFFSET_ATTR_KEY, System.currentTimeMillis(), 0L);
    }

    public void updateQueueStartTsSeqIdOffset(TenantId tenantId, String attrStartTsKey, String attrSeqIdKey, Long startTs, Long seqIdOffset) {
        log.trace("updateQueueStartTsSeqIdOffset [{}][{}][{}][{}]", attrStartTsKey, attrSeqIdKey, startTs, seqIdOffset);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(attrStartTsKey, startTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(attrSeqIdKey, seqIdOffset), System.currentTimeMillis())
        );
        try {
            attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes).get();
        } catch (Exception e) {
            log.error("Failed to update queueStartTsSeqIdOffset [{}][{}]", attrStartTsKey, attrSeqIdKey, e);
        }
    }
}
