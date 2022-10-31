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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class OtaPackageEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertOtaPackageEventToDownlink(EdgeEvent edgeEvent) {
        OtaPackageId otaPackageId = new OtaPackageId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                OtaPackage otaPackage = otaPackageService.findOtaPackageById(edgeEvent.getTenantId(), otaPackageId);
                if (otaPackage != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OtaPackageUpdateMsg otaPackageUpdateMsg =
                            otaPackageMsgConstructor.constructOtaPackageUpdatedMsg(msgType, otaPackage);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                OtaPackageUpdateMsg otaPackageUpdateMsg =
                        otaPackageMsgConstructor.constructOtaPackageDeleteMsg(otaPackageId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processOtaPackageNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }
}
