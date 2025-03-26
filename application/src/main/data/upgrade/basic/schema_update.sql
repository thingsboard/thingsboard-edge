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

-- CONVERTERS 2.0 START

ALTER TABLE converter ADD COLUMN IF NOT EXISTS integration_type varchar(255);

ALTER TABLE converter ADD COLUMN IF NOT EXISTS converter_version INT DEFAULT 1;

-- CONVERTERS 2.0 END

-- UPDATE DEFAULT TENANT USERS ROLE START

UPDATE role SET permissions = '{"PROFILE":["ALL"],"ALL":["READ","RPC_CALL","READ_CREDENTIALS","READ_ATTRIBUTES","READ_TELEMETRY", "READ_CALCULATED_FIELD"]}'
            WHERE tenant_id = '13814000-1dd2-11b2-8080-808080808080' and customer_id = '13814000-1dd2-11b2-8080-808080808080' and name = 'Tenant User'
              and permissions = '{"PROFILE":["ALL"],"ALL":["READ","RPC_CALL","READ_CREDENTIALS","READ_ATTRIBUTES","READ_TELEMETRY"]}';

-- UPDATE DEFAULT TENANT USERS ROLE END

-- UPDATE SAVE TIME SERIES NODES START

UPDATE rule_node
SET configuration = (
    (configuration::jsonb - 'skipLatestPersistence')
        || jsonb_build_object(
            'processingSettings', jsonb_build_object(
                    'type',       'ADVANCED',
                    'timeseries',       jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                    'latest',           jsonb_build_object('type', 'SKIP'),
                    'webSockets',       jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                    'calculatedFields', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
                                   )
           )
    )::text,
    configuration_version = 1
WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
  AND configuration_version = 0
  AND configuration::jsonb ->> 'skipLatestPersistence' = 'true';

UPDATE rule_node
SET configuration = (
    (configuration::jsonb - 'skipLatestPersistence')
        || jsonb_build_object(
            'processingSettings', jsonb_build_object(
                    'type', 'ON_EVERY_MESSAGE'
                                   )
           )
    )::text,
    configuration_version = 1
WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
  AND configuration_version = 0
  AND (configuration::jsonb ->> 'skipLatestPersistence' != 'true' OR configuration::jsonb ->> 'skipLatestPersistence' IS NULL);

-- UPDATE SAVE TIME SERIES NODES END

-- UPDATE SAVE ATTRIBUTES NODES START

UPDATE rule_node
SET configuration = (
    configuration::jsonb
        || jsonb_build_object(
            'processingSettings', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
           )
    )::text,
    configuration_version = 3
WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode'
  AND configuration_version = 2;

-- UPDATE SAVE ATTRIBUTES NODES END

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- UPDATE TENANT PROFILE CALCULATED FIELD LIMITS START

UPDATE tenant_profile
SET profile_data = profile_data
    || jsonb_build_object(
        'configuration', profile_data->'configuration' || jsonb_build_object(
        'maxCalculatedFieldsPerEntity', COALESCE(profile_data->'configuration'->>'maxCalculatedFieldsPerEntity', '5')::bigint,
        'maxArgumentsPerCF', COALESCE(profile_data->'configuration'->>'maxArgumentsPerCF', '10')::bigint,
        'maxDataPointsPerRollingArg', COALESCE(profile_data->'configuration'->>'maxDataPointsPerRollingArg', '1000')::bigint,
        'maxStateSizeInKBytes', COALESCE(profile_data->'configuration'->>'maxStateSizeInKBytes', '32')::bigint,
        'maxSingleValueArgumentSizeInKBytes', COALESCE(profile_data->'configuration'->>'maxSingleValueArgumentSizeInKBytes', '2')::bigint
        )
    )
WHERE profile_data->'configuration'->>'maxCalculatedFieldsPerEntity' IS NULL;

-- UPDATE TENANT PROFILE CALCULATED FIELD LIMITS END

-- UPDATE TENANT PROFILE DEBUG DURATION START

UPDATE tenant_profile
SET profile_data = jsonb_set(profile_data, '{configuration,maxDebugModeDurationMinutes}', '15', true)
WHERE
    profile_data->'configuration' ? 'maxDebugModeDurationMinutes' = false
    OR (profile_data->'configuration'->>'maxDebugModeDurationMinutes')::int = 0;

-- UPDATE TENANT PROFILE DEBUG DURATION END
