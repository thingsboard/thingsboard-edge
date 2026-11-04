// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
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
                        CustomMenu savedCustomMenu = edgeCtx.getCustomMenuService().updateCustomMenu(customMenu, false, false);
                        if (assigneeList != null) {
                            CMAssigneeType assigneeType = savedCustomMenu.getAssigneeType();
                            String[] assigneeIds = assigneeList.stream().map(id -> id.getId().toString()).toArray(String[]::new);
                            edgeCtx.getCustomMenuService().updateAssigneeList(savedCustomMenu, assigneeType, assigneeList, (assigneeType == CMAssigneeType.USER_GROUPS) ? assigneeIds : new String[0], true);
                        }
                    } else {
                        edgeCtx.getCustomMenuService().createCustomMenu(customMenu, assigneeList, false, false);
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
