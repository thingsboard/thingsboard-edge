// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
