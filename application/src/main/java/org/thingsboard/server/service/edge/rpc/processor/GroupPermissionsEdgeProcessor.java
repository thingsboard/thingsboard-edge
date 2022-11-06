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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class GroupPermissionsEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertGroupPermissionEventToDownlink(EdgeEvent edgeEvent) {
        GroupPermissionId groupPermissionId = new GroupPermissionId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                GroupPermission groupPermission = groupPermissionService.findGroupPermissionById(edgeEvent.getTenantId(), groupPermissionId);
                if (groupPermission != null) {
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addGroupPermissionMsg(
                                    groupPermissionProtoConstructor.constructGroupPermissionProto(msgType, groupPermission))
                            .build();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addGroupPermissionMsg(
                                groupPermissionProtoConstructor.constructGroupPermissionDeleteMsg(groupPermissionId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processGroupPermissionNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type,
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (actionType) {
            case ADDED:
            case UPDATED:
                ListenableFuture<GroupPermission> gpFuture = groupPermissionService.findGroupPermissionByIdAsync(tenantId, new GroupPermissionId(entityId.getId()));
                return Futures.transformAsync(gpFuture, groupPermission -> {
                    if (groupPermission == null) {
                        return Futures.immediateFuture(null);
                    }
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<EdgeId> pageData;
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    do {
                        pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, groupPermission.getUserGroupId(), EntityType.USER, pageLink);
                        if (pageData.getData().size() > 0) {
                            for (EdgeId edgeId : pageData.getData()) {
                                ListenableFuture<Boolean> checkFuture =
                                        entityGroupService.checkEdgeEntityGroupById(tenantId, edgeId, groupPermission.getEntityGroupId(), groupPermission.getEntityGroupType());
                                futures.add(Futures.transformAsync(checkFuture, exists -> {
                                    if (Boolean.TRUE.equals(exists)) {
                                        return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, null);
                                    } else {
                                        return Futures.immediateFuture(null);
                                    }
                                }, dbCallbackExecutorService));
                            }
                            if (pageData.hasNext()) {
                                pageLink = pageLink.nextPageLink();
                            }
                        }
                    } while (pageData.hasNext());
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                }, dbCallbackExecutorService);
            case DELETED:
                return processActionForAllEdges(tenantId, type, actionType, entityId);
            default:
                return Futures.immediateFuture(null);
        }
    }

}
