/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.CloudUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to cloud",
        configClazz = TbMsgPushToCloudNodeConfiguration.class,
        nodeDescription = "Pushes messages from edge to cloud",
        nodeDetails = "Push messages from edge to cloud. " +
                "This node used only on edge to push messages from edge to cloud. " +
                "Once message arrived into this node it’s going to be converted into cloud event and saved to the local database. " +
                "Node doesn't push messages directly to cloud, but stores event(s) in the cloud queue. " +
                "<br>Supports next originator types:" +
                "<br><code>DEVICE</code>" +
                "<br><code>ASSET</code>" +
                "<br><code>ENTITY_VIEW</code>" +
                "<br><code>DASHBOARD</code>" +
                "<br><code>TENANT</code>" +
                "<br><code>CUSTOMER</code>" +
                "<br><code>EDGE</code><br><br>" +
                "As well node supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save cloud event to database or unsupported originator type/message type arrived. " +
                "In case successful storage cloud event to database message will be routed via <b>Success</b> route.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePushToCloudConfig",
        icon = "cloud_upload"
)
public class TbMsgPushToCloudNode extends AbstractTbMsgPushNode<TbMsgPushToCloudNodeConfiguration, CloudEvent, CloudEventType> {

    @Override
    CloudEvent buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, CloudEventType eventType, JsonNode entityBody) {
        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setTenantId(tenantId);
        cloudEvent.setAction(eventAction);
        cloudEvent.setEntityId(entityId);
        cloudEvent.setType(eventType);
        cloudEvent.setEntityBody(entityBody);
        return cloudEvent;
    }

    @Override
    CloudEventType getEventTypeByEntityType(EntityType entityType) {
        return CloudUtils.getCloudEventTypeByEntityType(entityType);
    }

    @Override
    CloudEventType getAlarmEventType() {
        return CloudEventType.ALARM;
    }

    @Override
    String getIgnoredMessageSource() {
        return DataConstants.CLOUD_MSG_SOURCE;
    }

    @Override
    protected Class<TbMsgPushToCloudNodeConfiguration> getConfigClazz() {
        return TbMsgPushToCloudNodeConfiguration.class;
    }

    @Override
    void processMsg(TbContext ctx, TbMsg msg) {
        try {
            CloudEvent cloudEvent = buildEvent(msg, ctx);
            ListenableFuture<Void> saveFuture = ctx.getCloudEventService().saveAsync(cloudEvent);
            Futures.addCallback(saveFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    ctx.tellNext(msg, SUCCESS);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    ctx.tellFailure(msg, throwable);
                }
            }, ctx.getDbCallbackExecutor());
        } catch (Exception e) {
            log.error("Failed to build cloud event", e);
            ctx.tellFailure(msg, e);
        }
    }

}
