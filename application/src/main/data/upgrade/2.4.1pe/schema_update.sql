--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
    is_remote boolean,
    name varchar(255),
    secret varchar(255),
    converter_id varchar(31),
    downlink_converter_id varchar(31),
    routing_key varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255)
);

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

CREATE TABLE IF NOT EXISTS scheduler_event (
    id varchar(31) NOT NULL CONSTRAINT scheduler_event_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id varchar(31),
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255),
    schedule varchar,
    configuration varchar(10000000)
);

CREATE TABLE IF NOT EXISTS blob_entity (
    id varchar(31) NOT NULL CONSTRAINT blob_entity_pkey PRIMARY KEY,
    tenant_id varchar(31),
    customer_id varchar(31),
    name varchar(255),
    type varchar(255),
    content_type varchar(255),
    search_text varchar(255),
    data varchar(10485760),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS role (
    id varchar(31) NOT NULL CONSTRAINT role_pkey PRIMARY KEY,
    tenant_id varchar(31),
    customer_id varchar(31),
    name varchar(255),
    type varchar(255),
    search_text varchar(255),
    permissions varchar(10000000),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS group_permission (
    id varchar(31) NOT NULL CONSTRAINT group_permission_pkey PRIMARY KEY,
    tenant_id varchar(31),
    role_id varchar(31),
    user_group_id varchar(31),
    entity_group_id varchar(31),
    entity_group_type varchar(255),
    is_public boolean
);
