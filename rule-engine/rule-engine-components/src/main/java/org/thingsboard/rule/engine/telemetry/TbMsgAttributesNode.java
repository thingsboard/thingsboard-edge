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
package org.thingsboard.rule.engine.telemetry;

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.adaptor.JsonConverter;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save attributes",
        configClazz = TbMsgAttributesNodeConfiguration.class,
        nodeDescription = "Saves attributes data",
        nodeDetails = "Saves entity attributes based on configurable scope parameter. Expects messages with 'POST_ATTRIBUTES_REQUEST' message type. " +
                      "If upsert(update/insert) operation is completed successfully rule node will send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used. " +
                      "Additionally if checkbox <b>Send attributes updated notification</b> is set to true, rule node will put the \"Attributes Updated\" " +
                      "event for <b>SHARED_SCOPE</b> and <b>SERVER_SCOPE</b> attributes updates to the corresponding rule engine queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAttributesConfig",
        icon = "file_upload"
)
public class TbMsgAttributesNode implements TbNode {

    private TbMsgAttributesNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgAttributesNodeConfiguration.class);
        if (config.getNotifyDevice() == null) {
            config.setNotifyDevice(true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        String src = msg.getData();
        List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(src)));
        if (attributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        String scope = getScope(msg.getMetaData().getValue(SCOPE));
        boolean sendAttributesUpdateNotification = checkSendNotification(scope);
        ctx.getTelemetryService().saveAndNotify(
                ctx.getTenantId(),
                msg.getOriginator(),
                scope,
                attributes,
                checkNotifyDevice(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY)),
                sendAttributesUpdateNotification ?
                        new AttributesUpdateNodeCallback(ctx, msg, scope, attributes) :
                        new TelemetryNodeCallback(ctx, msg)
        );
    }

    private boolean checkSendNotification(String scope) {
        return config.isSendAttributesUpdatedNotification() && !CLIENT_SCOPE.equals(scope);
    }

    private boolean checkNotifyDevice(String notifyDeviceMdValue) {
        return config.getNotifyDevice() || StringUtils.isEmpty(notifyDeviceMdValue) || Boolean.parseBoolean(notifyDeviceMdValue);
    }

    private String getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return mdScopeValue;
        }
        return config.getScope();
    }

}
