/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.gen.edge.v1.CustomMenuProto;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;

@Component
@Slf4j
public class CustomMenuCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processCustomMenuMsgFromCloud(TenantId tenantId, CustomMenuProto customMenuProto) {
        try {
            switch (customMenuProto.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    CustomMenu customMenu = JacksonUtil.fromString(customMenuProto.getEntity(), CustomMenu.class, true);
                    if (customMenu == null) {
                        throw new RuntimeException("[{" + tenantId + "}] customMenuProto {" + customMenuProto + "} cannot be converted to custom menu");
                    }
                    List<EntityId> assigneeList = customMenuProto.hasAssigneeList()
                            ? JacksonUtil.fromString(customMenuProto.getAssigneeList(), new TypeReference<>() {}, true) : null;
                    CustomMenu existing = edgeCtx.getCustomMenuService().findCustomMenuById(customMenu.getTenantId(), customMenu.getId());
                    if (existing != null) {
                        CustomMenu savedCustomMenu = edgeCtx.getCustomMenuService().updateCustomMenu(customMenu, false);
                        if (assigneeList != null) {
                            edgeCtx.getCustomMenuService().updateAssigneeList(savedCustomMenu, savedCustomMenu.getAssigneeType(), assigneeList, true);
                        }
                    } else {
                        edgeCtx.getCustomMenuService().createCustomMenu(customMenu, assigneeList, false);
                    }
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    CustomMenu customMenu = JacksonUtil.fromString(customMenuProto.getEntity(), CustomMenu.class, true);
                    if (customMenu == null) {
                        throw new RuntimeException("[{" + tenantId + "}] customMenuProto {" + customMenuProto + "} cannot be converted to custom menu");
                    }
                    CustomMenu existing = edgeCtx.getCustomMenuService().findCustomMenuById(customMenu.getTenantId(), customMenu.getId());
                    if (existing != null) {
                        edgeCtx.getCustomMenuService().deleteCustomMenu(customMenu, true);
                    }
                }
            }
        } catch (Exception e) {
            String errMsg = "Exception during updating custom menu";
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

}
