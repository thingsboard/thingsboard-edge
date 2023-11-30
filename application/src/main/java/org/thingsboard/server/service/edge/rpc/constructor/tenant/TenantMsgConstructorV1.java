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
package org.thingsboard.server.service.edge.rpc.constructor.tenant;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

@Component
@TbCoreComponent
public class TenantMsgConstructorV1 implements TenantMsgConstructor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    @Override
    public TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant) {
        TenantUpdateMsg.Builder builder = TenantUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenant.getId().getId().getMostSignificantBits())
                .setIdLSB(tenant.getId().getId().getLeastSignificantBits())
                .setTitle(tenant.getTitle())
                .setProfileIdMSB(tenant.getTenantProfileId().getId().getMostSignificantBits())
                .setProfileIdLSB(tenant.getTenantProfileId().getId().getLeastSignificantBits())
                .setRegion(tenant.getRegion());
        if (tenant.getCountry() != null) {
            builder.setCountry(tenant.getCountry());
        }
        if (tenant.getState() != null) {
            builder.setState(tenant.getState());
        }
        if (tenant.getCity() != null) {
            builder.setCity(tenant.getCity());
        }
        if (tenant.getAddress() != null) {
            builder.setAddress(tenant.getAddress());
        }
        if (tenant.getAddress2() != null) {
            builder.setAddress2(tenant.getAddress2());
        }
        if (tenant.getZip() != null) {
            builder.setZip(tenant.getZip());
        }
        if (tenant.getPhone() != null) {
            builder.setPhone(tenant.getPhone());
        }
        if (tenant.getEmail() != null) {
            builder.setEmail(tenant.getEmail());
        }
        if (tenant.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(tenant.getAdditionalInfo()));
        }
        return builder.build();
    }

    @Override
    public TenantProfileUpdateMsg constructTenantProfileUpdateMsg(UpdateMsgType msgType, TenantProfile tenantProfile, EdgeVersion edgeVersion) {
        ByteString profileData = EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_6_1) ?
                ByteString.empty() : ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile.getProfileData()));
        TenantProfileUpdateMsg.Builder builder = TenantProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenantProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(tenantProfile.getId().getId().getLeastSignificantBits())
                .setName(tenantProfile.getName())
                .setDefault(tenantProfile.isDefault())
                .setIsolatedRuleChain(tenantProfile.isIsolatedTbRuleEngine())
                .setProfileDataBytes(profileData);
        if (tenantProfile.getDescription() != null) {
            builder.setDescription(tenantProfile.getDescription());
        }
        return builder.build();
    }
}
