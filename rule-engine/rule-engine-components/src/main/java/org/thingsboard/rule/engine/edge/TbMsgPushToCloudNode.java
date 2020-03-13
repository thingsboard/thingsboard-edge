/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
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
        if ("cloud".equalsIgnoreCase(msg.getMetaData().getValue("source"))) {
            return;
        }
        if (!msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name()) &&
                !msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) &&
                !msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED) &&
                !msg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        String entityName = null;
        String entityType = null;
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
                entityName = device.getName();
                entityType = device.getType();
                break;
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
                entityName = asset.getName();
                entityType = asset.getType();
                break;
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
                entityName = entityView.getName();
                entityType = entityView.getType();
                break;
        }

        if (entityName != null && entityType != null) {
            msg.getMetaData().putValue("source", "edge");
            ctx.getEdgeEventStorage().write(constructUplinkMsg(entityName, entityType, msg), new PushToCloudNodeCallback(ctx, msg));
        }
    }

    private UplinkMsg constructUplinkMsg(String entityName, String entityType, TbMsg tbMsg) {
        EntityDataProto entityData = EntityDataProto.newBuilder()
                .setEntityName(entityName)
                .setEntityType(entityType)
                .setTbMsg(ByteString.copyFrom(TbMsg.toBytes(tbMsg))).build();

        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .addAllEntityData(Collections.singletonList(entityData));
        return builder.build();
    }

    @Override
    public void destroy() {
    }

}
