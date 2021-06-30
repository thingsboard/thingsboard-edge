/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;

import java.util.UUID;

@Component
@Slf4j
public class WidgetBundleCloudProcessor extends BaseCloudProcessor {

    public ListenableFuture<Void> processWidgetsBundleMsgFromCloud(TenantId tenantId, WidgetsBundleUpdateMsg widgetsBundleUpdateMsg) {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetsBundleUpdateMsg.getIdMSB(), widgetsBundleUpdateMsg.getIdLSB()));
        switch (widgetsBundleUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    widgetCreationLock.lock();
                    WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
                    boolean created = false;
                    if (widgetsBundle == null) {
                        created = true;
                        widgetsBundle = new WidgetsBundle();
                        if (widgetsBundleUpdateMsg.getIsSystem()) {
                            widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
                        } else {
                            widgetsBundle.setTenantId(tenantId);
                        }
                        widgetsBundle.setId(widgetsBundleId);
                        widgetsBundle.setCreatedTime(Uuids.unixTimestamp(widgetsBundleId.getId()));
                    }
                    widgetsBundle.setTitle(widgetsBundleUpdateMsg.getTitle());
                    widgetsBundle.setAlias(widgetsBundleUpdateMsg.getAlias());
                    widgetsBundle.setImage(widgetsBundleUpdateMsg.getImage().toString());
                    widgetsBundleService.saveWidgetsBundle(widgetsBundle);

                    if (created) {
                        saveCloudEvent(tenantId, CloudEventType.WIDGETS_BUNDLE, ActionType.WIDGET_BUNDLE_TYPES_REQUEST, widgetsBundleId, null);
                    }
                } finally {
                    widgetCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
                if (widgetsBundle != null) {
                    widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type");
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + widgetsBundleUpdateMsg.getMsgType()));
        }
        return Futures.immediateFuture(null);
    }

    public UplinkMsg processWidgetBundleTypesRequestMsgToCloud(CloudEvent cloudEvent) {
        EntityId widgetBundleId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getCloudEventType(), cloudEvent.getEntityId());
        WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg = WidgetBundleTypesRequestMsg.newBuilder()
                .setWidgetBundleIdMSB(widgetBundleId.getId().getMostSignificantBits())
                .setWidgetBundleIdLSB(widgetBundleId.getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addWidgetBundleTypesRequestMsg(widgetBundleTypesRequestMsg);
        return builder.build();
    }
}
