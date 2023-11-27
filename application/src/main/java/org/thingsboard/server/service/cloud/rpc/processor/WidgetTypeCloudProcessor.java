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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class WidgetTypeCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processWidgetTypeMsgFromCloud(TenantId tenantId, WidgetTypeUpdateMsg widgetTypeUpdateMsg) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(new UUID(widgetTypeUpdateMsg.getIdMSB(), widgetTypeUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (widgetTypeUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    widgetCreationLock.lock();
                    try {
                        WidgetTypeDetails widgetTypeDetails = JacksonUtil.fromStringIgnoreUnknownProperties(widgetTypeUpdateMsg.getEntity(), WidgetTypeDetails.class);
                        if (widgetTypeDetails == null) {
                            throw new RuntimeException("[{" + tenantId + "}] widgetTypeUpdateMsg {" + widgetTypeUpdateMsg + "} cannot be converted to widget type");
                        }
                        try {
                            widgetTypeService.saveWidgetType(widgetTypeDetails, false);
                        } catch (Exception e) {
                            if (e instanceof DataValidationException && e.getMessage().contains("fqn already exists")) {
                                deleteWidgetBundlesAndTypes(TenantId.SYS_TENANT_ID);
                                deleteWidgetBundlesAndTypes(tenantId);
                                widgetTypeService.saveWidgetType(widgetTypeDetails, false);
                            } else {
                                throw new RuntimeException(e);
                            }
                        }
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
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private void deleteWidgetBundlesAndTypes(TenantId tenantId) {
        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);
        for (WidgetsBundle systemWidgetsBundle : systemWidgetsBundles) {
            if (systemWidgetsBundle != null) {
                PageData<WidgetTypeInfo> widgetTypes;
                var pageLink = new PageLink(1024);
                do {
                    widgetTypes = widgetTypeService.findWidgetTypesInfosByWidgetsBundleId(tenantId, systemWidgetsBundle.getId(), false, DeprecatedFilter.ALL, null, pageLink);
                    for (var widgetType : widgetTypes.getData()) {
                        widgetTypeService.deleteWidgetType(tenantId, widgetType.getId());
                    }
                    pageLink.nextPageLink();
                } while (widgetTypes.hasNext());
                widgetsBundleService.deleteWidgetsBundle(tenantId, systemWidgetsBundle.getId());
            }
        }
    }
}
