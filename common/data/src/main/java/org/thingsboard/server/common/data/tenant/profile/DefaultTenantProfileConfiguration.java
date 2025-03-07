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
package org.thingsboard.server.common.data.tenant.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfileType;

import java.io.Serial;

@Schema
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DefaultTenantProfileConfiguration implements TenantProfileConfiguration {

    @Serial
    private static final long serialVersionUID = -7134932690332578595L;

    private long maxDevices;
    private long maxAssets;
    private long maxCustomers;
    private long maxUsers;
    private long maxDashboards;
    private long maxRuleChains;
    private long maxEdges;
    private long maxResourcesInBytes;
    private long maxOtaPackagesInBytes;
    private long maxResourceSize;
    private long maxIntegrations;
    private long maxConverters;
    private long maxSchedulerEvents;

    @Schema(example = "1000:1,20000:60")
    private String transportTenantMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    private String transportTenantTelemetryMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    private String transportTenantTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceTelemetryDataPointsRateLimit;

    @Schema(example = "20:1,600:60")
    private String integrationMsgsPerTenantRateLimit;
    @Schema(example = "20:1,600:60")
    private String integrationMsgsPerDeviceRateLimit;
    private String integrationMsgsPerAssetRateLimit;

    @Schema(example = "20:1,600:60")
    private String tenantEntityExportRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantEntityImportRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantNotificationRequestsRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantNotificationRequestsPerRuleRateLimit;

    @Schema(example = "10000000")
    private long maxTransportMessages;
    @Schema(example = "10000000")
    private long maxTransportDataPoints;
    @Schema(example = "4000000")
    private long maxREExecutions;
    @Schema(example = "5000000")
    private long maxJSExecutions;
    @Schema(example = "5000000")
    private long maxTbelExecutions;
    @Schema(example = "0")
    private long maxDPStorageDays;
    @Schema(example = "50")
    private int maxRuleNodeExecutionsPerMessage;
    @Schema(example = "15")
    private int maxDebugModeDurationMinutes;
    @Schema(example = "0")
    private long maxEmails;
    @Schema(example = "true")
    private Boolean smsEnabled;
    @Schema(example = "0")
    private long maxSms;
    @Schema(example = "1000")
    private long maxCreatedAlarms;

    private String tenantServerRestLimitsConfiguration;
    private String customerServerRestLimitsConfiguration;

    private int maxWsSessionsPerTenant;
    private int maxWsSessionsPerCustomer;
    private int maxWsSessionsPerRegularUser;
    private int maxWsSessionsPerPublicUser;
    private int wsMsgQueueLimitPerSession;
    private long maxWsSubscriptionsPerTenant;
    private long maxWsSubscriptionsPerCustomer;
    private long maxWsSubscriptionsPerRegularUser;
    private long maxWsSubscriptionsPerPublicUser;
    private String wsUpdatesPerSessionRateLimit;

    private String cassandraQueryTenantRateLimitsConfiguration;

    private String edgeEventRateLimits;
    private String edgeEventRateLimitsPerEdge;
    private String edgeUplinkMessagesRateLimits;
    private String edgeUplinkMessagesRateLimitsPerEdge;

    private int defaultStorageTtlDays;
    private int alarmsTtlDays;
    private int rpcTtlDays;
    private int queueStatsTtlDays;
    private int ruleEngineExceptionsTtlDays;
    private int blobEntityTtlDays;

    private double warnThreshold;

    @Schema(example = "5")
    private long maxCalculatedFieldsPerEntity = 5;
    @Schema(example = "10")
    private long maxArgumentsPerCF = 10;
    @Min(value = 0, message = "must be at least 0")
    @Schema(example = "1000")
    private long maxDataPointsPerRollingArg = 1000;
    @Schema(example = "32")
    private long maxStateSizeInKBytes = 32;
    @Schema(example = "2")
    private long maxSingleValueArgumentSizeInKBytes = 2;

    @Override
    public long getProfileThreshold(ApiUsageRecordKey key) {
        return switch (key) {
            case TRANSPORT_MSG_COUNT -> maxTransportMessages;
            case TRANSPORT_DP_COUNT -> maxTransportDataPoints;
            case JS_EXEC_COUNT -> maxJSExecutions;
            case TBEL_EXEC_COUNT -> maxTbelExecutions;
            case RE_EXEC_COUNT -> maxREExecutions;
            case STORAGE_DP_COUNT -> maxDPStorageDays;
            case EMAIL_EXEC_COUNT -> maxEmails;
            case SMS_EXEC_COUNT -> maxSms;
            case CREATED_ALARMS_COUNT -> maxCreatedAlarms;
            default -> 0L;
        };
    }

    @Override
    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        switch (key) {
            case SMS_EXEC_COUNT:
                return smsEnabled == null || Boolean.TRUE.equals(smsEnabled);
            default:
                return true;
        }
    }

    @Override
    public long getWarnThreshold(ApiUsageRecordKey key) {
        return (long) (getProfileThreshold(key) * (warnThreshold > 0.0 ? warnThreshold : 0.8));
    }

    public long getEntitiesLimit(EntityType entityType) {
        return switch (entityType) {
            case DEVICE -> maxDevices;
            case ASSET -> maxAssets;
            case CUSTOMER -> maxCustomers;
            case USER -> maxUsers;
            case DASHBOARD -> maxDashboards;
            case RULE_CHAIN -> maxRuleChains;
            case EDGE -> maxEdges;
            case INTEGRATION -> maxIntegrations;
            case CONVERTER -> maxConverters;
            case SCHEDULER_EVENT -> maxSchedulerEvents;
            default -> 0;
        };
    }

    @Override
    public TenantProfileType getType() {
        return TenantProfileType.DEFAULT;
    }

    @Override
    public int getMaxRuleNodeExecsPerMessage() {
        return maxRuleNodeExecutionsPerMessage;
    }

}
