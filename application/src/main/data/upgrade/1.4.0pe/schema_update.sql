--
-- Thingsboard OÜ ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of Thingsboard OÜ and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to Thingsboard OÜ
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

CREATE TABLE IF NOT EXISTS entity_group (
    id varchar(31) NOT NULL CONSTRAINT entity_group_pkey PRIMARY KEY,
    type varchar(255) NOT NULL,
    name varchar(255),
    additional_info varchar,
    configuration varchar(10000000)
);

CREATE TABLE IF NOT EXISTS converter (
    id varchar(31) NOT NULL CONSTRAINT converter_pkey PRIMARY KEY,
    additional_info varchar,
    configuration varchar(10000000),
    debug_mode boolean,
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS integration (
    id varchar(31) NOT NULL CONSTRAINT integration_pkey PRIMARY KEY,
    additional_info varchar,
    configuration varchar(10000000),
    debug_mode boolean,
    name varchar(255),
    converter_id varchar(31),
    downlink_converter_id varchar(31),
    routing_key varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255)
);

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

ALTER TABLE integration ADD COLUMN downlink_converter_id varchar(31);
