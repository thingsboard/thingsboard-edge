/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "fetch device credentials",
        configClazz = TbFetchDeviceCredentialsNodeConfiguration.class,
        nodeDescription = "Adds <b>deviceCredentials</b> property to the message metadata if the configuration parameter <b>fetchToMetadata</b>" +
                " is set to <code>true</code> or if it does not exist, otherwise, adds <b>deviceCredentials</b> property to the message data!",
        nodeDetails = "Rule node returns transformed messages via <code>Success</code> chain in case that message successfully transformed" +
                " otherwise returns the incoming message as outbound message with <code>Failure</code> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeFetchDeviceCredentialsConfig",
        icon = "functions"
)
public class TbFetchDeviceCredentialsNode implements TbNode {

    private static final String DEVICE_CREDENTIAL = "deviceCredentials";
    TbFetchDeviceCredentialsNodeConfiguration config;
    boolean fetchToMetadata;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbFetchDeviceCredentialsNodeConfiguration.class);
        fetchToMetadata = BooleanUtils.toBooleanDefaultIfNull(config.isFetchToMetadata(), true);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        EntityId originator = msg.getOriginator();
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type: " + originator.getEntityType() + "!"));
            return;
        }
        DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
        DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(ctx.getTenantId(), deviceId);
        if (deviceCredentials == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to get Device Credentials for device: " + deviceId + "!"));
            return;
        }
        //TODO -- ask return type data
        /*String credentialsId = deviceCredentials.getCredentialsId();
        if (StringUtils.isEmpty(credentialsId)) {
            ctx.tellFailure(msg, new RuntimeException("Failed to get accessToken for device: " + deviceId + "!"));
            return;
        }*/

        TbMsg transformedMsg;
        if (fetchToMetadata) {
            TbMsgMetaData metaData = msg.getMetaData();
            metaData.putValue(DEVICE_CREDENTIAL, JacksonUtil.toString(deviceCredentials));
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, metaData, msg.getData());
        } else {
            ObjectNode data = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            data.set(DEVICE_CREDENTIAL, JacksonUtil.valueToTree(deviceCredentials));
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, msg.getMetaData(), JacksonUtil.toString(data));
        }
        ctx.tellSuccess(transformedMsg);
    }

    @Override
    public void destroy() {
    }

}

