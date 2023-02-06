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
package org.thingsboard.server.service.install.update;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
class RateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {

    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_TENANT_ENABLED') ?: environment.getProperty('server.rest.limits.tenant.enabled') ?: 'false' }")
    boolean tenantServerRestLimitsEnabled;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_TENANT_CONFIGURATION') ?: environment.getProperty('server.rest.limits.tenant.configuration') ?: '100:1,2000:60' }")
    String tenantServerRestLimitsConfiguration;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_CUSTOMER_ENABLED') ?: environment.getProperty('server.rest.limits.customer.enabled') ?: 'false' }")
    boolean customerServerRestLimitsEnabled;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_CUSTOMER_CONFIGURATION') ?: environment.getProperty('server.rest.limits.customer.configuration') ?: '50:1,1000:60' }")
    String customerServerRestLimitsConfiguration;

    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_TENANT') ?: environment.getProperty('server.ws.limits.max_sessions_per_tenant') ?: '0' }")
    private int maxWsSessionsPerTenant;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_CUSTOMER') ?: environment.getProperty('server.ws.limits.max_sessions_per_customer') ?: '0' }")
    private int maxWsSessionsPerCustomer;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_REGULAR_USER') ?: environment.getProperty('server.ws.limits.max_sessions_per_regular_user') ?: '0' }")
    private int maxWsSessionsPerRegularUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_PUBLIC_USER') ?: environment.getProperty('server.ws.limits.max_sessions_per_public_user') ?: '0' }")
    private int maxWsSessionsPerPublicUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_QUEUE_PER_WS_SESSION') ?: environment.getProperty('server.ws.limits.max_queue_per_ws_session') ?: '500' }")
    private int wsMsgQueueLimitPerSession;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_TENANT') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_tenant') ?: '0' }")
    private long maxWsSubscriptionsPerTenant;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_CUSTOMER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_customer') ?: '0' }")
    private long maxWsSubscriptionsPerCustomer;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_REGULAR_USER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_regular_user') ?: '0' }")
    private long maxWsSubscriptionsPerRegularUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_PUBLIC_USER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_public_user') ?: '0' }")
    private long maxWsSubscriptionsPerPublicUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_UPDATES_PER_SESSION') ?: environment.getProperty('server.ws.limits.max_updates_per_session') ?: '300:1,3000:60' }")
    private String wsUpdatesPerSessionRateLimit;

    @Value("#{ environment.getProperty('CASSANDRA_QUERY_TENANT_RATE_LIMITS_ENABLED') ?: environment.getProperty('cassandra.query.tenant_rate_limits.enabled') ?: 'false' }")
    private boolean cassandraQueryTenantRateLimitsEnabled;
    @Value("#{ environment.getProperty('CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION') ?: environment.getProperty('cassandra.query.tenant_rate_limits.configuration') ?: '1000:1,30000:60' }")
    private String cassandraQueryTenantRateLimitsConfiguration;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Override
    protected boolean forceReportTotal() {
        return true;
    }

    @Override
    protected String getName() {
        return "Rate limits updater";
    }

    @Override
    protected PageData<TenantProfile> findEntities(String id, PageLink pageLink) {
        return tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    protected void updateEntity(TenantProfile tenantProfile) {
        var profileConfiguration = tenantProfile.getDefaultProfileConfiguration();

        if (tenantServerRestLimitsEnabled && StringUtils.isNotEmpty(tenantServerRestLimitsConfiguration)) {
            profileConfiguration.setTenantServerRestLimitsConfiguration(tenantServerRestLimitsConfiguration);
        }
        if (customerServerRestLimitsEnabled && StringUtils.isNotEmpty(customerServerRestLimitsConfiguration)) {
            profileConfiguration.setCustomerServerRestLimitsConfiguration(customerServerRestLimitsConfiguration);
        }

        profileConfiguration.setMaxWsSessionsPerTenant(maxWsSessionsPerTenant);
        profileConfiguration.setMaxWsSessionsPerCustomer(maxWsSessionsPerCustomer);
        profileConfiguration.setMaxWsSessionsPerPublicUser(maxWsSessionsPerPublicUser);
        profileConfiguration.setMaxWsSessionsPerRegularUser(maxWsSessionsPerRegularUser);
        profileConfiguration.setMaxWsSubscriptionsPerTenant(maxWsSubscriptionsPerTenant);
        profileConfiguration.setMaxWsSubscriptionsPerCustomer(maxWsSubscriptionsPerCustomer);
        profileConfiguration.setMaxWsSubscriptionsPerPublicUser(maxWsSubscriptionsPerPublicUser);
        profileConfiguration.setMaxWsSubscriptionsPerRegularUser(maxWsSubscriptionsPerRegularUser);
        profileConfiguration.setWsMsgQueueLimitPerSession(wsMsgQueueLimitPerSession);
        if (StringUtils.isNotEmpty(wsUpdatesPerSessionRateLimit)) {
            profileConfiguration.setWsUpdatesPerSessionRateLimit(wsUpdatesPerSessionRateLimit);
        }

        if (cassandraQueryTenantRateLimitsEnabled && StringUtils.isNotEmpty(cassandraQueryTenantRateLimitsConfiguration)) {
            profileConfiguration.setCassandraQueryTenantRateLimitsConfiguration(cassandraQueryTenantRateLimitsConfiguration);
        }

        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

}
