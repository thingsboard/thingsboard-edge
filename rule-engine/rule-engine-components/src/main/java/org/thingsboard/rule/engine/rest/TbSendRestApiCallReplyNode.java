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
package org.thingsboard.rule.engine.rest;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rest call reply",
        configClazz = TbSendRestApiCallReplyNodeConfiguration.class,
        nodeDescription = "Sends reply to REST API call to rule engine",
        nodeDetails = "Expects messages with any message type. Forwards incoming message as a reply to REST API call sent to rule engine.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSendRestApiCallReplyConfig",
        icon = "call_merge"
)
public class TbSendRestApiCallReplyNode implements TbNode {

    private TbSendRestApiCallReplyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRestApiCallReplyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String serviceIdStr = msg.getMetaData().getValue(config.getServiceIdMetaDataAttribute());
        String requestIdStr = msg.getMetaData().getValue(config.getRequestIdMetaDataAttribute());
        if (StringUtils.isEmpty(requestIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Request id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(msg.getData())) {
            ctx.tellFailure(msg, new RuntimeException("Request body is empty!"));
        } else {
            ctx.getRpcService().sendRestApiCallReply(serviceIdStr, UUID.fromString(requestIdStr), msg);
            ctx.tellSuccess(msg);
        }
    }

}
