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
package org.thingsboard.server.dao.util.limits;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.function.BiFunction;
import java.util.function.Function;

public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit),
    REST_REQUESTS((profileConfiguration, level) -> ((EntityId) level).getEntityType() == EntityType.TENANT ?
            profileConfiguration.getTenantServerRestLimitsConfiguration() :
            profileConfiguration.getCustomerServerRestLimitsConfiguration()),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit),
    CASSANDRA_QUERIES(DefaultTenantProfileConfiguration::getCassandraQueryTenantRateLimitsConfiguration),
    PASSWORD_RESET(true),
    TWO_FA_VERIFICATION_CODE_SEND(true),
    TWO_FA_VERIFICATION_CODE_CHECK(true);

    private final BiFunction<DefaultTenantProfileConfiguration, Object, String> configExtractor;
    @Getter
    private final boolean refillRateLimitIntervally;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor) {
        this((profileConfiguration, level) -> configExtractor.apply(profileConfiguration));
    }

    LimitedApi(BiFunction<DefaultTenantProfileConfiguration, Object, String> configExtractor) {
        this.configExtractor = configExtractor;
        this.refillRateLimitIntervally = false;
    }

    LimitedApi(boolean refillRateLimitIntervally) {
        this.configExtractor = null;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration, Object level) {
        if (configExtractor != null) {
            return configExtractor.apply(profileConfiguration, level);
        } else {
            throw new IllegalArgumentException("No tenant profile config for " + name() + " rate limits");
        }
    }

}
