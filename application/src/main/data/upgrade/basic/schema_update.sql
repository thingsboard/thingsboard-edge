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

-- UPDATE SAVE TIME SERIES NODES START

DO $$
    BEGIN
        -- Check if the rule_node table exists
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_name = 'rule_node'
        ) THEN

            UPDATE rule_node
            SET configuration = (
                (configuration::jsonb - 'skipLatestPersistence')
                    || jsonb_build_object(
                        'persistenceSettings', jsonb_build_object(
                                'type',       'ADVANCED',
                                'timeseries', jsonb_build_object('type', 'ON_EVERY_MESSAGE'),
                                'latest',     jsonb_build_object('type', 'SKIP'),
                                'webSockets', jsonb_build_object('type', 'ON_EVERY_MESSAGE')
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
                        'persistenceSettings', jsonb_build_object(
                                'type', 'ON_EVERY_MESSAGE'
                                               )
                       )
                )::text,
                configuration_version = 1
            WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode'
              AND configuration_version = 0
              AND (configuration::jsonb ->> 'skipLatestPersistence' != 'true' OR configuration::jsonb ->> 'skipLatestPersistence' IS NULL);

        END IF;
    END;
$$;

-- UPDATE SAVE TIME SERIES NODES END

ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS has_secrets boolean default false;
