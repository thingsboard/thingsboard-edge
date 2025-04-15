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

