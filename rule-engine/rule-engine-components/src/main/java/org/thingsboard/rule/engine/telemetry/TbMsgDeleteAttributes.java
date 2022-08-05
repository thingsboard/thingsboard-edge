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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete attributes",
        configClazz = TbMsgDeleteAttributesConfiguration.class,
        nodeDescription = "Delete attributes for Message Originator.",
        nodeDetails = "Allowed scope parameter values: <b>SERVER/CLIENT/SHARED</b>. If no attributes are selected - " +
                "message send via <b>Failure</b> chain. If selected attributes successfully deleted - message send via " +
                "<b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteAttributesConfig",
        icon = "remove_circle"
)
public class TbMsgDeleteAttributes implements TbNode {

    TbMsgDeleteAttributesConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeleteAttributesConfiguration.class);
        if (CollectionUtils.isEmpty(config.getKeys())) {
            throw new IllegalArgumentException("Attribute keys list is empty!");
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        List<String> keysPatterns = config.getKeys();
        String scope = TbNodeUtils.processPattern(config.getScope(), msg);
        if (DataConstants.SERVER_SCOPE.equals(scope) ||
                DataConstants.CLIENT_SCOPE.equals(scope) ||
                DataConstants.SHARED_SCOPE.equals(scope)) {
            List<String> keysToDelete = keysPatterns.stream()
                    .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                    .distinct()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (keysToDelete.isEmpty()) {
                throw new RuntimeException("Selected keys patterns have invalid values!");
            }
            ctx.getTelemetryService().deleteAndNotify(ctx.getTenantId(), msg.getOriginator(), scope, keysToDelete, new TelemetryNodeCallback(ctx, msg));
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported attributes scope '" + scope + "'! Only 'SERVER_SCOPE', 'CLIENT_SCOPE' or 'SHARED_SCOPE' are allowed!"));
        }
    }

    @Override
    public void destroy() {

    }
}
