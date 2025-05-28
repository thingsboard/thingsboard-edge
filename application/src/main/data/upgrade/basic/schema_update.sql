--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of ThingsBoard, Inc. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to ThingsBoard, Inc.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
--
-- Dissemination of this information or reproduction of this material is strictly forbidden
-- unless prior written permission is obtained from COMPANY.
--
-- Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
-- managers or contractors who have executed Confidentiality and Non-disclosure agreements
-- explicitly covering such access.
--
-- The copyright notice above does not evidence any actual or intended publication
-- or disclosure  of  this source code, which includes
-- information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
-- ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
-- OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
-- THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
-- AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
-- THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
-- DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
-- OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
--

-- UPDATE INTEGRATION PROTOCOL VERSION FOR MQTT CLIENT TYPES START

UPDATE integration
SET configuration = jsonb_set(configuration::jsonb,'{clientConfiguration,protocolVersion}','"MQTT_3_1"', true)::varchar
WHERE
    NOT (configuration::jsonb)->'clientConfiguration' ? 'protocolVersion'
    AND type IN ('MQTT', 'AWS_IOT', 'IBM_WATSON_IOT', 'TTI', 'TTN');

-- Set "MQTT_3_1_1" only for AZURE_IOT_HUB
UPDATE integration
SET configuration = jsonb_set(configuration::jsonb,'{clientConfiguration,protocolVersion}','"MQTT_3_1_1"', true)::varchar
WHERE
    NOT (configuration::jsonb)->'clientConfiguration' ? 'protocolVersion'
    AND type = 'AZURE_IOT_HUB';

-- UPDATE INTEGRATION PROTOCOL VERSION FOR MQTT CLIENT TYPES END

-- UPDATE TENANT PROFILE CASSANDRA RATE LIMITS START

UPDATE tenant_profile
SET profile_data = jsonb_set(
        profile_data,
        '{configuration}',
        (
            (profile_data -> 'configuration') - 'cassandraQueryTenantRateLimitsConfiguration'
                ||
            COALESCE(
                    CASE
                        WHEN profile_data -> 'configuration' ->
                             'cassandraQueryTenantRateLimitsConfiguration' IS NOT NULL THEN
                            jsonb_build_object(
                                    'cassandraReadQueryTenantCoreRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraWriteQueryTenantCoreRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraReadQueryTenantRuleEngineRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration',
                                    'cassandraWriteQueryTenantRuleEngineRateLimits',
                                    profile_data -> 'configuration' -> 'cassandraQueryTenantRateLimitsConfiguration'
                            )
                        END,
                    '{}'::jsonb
            )
            )
                   )
WHERE profile_data -> 'configuration' ? 'cassandraQueryTenantRateLimitsConfiguration';

-- UPDATE TENANT PROFILE CASSANDRA RATE LIMITS END
