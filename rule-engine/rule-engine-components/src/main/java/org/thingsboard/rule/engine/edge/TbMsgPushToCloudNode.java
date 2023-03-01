/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
                "<br><code>EDGE</code>" +
                "<br><code>ENTITY_GROUP</code><br><br>" +
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
