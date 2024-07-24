--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

-- USER CREDENTIALS UPDATE START

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS activate_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing activation tokens
UPDATE user_credentials SET activate_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE activate_token IS NOT NULL AND activate_token_exp_time IS NULL;

ALTER TABLE user_credentials ADD COLUMN IF NOT EXISTS reset_token_exp_time BIGINT;
-- Setting 24-hour TTL for existing password reset tokens
UPDATE user_credentials SET reset_token_exp_time = cast(extract(EPOCH FROM NOW()) * 1000 AS BIGINT) + 86400000
    WHERE reset_token IS NOT NULL AND reset_token_exp_time IS NULL;

UPDATE admin_settings SET json_value = (json_value::jsonb || '{"userActivationTokenTtl":24,"passwordResetTokenTtl":24}'::jsonb)::varchar
    WHERE key = 'securitySettings';

-- USER CREDENTIALS UPDATE END
