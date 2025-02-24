/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class NotificationCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processNotificationRuleMsgFromCloud(TenantId tenantId, NotificationRuleUpdateMsg notificationRuleUpdateMsg) {
        switch (notificationRuleUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                NotificationRule notificationRule = JacksonUtil.fromString(notificationRuleUpdateMsg.getEntity(), NotificationRule.class, true);
                if (notificationRule == null) {
                    throw new RuntimeException("[{" + tenantId + "}] notificationRuleUpdateMsg {" + notificationRuleUpdateMsg + "} cannot be converted to notification rule");
                }
                NotificationRuleService notificationRuleService = edgeCtx.getNotificationRuleService();
                Optional<NotificationRule> edgeNotificationRule = notificationRuleService.findNotificationRuleByTenantIdAndName(tenantId, notificationRule.getName());
                edgeNotificationRule.filter(rule -> !rule.getId().equals(notificationRule.getId()))
                        .ifPresent(rule -> notificationRuleService.deleteNotificationRuleById(tenantId, rule.getId()));

                notificationRuleService.saveNotificationRule(tenantId, notificationRule);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationRuleId notificationRuleId = new NotificationRuleId(new UUID(notificationRuleUpdateMsg.getIdMSB(), notificationRuleUpdateMsg.getIdLSB()));
                NotificationRule notificationRuleToDelete = edgeCtx.getNotificationRuleService().findNotificationRuleById(tenantId, notificationRuleId);
                if (notificationRuleToDelete != null) {
                    edgeCtx.getNotificationRuleService().deleteNotificationRuleById(tenantId, notificationRuleId);
                }
                return Futures.immediateFuture(null);
            default:
                return handleUnsupportedMsgType(notificationRuleUpdateMsg.getMsgType());
        }
    }

    public ListenableFuture<Void> processNotificationTargetMsgFromCloud(TenantId tenantId, NotificationTargetUpdateMsg notificationTargetUpdateMsg) {
        switch (notificationTargetUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                NotificationTarget notificationTarget = JacksonUtil.fromString(notificationTargetUpdateMsg.getEntity(), NotificationTarget.class, true);
                if (notificationTarget == null) {
                    throw new RuntimeException("[{" + tenantId + "}] notificationTargetUpdateMsg {" + notificationTargetUpdateMsg + "} cannot be converted to notification target");
                }
                NotificationTargetService notificationTargetService = edgeCtx.getNotificationTargetService();
                Optional<NotificationTarget> edgeNotificationTarget = notificationTargetService.findNotificationTargetByTenantIdAndName(tenantId, notificationTarget.getName());
                edgeNotificationTarget.filter(target -> !target.getId().equals(notificationTarget.getId()))
                        .ifPresent(target -> notificationTargetService.deleteNotificationTargetById(tenantId, target.getId()));

                notificationTargetService.saveNotificationTarget(tenantId, notificationTarget);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationTargetId notificationTargetId = new NotificationTargetId(new UUID(notificationTargetUpdateMsg.getIdMSB(), notificationTargetUpdateMsg.getIdLSB()));
                NotificationTarget notificationTargetToDelete = edgeCtx.getNotificationTargetService().findNotificationTargetById(tenantId, notificationTargetId);
                if (notificationTargetToDelete != null) {
                    edgeCtx.getNotificationTargetService().deleteNotificationTargetById(tenantId, notificationTargetId);
                }
                return Futures.immediateFuture(null);
            default:
                return handleUnsupportedMsgType(notificationTargetUpdateMsg.getMsgType());
        }
    }

    public ListenableFuture<Void> processNotificationTemplateMsgFromCloud(TenantId tenantId, NotificationTemplateUpdateMsg notificationTemplateUpdateMsg) {
        switch (notificationTemplateUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                NotificationTemplate notificationTemplate = JacksonUtil.fromString(notificationTemplateUpdateMsg.getEntity(), NotificationTemplate.class, true);
                if (notificationTemplate == null) {
                    throw new RuntimeException("[{" + tenantId + "}] notificationTemplateUpdateMsg {" + notificationTemplateUpdateMsg + "} cannot be converted to notification template");
                }
                NotificationTemplateService notificationTemplateService = edgeCtx.getNotificationTemplateService();
                Optional<NotificationTemplate> edgeNotificationTemplate = notificationTemplateService.findNotificationTemplateByTenantIdAndName(tenantId, notificationTemplate.getName());
                edgeNotificationTemplate.filter(template -> !template.getId().equals(notificationTemplate.getId()))
                        .ifPresent(template -> notificationTemplateService.deleteNotificationTemplateById(tenantId, template.getId()));

                notificationTemplateService.saveNotificationTemplate(tenantId, notificationTemplate);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationTemplateId notificationTemplateId = new NotificationTemplateId(new UUID(notificationTemplateUpdateMsg.getIdMSB(), notificationTemplateUpdateMsg.getIdLSB()));
                NotificationTemplate notificationTemplateToDelete = edgeCtx.getNotificationTemplateService().findNotificationTemplateById(tenantId, notificationTemplateId);
                if (notificationTemplateToDelete != null) {
                    edgeCtx.getNotificationTemplateService().deleteNotificationTemplateById(tenantId, notificationTemplateId);
                }
                return Futures.immediateFuture(null);
            default:
                return handleUnsupportedMsgType(notificationTemplateUpdateMsg.getMsgType());
        }
    }

}
