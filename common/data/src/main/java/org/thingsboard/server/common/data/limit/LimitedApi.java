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
package org.thingsboard.server.common.data.limit;

import lombok.Getter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Optional;
import java.util.function.Function;

@Getter
public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit, "entity version creation", true),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit, "entity version load", true),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit, "notification requests", true),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit, "notification requests per rule", false),
    REST_REQUESTS_PER_TENANT(DefaultTenantProfileConfiguration::getTenantServerRestLimitsConfiguration, "REST API requests", true),
    REST_REQUESTS_PER_CUSTOMER(DefaultTenantProfileConfiguration::getCustomerServerRestLimitsConfiguration, "REST API requests per customer", false),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit, "WS updates per session", true),
    CASSANDRA_WRITE_QUERIES_CORE(DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantCoreRateLimits, "Rest API Cassandra write queries", true),
    CASSANDRA_READ_QUERIES_CORE(DefaultTenantProfileConfiguration::getCassandraReadQueryTenantCoreRateLimits, "Rest API and WS telemetry Cassandra read queries", true),
    CASSANDRA_WRITE_QUERIES_RULE_ENGINE(DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantRuleEngineRateLimits, "Rule Engine telemetry Cassandra write queries", true),
    CASSANDRA_READ_QUERIES_RULE_ENGINE(DefaultTenantProfileConfiguration::getCassandraReadQueryTenantRuleEngineRateLimits, "Rule Engine telemetry Cassandra read queries", true),
    CASSANDRA_READ_QUERIES_MONOLITH(
            RateLimitUtil.merge(
                    DefaultTenantProfileConfiguration::getCassandraReadQueryTenantCoreRateLimits,
                    DefaultTenantProfileConfiguration::getCassandraReadQueryTenantRuleEngineRateLimits), "Monolith telemetry Cassandra read queries", true),
    CASSANDRA_WRITE_QUERIES_MONOLITH(
            RateLimitUtil.merge(
                    DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantCoreRateLimits,
                    DefaultTenantProfileConfiguration::getCassandraWriteQueryTenantRuleEngineRateLimits), "Monolith telemetry Cassandra write queries", true),
    CASSANDRA_QUERIES(null, true), // left for backward compatibility with RateLimitsNotificationInfo
    EDGE_EVENTS(DefaultTenantProfileConfiguration::getEdgeEventRateLimits, "Edge events", true),
    EDGE_EVENTS_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeEventRateLimitsPerEdge, "Edge events per edge", false),
    EDGE_UPLINK_MESSAGES(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimits, "Edge uplink messages", true),
    EDGE_UPLINK_MESSAGES_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimitsPerEdge, "Edge uplink messages per edge", false),
    INTEGRATION_MSGS_PER_TENANT(DefaultTenantProfileConfiguration::getIntegrationMsgsPerTenantRateLimit, "integration messages", true),
    INTEGRATION_MSGS_PER_DEVICE(DefaultTenantProfileConfiguration::getIntegrationMsgsPerDeviceRateLimit, "integration messages per device", false),
    INTEGRATION_MSGS_PER_ASSET(DefaultTenantProfileConfiguration::getIntegrationMsgsPerAssetRateLimit, "integration messages per asset", false),
    INTEGRATION_EVENTS(false, true),
    CONVERTER_EVENTS(false, true),
    REPORTS("reports generation", true),
    PASSWORD_RESET(false, true),
    TWO_FA_VERIFICATION_CODE_SEND(false, true),
    TWO_FA_VERIFICATION_CODE_CHECK(false, true),
    TRANSPORT_MESSAGES_PER_TENANT("transport messages", true),
    TRANSPORT_MESSAGES_PER_DEVICE("transport messages per device", false),
    TRANSPORT_MESSAGES_PER_GATEWAY("transport messages per gateway", false),
    TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE("transport messages per gateway device", false),
    EMAILS("emails sending", true),
    WS_SUBSCRIPTIONS("WS subscriptions", false),
    CALCULATED_FIELD_DEBUG_EVENTS("calculated field debug events", true);

    private final Function<DefaultTenantProfileConfiguration, String> configExtractor;
    private final boolean perTenant;
    private final boolean refillRateLimitIntervally;
    private final String label;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant) {
        this(configExtractor, label, perTenant, false);
    }

    LimitedApi(boolean perTenant, boolean refillRateLimitIntervally) {
        this(null, null, perTenant, refillRateLimitIntervally);
    }

    LimitedApi(String label, boolean perTenant) {
        this(null, label, perTenant, false);
    }

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant, boolean refillRateLimitIntervally) {
        this.configExtractor = configExtractor;
        this.label = label;
        this.perTenant = perTenant;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration) {
        return Optional.ofNullable(configExtractor)
                .map(extractor -> extractor.apply(profileConfiguration))
                .orElse(null);
    }

}
