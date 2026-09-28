// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.postgres;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.service.cloud.CloudContextComponent;
import org.thingsboard.server.service.cloud.CloudEventFinder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresCloudEventUplinkRetriever {

    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final CloudEventStorageSettings cloudEventStorageSettings;
    private final AttributesService attributesService;
    private final CloudContextComponent cloudCtx;

    public TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart, String key, CloudEventFinder finder) {
        try {
            long lastProcessedTs = getLongAttrByKey(tenantId, key).get();
            // Subtract MISORDERING_COMPENSATION_MILLIS to ensure no events are missed in a clustered environment.
            // While events are identified using seqId, we use partitioning for performance reasons
            // and partitioning is based on created_time.
            long queueStartTs = lastProcessedTs > 0 ? lastProcessedTs - cloudCtx.getCloudEventStorageSettings().getMisorderingCompensationMillis() : 0;
            long queueEndTs = queueStartTs > 0 ? queueStartTs + TimeUnit.DAYS.toMillis(1) : System.currentTimeMillis();
            log.trace("newCloudEventsAvailable, queueSeqIdStart = {}, key = {}, queueStartTs = {}, queueEndTs = {}",
                    queueSeqIdStart, key, queueStartTs, queueEndTs);
            TimePageLink pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                    0, null, null, queueStartTs, queueEndTs);
            PageData<CloudEvent> cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
            if (cloudEvents.getData().isEmpty()) {
                if (!findCloudEventsFromBeginning(tenantId, queueSeqIdStart, lastProcessedTs, pageLink, finder).getData().isEmpty()) {
                    log.info("newCloudEventsAvailable: new cycle started (seq_id starts from '1')!");
                    return pageLink;
                }
                while (queueEndTs < System.currentTimeMillis()) {
                    log.trace("newCloudEventsAvailable: queueEndTs < System.currentTimeMillis() [{}] [{}]", queueEndTs, System.currentTimeMillis());
                    queueStartTs = queueEndTs;
                    queueEndTs = queueEndTs + TimeUnit.DAYS.toMillis(1);
                    pageLink = new TimePageLink(cloudEventStorageSettings.getMaxReadRecordsCount(),
                            0, null, null, queueStartTs, queueEndTs);
                    cloudEvents = finder.find(tenantId, queueSeqIdStart, null, pageLink);
                    if (!cloudEvents.getData().isEmpty()) {
                        return pageLink;
                    }
                }
                return null;
            } else {
                return pageLink;
            }
        } catch (Exception e) {
            log.warn("Failed to check newCloudEventsAvailable!", e);
            return null;
        }
    }

    public PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, Long queueSeqIdStart, String queueStartTsAttrKey, TimePageLink pageLink, CloudEventFinder finder) throws ExecutionException, InterruptedException {
        return findCloudEventsFromBeginning(tenantId, queueSeqIdStart, getLongAttrByKey(tenantId, queueStartTsAttrKey).get(), pageLink, finder);
    }

    // a wrapped sequence hands out seq_id 1 after the last processed event, a young table handed it out before
    private PageData<CloudEvent> findCloudEventsFromBeginning(TenantId tenantId, Long queueSeqIdStart, long lastProcessedTs, TimePageLink pageLink, CloudEventFinder finder) {
        if (queueSeqIdStart <= cloudEventStorageSettings.getMaxReadRecordsCount()) {
            return new PageData<>();
        }
        long seqIdEnd = Math.max(Integer.toUnsignedLong(cloudEventStorageSettings.getMaxReadRecordsCount()), 50L);
        PageData<CloudEvent> cloudEvents = finder.find(tenantId, 0L, seqIdEnd, pageLink);
        if (cloudEvents.getData().stream().anyMatch(ce -> ce.getSeqId() == 1 && ce.getCreatedTime() >= lastProcessedTs)) {
            return cloudEvents;
        }
        return new PageData<>();
    }

    private ListenableFuture<Long> getLongAttrByKey(TenantId tenantId, String attrKey) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKey);
        return Futures.transform(future, attributeKvEntryOpt -> {
            if (attributeKvEntryOpt != null && attributeKvEntryOpt.isPresent()) {
                AttributeKvEntry attributeKvEntry = attributeKvEntryOpt.get();
                return attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
            } else {
                return 0L;
            }
        }, dbCallbackExecutorService);
    }
}
