/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.UplinkMsg;

import java.util.Collections;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to cloud",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes messages to cloud",
        nodeDetails = "Pushes messages to cloud. This node is used only on Edge instances to push messages from Edge to Cloud.",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbNodeEmptyConfig",
        icon = "cloud_upload"
)
public class TbMsgPushToCloudNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (DataConstants.CLOUD_MSG_SOURCE.equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the cloud, msg [{}]", msg);
            return;
        }
        if (isSupportedOriginator(msg.getOriginator().getEntityType())) {
            if (isSupportedMsgType(msg.getType())) {
                ctx.getEdgeEventStorage().write(constructUplinkMsg(msg), new PushToCloudNodeCallback(ctx, msg));
            } else {
                log.debug("Unsupported msg type {}", msg.getType());
                ctx.tellFailure(msg, new RuntimeException("Unsupported msg type '" + msg.getType() + "'"));
            }
        } else {
            log.debug("Unsupported originator type {}", msg.getOriginator().getEntityType());
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + msg.getOriginator().getEntityType() + "'"));
        }
    }

    private boolean isSupportedMsgType(String msgType) {
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(msgType)
                || SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msgType)
                || DataConstants.ATTRIBUTES_UPDATED.equals(msgType)
                || DataConstants.ATTRIBUTES_DELETED.equals(msgType)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSupportedOriginator(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
                return true;
            default:
                return false;
        }
    }

    private UplinkMsg constructUplinkMsg(TbMsg tbMsg) {
        EntityDataProto.Builder entityDataBuilder = EntityDataProto.newBuilder()
                .setEntityIdMSB(tbMsg.getOriginator().getId().getMostSignificantBits())
                .setEntityIdLSB(tbMsg.getOriginator().getId().getLeastSignificantBits())
                .setEntityType(tbMsg.getOriginator().getEntityType().name());
        if (SessionMsgType.POST_TELEMETRY_REQUEST.name().equals(tbMsg.getType())) {
            entityDataBuilder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(JsonUtils.parse(tbMsg.getData())));
        }
        if (SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(tbMsg.getType())
                || DataConstants.ATTRIBUTES_UPDATED.equals(tbMsg.getType())) {
            entityDataBuilder.setPostAttributesMsg(JsonConverter.convertToAttributesProto(JsonUtils.parse(tbMsg.getData())));
            // TODO: voba - add support for attribute delete
            // || DataConstants.ATTRIBUTES_DELETED.equals(tbMsg.getType())
        }

        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityDataBuilder.build()));
        return builder.build();
    }

    @Override
    public void destroy() {
    }

}
