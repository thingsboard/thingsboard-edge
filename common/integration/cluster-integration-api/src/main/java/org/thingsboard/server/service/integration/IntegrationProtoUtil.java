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
package org.thingsboard.server.service.integration;

import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;

import java.util.UUID;

public class IntegrationProtoUtil {

    public static IntegrationInfoProto toProto(AbstractIntegration integrationInfo) {
        return IntegrationInfoProto.newBuilder()
                .setIntegrationIdMSB(integrationInfo.getId().getId().getMostSignificantBits())
                .setIntegrationIdLSB(integrationInfo.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(integrationInfo.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(integrationInfo.getTenantId().getId().getLeastSignificantBits())
                .setName(integrationInfo.getName())
                .setType(integrationInfo.getType().name())
                .setEnabled(integrationInfo.isEnabled())
                .setRemote(integrationInfo.isRemote())
                .setAllowCreateDevicesOrAssets(integrationInfo.isAllowCreateDevicesOrAssets())
                .build();
    }

    public static IntegrationInfo toInfo(IntegrationInfoProto proto) {
        var result = new IntegrationInfo(new IntegrationId(new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB())));
        result.setTenantId(new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())));
        result.setName(proto.getName());
        result.setType(IntegrationType.valueOf(proto.getType()));
        result.setRemote(proto.getRemote());
        result.setEnabled(proto.getEnabled());
        result.setAllowCreateDevicesOrAssets(proto.getAllowCreateDevicesOrAssets());
        return result;
    }
}
