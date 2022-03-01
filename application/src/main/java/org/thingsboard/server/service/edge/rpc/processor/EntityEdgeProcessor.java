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
package org.thingsboard.server.service.edge.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class EntityEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg processEntityMergeRequestMessageToEdge(Edge edge, EdgeEvent edgeEvent) {
        DownlinkMsg downlinkMsg = null;
        if (EdgeEventType.DEVICE.equals(edgeEvent.getType())) {
            DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
            Device device = deviceService.findDeviceById(edge.getTenantId(), deviceId);
            // TODO: voba - fix this
            // CustomerId customerId = getCustomerIdIfEdgeAssignedToCustomer(device, edge);
            String conflictName = null;
            if (edgeEvent.getBody() != null) {
                conflictName = edgeEvent.getBody().get("conflictName").asText();
            }
            DeviceUpdateMsg deviceUpdateMsg = deviceMsgConstructor
                    .constructDeviceUpdatedMsg(UpdateMsgType.ENTITY_MERGE_RPC_MESSAGE, device, null, conflictName);
            downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addDeviceUpdateMsg(deviceUpdateMsg)
                    .build();
        }
        return downlinkMsg;
    }

    public DownlinkMsg processCredentialsRequestMessageToEdge(EdgeEvent edgeEvent) {
        DownlinkMsg downlinkMsg = null;
        if (EdgeEventType.DEVICE.equals(edgeEvent.getType())) {
            DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
            DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                    .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                    .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                    .build();
            DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg);
            downlinkMsg = builder.build();
        }
        return downlinkMsg;
    }

    public void processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId edgeId = null;
        if (edgeNotificationMsg.getEdgeIdMSB() != 0 && edgeNotificationMsg.getEdgeIdLSB() != 0) {
            edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB()));
        }
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ADDED_TO_ENTITY_GROUP:
            case CREDENTIALS_UPDATED:
                pushNotificationToAllRelatedEdges(tenantId, entityId, type, actionType, constructEntityGroupId(tenantId, edgeNotificationMsg));
                break;
            case DELETED:
            case CHANGE_OWNER:
                if (edgeId != null) {
                    saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                } else {
                    pushNotificationToAllRelatedEdges(tenantId, entityId, type, actionType, null);
                }
                break;
            case REMOVED_FROM_ENTITY_GROUP:
                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null, constructEntityGroupId(tenantId, edgeNotificationMsg));
                break;
            case ASSIGNED_TO_EDGE:
            case UNASSIGNED_FROM_EDGE:
                saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                if (type.equals(EdgeEventType.RULE_CHAIN)) {
                    updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                }
                break;
        }
    }

    private EntityGroupId constructEntityGroupId(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        if (edgeNotificationMsg.getEntityGroupIdMSB() != 0 && edgeNotificationMsg.getEntityGroupIdLSB() != 0) {
            EntityGroupId entityGroupId = new EntityGroupId(new UUID(edgeNotificationMsg.getEntityGroupIdMSB(), edgeNotificationMsg.getEntityGroupIdLSB()));
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
            if (entityGroup.isEdgeGroupAll()) {
                return null;
            } else {
                return entityGroupId;
            }
        } else {
            return null;
        }
    }

    private void pushNotificationToAllRelatedEdges(TenantId tenantId, EntityId entityId, EdgeEventType type, EdgeEventActionType actionType, EntityGroupId entityGroupId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EdgeId> pageData;
        do {
            pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (EdgeId relatedEdgeId : pageData.getData()) {
                    saveEdgeEvent(tenantId, relatedEdgeId, type, actionType, entityId, null, entityGroupId);
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
    }

    private void updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                for (RuleChain ruleChain : pageData.getData()) {
                    if (!ruleChain.getId().equals(processingRuleChainId)) {
                        List<RuleChainConnectionInfo> connectionInfos =
                                ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
                        if (connectionInfos != null && !connectionInfos.isEmpty()) {
                            for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                                if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                                    saveEdgeEvent(tenantId,
                                            edgeId,
                                            EdgeEventType.RULE_CHAIN_METADATA,
                                            EdgeEventActionType.UPDATED,
                                            ruleChain.getId(),
                                            null);
                                }
                            }
                        }
                    }
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
    }

    public void processEntityNotificationForAllEdges(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case DELETED:
                processActionForAllEdges(tenantId, type, actionType, entityId);
                break;
        }
    }
}

