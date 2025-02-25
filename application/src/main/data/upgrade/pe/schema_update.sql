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

ALTER TABLE customer ADD COLUMN IF NOT EXISTS parent_customer_id uuid;

ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS customer_id uuid;

ALTER TABLE edge_event ADD COLUMN IF NOT EXISTS entity_group_id uuid;

ALTER TABLE alarm ADD COLUMN IF NOT EXISTS propagate_to_owner_hierarchy boolean DEFAULT false;

ALTER TABLE edge ADD COLUMN IF NOT EXISTS edge_license_key varchar(30) DEFAULT 'PUT_YOUR_EDGE_LICENSE_HERE';

ALTER TABLE edge ADD COLUMN IF NOT EXISTS cloud_endpoint varchar(255) DEFAULT 'PUT_YOUR_CLOUD_ENDPOINT_HERE';

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

ALTER TABLE resource ADD COLUMN IF NOT EXISTS customer_id uuid;

ALTER TABLE qr_code_settings ADD COLUMN IF NOT EXISTS use_system_settings boolean default true;

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS custom_menu_id UUID;

ALTER TABLE customer ADD COLUMN IF NOT EXISTS custom_menu_id UUID;

ALTER TABLE mobile_app_bundle ADD COLUMN IF NOT EXISTS self_registration_config varchar(16384),
    ADD COLUMN IF NOT EXISTS terms_of_use varchar(10000000),
    ADD COLUMN IF NOT EXISTS privacy_policy varchar(10000000);

ALTER TABLE domain ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';
ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS customer_id uuid not null default '13814000-1dd2-11b2-8080-808080808080';

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS basic_parent_customer_name_pattern varchar(255);

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS basic_user_groups_name_pattern varchar(1024);

ALTER TABLE oauth2_client_registration_template ADD COLUMN IF NOT EXISTS basic_parent_customer_name_pattern varchar(255);

ALTER TABLE oauth2_client_registration_template ADD COLUMN IF NOT EXISTS basic_user_groups_name_pattern varchar(1024);
