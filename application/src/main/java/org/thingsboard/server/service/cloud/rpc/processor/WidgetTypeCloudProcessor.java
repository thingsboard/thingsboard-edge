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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class WidgetTypeCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processWidgetTypeMsgFromCloud(TenantId tenantId, WidgetTypeUpdateMsg widgetTypeUpdateMsg) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(new UUID(widgetTypeUpdateMsg.getIdMSB(), widgetTypeUpdateMsg.getIdLSB()));
        switch (widgetTypeUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                widgetCreationLock.lock();
                try {
                    WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(tenantId, widgetTypeId);
                    if (widgetTypeDetails == null) {
                        widgetTypeDetails = new WidgetTypeDetails();
                        if (widgetTypeUpdateMsg.getIsSystem()) {
                            widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
                        } else {
                            widgetTypeDetails.setTenantId(tenantId);
                        }
                        widgetTypeDetails.setId(widgetTypeId);
                        widgetTypeDetails.setCreatedTime(Uuids.unixTimestamp(widgetTypeId.getId()));
                    }
                    widgetTypeDetails.setBundleAlias(widgetTypeUpdateMsg.hasBundleAlias() ? widgetTypeUpdateMsg.getBundleAlias() : null);
                    widgetTypeDetails.setAlias(widgetTypeUpdateMsg.hasAlias() ? widgetTypeUpdateMsg.getAlias() : null);
                    widgetTypeDetails.setName(widgetTypeUpdateMsg.hasName() ? widgetTypeUpdateMsg.getName() : null);
                    widgetTypeDetails.setDescriptor(widgetTypeUpdateMsg.hasDescriptorJson() ? JacksonUtil.toJsonNode(widgetTypeUpdateMsg.getDescriptorJson()) : null);
                    widgetTypeDetails.setImage(widgetTypeUpdateMsg.hasImage() ? widgetTypeUpdateMsg.getImage() : null);
                    widgetTypeDetails.setDescription(widgetTypeUpdateMsg.hasDescription() ? widgetTypeUpdateMsg.getDescription() : null);
                    widgetTypeService.saveWidgetType(widgetTypeDetails, false);
                } finally {
                    widgetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                WidgetType widgetType = widgetTypeService.findWidgetTypeById(tenantId, widgetTypeId);
                if (widgetType != null) {
                    widgetTypeService.deleteWidgetType(tenantId, widgetType.getId());
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(widgetTypeUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }
}
