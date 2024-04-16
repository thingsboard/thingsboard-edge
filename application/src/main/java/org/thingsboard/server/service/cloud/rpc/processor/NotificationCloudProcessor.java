/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class NotificationCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processNotificationRuleMsgFromCloud(TenantId tenantId, NotificationRuleUpdateMsg notificationRuleUpdateMsg) {
        switch (notificationRuleUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                NotificationRule notificationRule = JacksonUtil.fromString(notificationRuleUpdateMsg.getEntity(), NotificationRule.class, true);
                if (notificationRule == null) {
                    throw new RuntimeException("[{" + tenantId + "}] notificationRuleUpdateMsg {" + notificationRuleUpdateMsg + "} cannot be converted to notification rule");
                }
                notificationRuleService.saveNotificationRule(tenantId, notificationRule);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationRuleId notificationRuleId = new NotificationRuleId(new UUID(notificationRuleUpdateMsg.getIdMSB(), notificationRuleUpdateMsg.getIdLSB()));
                NotificationRule notificationRuleToDelete = notificationRuleService.findNotificationRuleById(tenantId, notificationRuleId);
                if (notificationRuleToDelete != null) {
                    notificationRuleService.deleteNotificationRuleById(tenantId, notificationRuleId);
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
                notificationTargetService.saveNotificationTarget(tenantId, notificationTarget);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationTargetId notificationTargetId = new NotificationTargetId(new UUID(notificationTargetUpdateMsg.getIdMSB(), notificationTargetUpdateMsg.getIdLSB()));
                NotificationTarget notificationTargetToDelete = notificationTargetService.findNotificationTargetById(tenantId, notificationTargetId);
                if (notificationTargetToDelete != null) {
                    notificationTargetService.deleteNotificationTargetById(tenantId, notificationTargetId);
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
                notificationTemplateService.saveNotificationTemplate(tenantId, notificationTemplate);
                return Futures.immediateFuture(null);
            case ENTITY_DELETED_RPC_MESSAGE:
                NotificationTemplateId notificationTemplateId = new NotificationTemplateId(new UUID(notificationTemplateUpdateMsg.getIdMSB(), notificationTemplateUpdateMsg.getIdLSB()));
                NotificationTemplate notificationTemplateToDelete = notificationTemplateService.findNotificationTemplateById(tenantId, notificationTemplateId);
                if (notificationTemplateToDelete != null) {
                    notificationTemplateService.deleteNotificationTemplateById(tenantId, notificationTemplateId);
                }
                return Futures.immediateFuture(null);
            default:
                return handleUnsupportedMsgType(notificationTemplateUpdateMsg.getMsgType());
        }
    }

}
