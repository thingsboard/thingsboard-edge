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
package org.thingsboard.server.service.limits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cache.limits.DefaultRateLimitService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.tenant.DefaultTbTenantProfileCache;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private DefaultTbTenantProfileCache tenantProfileCache;
    private TenantId tenantId;

    @BeforeEach
    public void beforeEach() {
        tenantProfileCache = Mockito.mock(DefaultTbTenantProfileCache.class);
        rateLimitService = new DefaultRateLimitService(tenantProfileCache, mock(NotificationRuleProcessor.class), 60, 100);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
    }

    @Test
    public void testRateLimits() {
        int max = 2;
        String rateLimit = max + ":600";
        DefaultTenantProfileConfiguration profileConfiguration = new DefaultTenantProfileConfiguration();
        profileConfiguration.setTenantEntityExportRateLimit(rateLimit);
        profileConfiguration.setTenantEntityImportRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(rateLimit);
        profileConfiguration.setTenantServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setCustomerServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setWsUpdatesPerSessionRateLimit(rateLimit);
        profileConfiguration.setCassandraReadQueryTenantCoreRateLimits(rateLimit);
        profileConfiguration.setCassandraWriteQueryTenantCoreRateLimits(rateLimit);
        profileConfiguration.setCassandraReadQueryTenantRuleEngineRateLimits(rateLimit);
        profileConfiguration.setCassandraWriteQueryTenantRuleEngineRateLimits(rateLimit);
        profileConfiguration.setEdgeEventRateLimits(rateLimit);
        profileConfiguration.setEdgeEventRateLimitsPerEdge(rateLimit);
        profileConfiguration.setEdgeUplinkMessagesRateLimits(rateLimit);
        profileConfiguration.setEdgeUplinkMessagesRateLimitsPerEdge(rateLimit);
        updateTenantProfileConfiguration(profileConfiguration);

        for (LimitedApi limitedApi : List.of(
                LimitedApi.ENTITY_EXPORT,
                LimitedApi.ENTITY_IMPORT,
                LimitedApi.NOTIFICATION_REQUESTS,
                LimitedApi.REST_REQUESTS_PER_CUSTOMER,
                LimitedApi.CASSANDRA_READ_QUERIES_CORE,
                LimitedApi.CASSANDRA_WRITE_QUERIES_CORE,
                LimitedApi.CASSANDRA_READ_QUERIES_RULE_ENGINE,
                LimitedApi.CASSANDRA_WRITE_QUERIES_RULE_ENGINE,
                LimitedApi.EDGE_EVENTS,
                LimitedApi.EDGE_EVENTS_PER_EDGE,
                LimitedApi.EDGE_UPLINK_MESSAGES,
                LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE
        )) {
            testRateLimits(limitedApi, max, tenantId);
        }

        for (LimitedApi limitedApi : List.of(
                LimitedApi.CASSANDRA_READ_QUERIES_MONOLITH,
                LimitedApi.CASSANDRA_WRITE_QUERIES_MONOLITH
        )) {
            testRateLimits(limitedApi, max * 2, tenantId);
        }

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        testRateLimits(LimitedApi.REST_REQUESTS_PER_CUSTOMER, max, customerId);

        NotificationRuleId notificationRuleId = new NotificationRuleId(UUID.randomUUID());
        testRateLimits(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, max, notificationRuleId);

        String wsSessionId = UUID.randomUUID().toString();
        testRateLimits(LimitedApi.WS_UPDATES_PER_SESSION, max, wsSessionId);
    }

    private void testRateLimits(LimitedApi limitedApi, int max, Object level) {
        for (int i = 1; i <= max; i++) {
            boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
            Assertions.assertTrue(success);
        }
        boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
        Assertions.assertFalse(success);
    }

    private void updateTenantProfileConfiguration(DefaultTenantProfileConfiguration profileConfiguration) {
        reset(tenantProfileCache);
        TenantProfile tenantProfile = new TenantProfile();
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(profileConfiguration);
        tenantProfile.setProfileData(profileData);
        when(tenantProfileCache.get(eq(tenantId))).thenReturn(tenantProfile);
    }

}
