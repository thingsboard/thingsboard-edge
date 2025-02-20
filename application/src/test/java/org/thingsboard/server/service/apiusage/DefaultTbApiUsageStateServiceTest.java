/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.apiusage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class DefaultTbApiUsageStateServiceTest extends AbstractControllerTest {

    @Autowired
    DefaultTbApiUsageStateService service;

    private TenantId tenantId;
    private Tenant savedTenant;

    private static final int MAX_ENABLE_VALUE = 5000;
    private static final long VALUE_WARNING = 4500L;
    private static final long VALUE_DISABLE = 5500L;
    private static final double WARN_THRESHOLD_VALUE = 0.8;

    @Before
    public void init() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = createTenantProfile();
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testProcess_transitionFromWarningToDisabled() {
        TransportProtos.ToUsageStatsServiceMsg.Builder warningMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null) {
                warningMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                        .setKey(key.name())
                        .setValue(VALUE_WARNING)
                        .build());
            }
        }

        TransportProtos.ToUsageStatsServiceMsg warningStatsMsg = warningMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> warningMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), warningStatsMsg);

        service.process(warningMsg, TbCallback.EMPTY);
        TenantApiUsageState tenantApiUsageState = (TenantApiUsageState) service.myUsageStates.get(tenantId);
        for (ApiFeature feature : ApiFeature.values()) {
            if (containsFeature(feature)) {
                assertEquals("For feature " + feature + " expected state WARNING", ApiUsageStateValue.WARNING,
                        tenantApiUsageState.getFeatureValue(feature));
            }
        }


        TransportProtos.ToUsageStatsServiceMsg.Builder disableMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null) {
                disableMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                        .setKey(key.name())
                        .setValue(VALUE_DISABLE)
                        .build());
            }
        }
        TransportProtos.ToUsageStatsServiceMsg disableStatsMsg = disableMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> disableMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), disableStatsMsg);

        service.process(disableMsg, TbCallback.EMPTY);
        tenantApiUsageState = (TenantApiUsageState) service.myUsageStates.get(tenantId);
        for (ApiFeature feature : ApiFeature.values()) {
            if (containsFeature(feature)) {
                assertEquals("For feature " + feature + " expected state DISABLED", ApiUsageStateValue.DISABLED,
                        tenantApiUsageState.getFeatureValue(feature));
            }
        }
    }


    private boolean containsFeature(ApiFeature feature) {
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null && key.getApiFeature().equals(feature)) {
                return true;
            }
        }
        return false;
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Tenant Profile");
        tenantProfile.setDescription("Tenant Profile" + " Test");

        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxJSExecutions(MAX_ENABLE_VALUE)
                .maxTransportMessages(MAX_ENABLE_VALUE)
                .maxRuleChains(MAX_ENABLE_VALUE)
                .maxTbelExecutions(MAX_ENABLE_VALUE)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .maxREExecutions(MAX_ENABLE_VALUE)
                .maxEmails(MAX_ENABLE_VALUE)
                .maxSms(MAX_ENABLE_VALUE)
                .maxCreatedAlarms(MAX_ENABLE_VALUE)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

}
