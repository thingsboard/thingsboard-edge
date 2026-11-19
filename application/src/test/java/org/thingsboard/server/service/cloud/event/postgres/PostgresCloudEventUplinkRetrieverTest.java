// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.postgres;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.service.cloud.CloudContextComponent;
import org.thingsboard.server.service.cloud.CloudEventFinder;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class PostgresCloudEventUplinkRetrieverTest {

    private static final String KEY = "queueStartTs";
    private static final int MAX_READ_RECORDS_COUNT = 50;
    private static final long MISORDERING_COMPENSATION_MILLIS = 60000L;
    private static final Long QUEUE_SEQ_ID_START = 51L;

    @Mock
    private DbCallbackExecutorService dbCallbackExecutorService;
    @Mock
    private CloudEventStorageSettings cloudEventStorageSettings;
    @Mock
    private AttributesService attributesService;
    @Mock
    private CloudContextComponent cloudCtx;
    @Mock
    private CloudEventFinder finder;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    private PostgresCloudEventUplinkRetriever retriever;
    private long lastProcessedTs;

    @BeforeEach
    public void setUp() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(dbCallbackExecutorService).execute(any(Runnable.class));
        lenient().when(cloudEventStorageSettings.getMaxReadRecordsCount()).thenReturn(MAX_READ_RECORDS_COUNT);
        lenient().when(cloudEventStorageSettings.getMisorderingCompensationMillis()).thenReturn(MISORDERING_COMPENSATION_MILLIS);
        lenient().when(cloudCtx.getCloudEventStorageSettings()).thenReturn(cloudEventStorageSettings);
        lastProcessedTs = System.currentTimeMillis();
        lenient().when(attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, KEY))
                .thenReturn(Futures.immediateFuture(Optional.of(new BaseAttributeKvEntry(new LongDataEntry(KEY, lastProcessedTs), lastProcessedTs))));
        retriever = new PostgresCloudEventUplinkRetriever(dbCallbackExecutorService, cloudEventStorageSettings, attributesService, cloudCtx);
    }

    @Test
    public void youngTableIsNotANewCycle() throws ExecutionException, InterruptedException {
        stubFinder(seqOneEvent(lastProcessedTs - 30_000));

        assertThat(retriever.newCloudEventsAvailable(tenantId, QUEUE_SEQ_ID_START, KEY, finder)).isNull();
        assertThat(retriever.findCloudEventsFromBeginning(tenantId, QUEUE_SEQ_ID_START, KEY, pageLink(), finder).getData()).isEmpty();
    }

    @Test
    public void wrappedSequenceIsANewCycle() throws ExecutionException, InterruptedException {
        CloudEvent wrapped = seqOneEvent(lastProcessedTs);
        stubFinder(wrapped);

        assertThat(retriever.newCloudEventsAvailable(tenantId, QUEUE_SEQ_ID_START, KEY, finder)).isNotNull();
        assertThat(retriever.findCloudEventsFromBeginning(tenantId, QUEUE_SEQ_ID_START, KEY, pageLink(), finder).getData()).containsExactly(wrapped);
    }

    private void stubFinder(CloudEvent fromBeginning) {
        lenient().when(finder.find(eq(tenantId), eq(QUEUE_SEQ_ID_START), eq(null), any(TimePageLink.class))).thenReturn(new PageData<>());
        lenient().when(finder.find(eq(tenantId), eq(0L), eq(50L), any(TimePageLink.class)))
                .thenReturn(new PageData<>(List.of(fromBeginning), 1, 1, false));
    }

    private CloudEvent seqOneEvent(long createdTime) {
        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setSeqId(1);
        cloudEvent.setCreatedTime(createdTime);
        return cloudEvent;
    }

    private TimePageLink pageLink() {
        return new TimePageLink(MAX_READ_RECORDS_COUNT, 0, null, null, 0L, System.currentTimeMillis());
    }
}
