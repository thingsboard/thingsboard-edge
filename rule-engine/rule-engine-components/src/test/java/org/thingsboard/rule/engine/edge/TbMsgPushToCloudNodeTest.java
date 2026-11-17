// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.edge;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.cloud.CloudEventService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbMsgPushToCloudNodeTest {

    private static final List<TbMsgType> TYPES = List.of(TbMsgType.POST_TELEMETRY_REQUEST, TbMsgType.POST_ATTRIBUTES_REQUEST, TbMsgType.ATTRIBUTES_UPDATED);

    private TbMsgPushToCloudNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;
    @Mock
    private CloudEventService cloudEventService;
    @Mock
    private ListeningExecutor dbCallbackExecutor;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbMsgPushToCloudNode();
        TbMsgPushToCloudNodeConfiguration config = new TbMsgPushToCloudNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void givenTypeCastDisabledKeys_whenPushTelemetryAndAttributes_thenCloudEventBodyCarriesTheKeys() {
        for (var type : TYPES) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putTypeCastDisabledKeys(List.of("joinEui"));

            CloudEvent cloudEvent = pushAndCaptureEvent(type, metaData, "{\"joinEui\":\"0000000000000000\",\"temperature\":\"21.5\"}");

            String dataKey = type == TbMsgType.POST_TELEMETRY_REQUEST ? "data" : "kv";
            assertThat(cloudEvent.getAction()).isEqualTo(expectedAction(type));
            assertThat(cloudEvent.getEntityBody().get(dataKey).get("joinEui").asText()).isEqualTo("0000000000000000");
            assertThat(JacksonUtil.convertValue(cloudEvent.getEntityBody().get(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS), String[].class))
                    .as("type cast disabled keys of %s", type)
                    .containsExactly("joinEui");
        }
    }

    @Test
    public void givenNoTypeCastDisabledKeys_whenPushTelemetryAndAttributes_thenCloudEventBodyIsUnchanged() {
        for (var type : TYPES) {
            CloudEvent cloudEvent = pushAndCaptureEvent(type, new TbMsgMetaData(), "{\"temperature\":\"21.5\"}");

            assertThat(cloudEvent.getEntityBody().has(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS))
                    .as("type cast disabled keys of %s", type)
                    .isFalse();
        }
    }

    private static EdgeEventActionType expectedAction(TbMsgType type) {
        return switch (type) {
            case POST_TELEMETRY_REQUEST -> EdgeEventActionType.TIMESERIES_UPDATED;
            case POST_ATTRIBUTES_REQUEST -> EdgeEventActionType.POST_ATTRIBUTES;
            default -> EdgeEventActionType.ATTRIBUTES_UPDATED;
        };
    }

    private CloudEvent pushAndCaptureEvent(TbMsgType type, TbMsgMetaData metaData, String data) {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getCloudEventService()).thenReturn(cloudEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(cloudEventService.saveTsKvAsync(any())).thenReturn(SettableFuture.create());

        TbMsg msg = TbMsg.newMsg()
                .type(type)
                .originator(deviceId)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(data)
                .build();

        node.onMsg(ctx, msg);

        ArgumentCaptor<CloudEvent> eventCaptor = ArgumentCaptor.forClass(CloudEvent.class);
        verify(cloudEventService).saveTsKvAsync(eventCaptor.capture());
        Mockito.reset(ctx, cloudEventService);
        return eventCaptor.getValue();
    }

}
