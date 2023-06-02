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
package org.thingsboard.server.common.data.limit;

import lombok.Getter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Optional;
import java.util.function.Function;

public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit, "entity version creation", true),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit, "entity version load", true),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit, "notification requests", true),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit, "notification requests per rule", false),
    REST_REQUESTS_PER_TENANT(DefaultTenantProfileConfiguration::getTenantServerRestLimitsConfiguration, "REST API requests", true),
    REST_REQUESTS_PER_CUSTOMER(DefaultTenantProfileConfiguration::getCustomerServerRestLimitsConfiguration, "REST API requests per customer", false),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit, "WS updates per session", true),
    CASSANDRA_QUERIES(DefaultTenantProfileConfiguration::getCassandraQueryTenantRateLimitsConfiguration, "Cassandra queries", true),
    PASSWORD_RESET(false, true),
    TWO_FA_VERIFICATION_CODE_SEND(false, true),
    TWO_FA_VERIFICATION_CODE_CHECK(false, true),
    TRANSPORT_MESSAGES_PER_TENANT("transport messages", true),
    TRANSPORT_MESSAGES_PER_DEVICE("transport messages per device", false);

    private Function<DefaultTenantProfileConfiguration, String> configExtractor;
    @Getter
    private final boolean perTenant;
    @Getter
    private boolean refillRateLimitIntervally;
    @Getter
    private String label;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant) {
        this.configExtractor = configExtractor;
        this.label = label;
        this.perTenant = perTenant;
    }

    LimitedApi(boolean perTenant, boolean refillRateLimitIntervally) {
        this.perTenant = perTenant;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    LimitedApi(String label, boolean perTenant) {
        this.label = label;
        this.perTenant = perTenant;
    }

    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration) {
        return Optional.ofNullable(configExtractor)
                .map(extractor -> extractor.apply(profileConfiguration))
                .orElse(null);
    }

}
