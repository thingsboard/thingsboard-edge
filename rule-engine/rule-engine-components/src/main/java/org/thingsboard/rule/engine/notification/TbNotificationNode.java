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
package org.thingsboard.rule.engine.notification;

import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.info.RuleEngineOriginatedNotificationInfo;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send notification",
        configClazz = TbNotificationNodeConfiguration.class,
        nodeDescription = "Sends notification to a target",
        nodeDetails = "Will send notification to the specified target",
        uiResources = {"static/rulenode/rulenode-core-config.js"}
)
public class TbNotificationNode implements TbNode {

    private TbNotificationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        RuleEngineOriginatedNotificationInfo notificationInfo = RuleEngineOriginatedNotificationInfo.builder()
                .msgOriginator(msg.getOriginator())
                .msgMetadata(msg.getMetaData().getData())
                .msgType(msg.getType())
                .build();

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .targets(config.getTargets())
                .templateId(config.getTemplateId())
                .info(notificationInfo)
                .additionalConfig(new NotificationRequestConfig())
                .originatorEntityId(ctx.getSelf().getRuleChainId())
                .build();

        DonAsynchron.withCallback(ctx.getNotificationExecutor().executeAsync(() -> {
                    return ctx.getNotificationCenter().processNotificationRequest(ctx.getTenantId(), notificationRequest);
                }),
                r -> {
                    TbMsgMetaData msgMetaData = msg.getMetaData().copy();
                    msgMetaData.putValue("notificationRequestId", r.getUuidId().toString());
                    ctx.tellSuccess(TbMsg.transformMsg(msg, msgMetaData));
                },
                e -> {
                    ctx.tellFailure(msg, e);
                });
    }

}
