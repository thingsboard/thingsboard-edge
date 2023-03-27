--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
    id uuid NOT NULL CONSTRAINT entity_group_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    type varchar(255) NOT NULL,
    name varchar(255),
    owner_id uuid,
    owner_type varchar(255),
    additional_info varchar,
    configuration varchar(10000000),
    external_id uuid,
    CONSTRAINT group_name_per_owner_unq_key UNIQUE (owner_id, owner_type, type, name)
);

CREATE TABLE IF NOT EXISTS converter (
    id uuid NOT NULL CONSTRAINT converter_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    debug_mode boolean,
    name varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    external_id uuid,
    is_edge_template boolean DEFAULT false
);

CREATE TABLE IF NOT EXISTS integration (
    id uuid NOT NULL CONSTRAINT integration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    debug_mode boolean,
    enabled boolean,
    is_remote boolean,
    allow_create_devices_or_assets boolean,
    name varchar(255),
    secret varchar(255),
    converter_id uuid not null,
    downlink_converter_id uuid,
    routing_key varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    external_id uuid,
    is_edge_template boolean DEFAULT false,
    CONSTRAINT fk_integration_converter FOREIGN KEY (converter_id) REFERENCES converter(id),
    CONSTRAINT fk_integration_downlink_converter FOREIGN KEY (downlink_converter_id) REFERENCES converter(id)
);

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

CREATE TABLE IF NOT EXISTS scheduler_event (
    id uuid NOT NULL CONSTRAINT scheduler_event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    originator_id uuid,
    originator_type varchar(255),
    name varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    schedule varchar,
    configuration varchar(10000000)
);

CREATE TABLE IF NOT EXISTS blob_entity (
    id uuid NOT NULL CONSTRAINT blob_entity_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    name varchar(255),
    type varchar(255),
    content_type varchar(255),
    search_text varchar(255),
    data varchar(10485760),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS role (
    id uuid NOT NULL CONSTRAINT role_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    name varchar(255),
    type varchar(255),
    search_text varchar(255),
    permissions varchar(10000000),
    additional_info varchar,
    external_id uuid
);

CREATE TABLE IF NOT EXISTS group_permission (
    id uuid NOT NULL CONSTRAINT group_permission_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    role_id uuid,
    user_group_id uuid,
    entity_group_id uuid,
    entity_group_type varchar(255),
    is_public boolean
);

CREATE TABLE IF NOT EXISTS device_group_ota_package (
    id uuid NOT NULL CONSTRAINT entity_group_firmware_pkey PRIMARY KEY,
    group_id uuid NOT NULL,
    ota_package_type varchar(32) NOT NULL,
    ota_package_id uuid NOT NULL,
    ota_package_update_time bigint NOT NULL,
    CONSTRAINT device_group_ota_package_unq_key UNIQUE (group_id, ota_package_type),
    CONSTRAINT fk_ota_package_device_group_ota_package FOREIGN KEY (ota_package_id) REFERENCES ota_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_group_device_group_ota_package FOREIGN KEY (group_id) REFERENCES entity_group(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS converter_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar,
    e_in_message_type varchar,
    e_in_message varchar,
    e_out_message_type varchar,
    e_out_message varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS integration_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar,
    e_message_type varchar,
    e_message varchar,
    e_status varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS raw_data_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_uuid varchar,
    e_message_type varchar,
    e_message varchar
) PARTITION BY RANGE (ts);


CREATE INDEX IF NOT EXISTS idx_entity_group_by_type_name_and_owner_id ON entity_group(type, name, owner_id);

CREATE INDEX IF NOT EXISTS idx_converter_external_id ON converter(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_integration_external_id ON integration(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_role_external_id ON role(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_entity_group_external_id ON entity_group(external_id);

CREATE INDEX IF NOT EXISTS idx_converter_debug_event_main
    ON converter_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_integration_debug_event_main
    ON integration_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_raw_data_event_main
    ON raw_data_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

DROP VIEW IF EXISTS integration_info CASCADE;
CREATE OR REPLACE VIEW integration_info as
SELECT created_time, id, tenant_id, name, type, debug_mode, enabled, is_remote,
       allow_create_devices_or_assets, is_edge_template, search_text,
       (SELECT cast(json_agg(element) as varchar)
        FROM (SELECT sum(se.e_messages_processed + se.e_errors_occurred) element
              FROM stats_event se
              WHERE se.tenant_id = i.tenant_id
                AND se.entity_id = i.id
                AND ts >= (EXTRACT(EPOCH FROM current_timestamp) * 1000 - 24 * 60 * 60 * 1000)::bigint

              GROUP BY ts / 3600000
              ORDER BY ts / 3600000) stats) as stats,
       (CASE WHEN i.enabled THEN
                 (SELECT cast(json_v as varchar)
                  FROM attribute_kv
                  WHERE entity_type = 'INTEGRATION'
                    AND entity_id = i.id
                    AND attribute_type = 'SERVER_SCOPE'
                    AND attribute_key LIKE 'integration_status_%'
                  ORDER BY last_update_ts
                 LIMIT 1) END) as status
FROM integration i;

DROP VIEW IF EXISTS dashboard_info_view CASCADE;
CREATE OR REPLACE VIEW dashboard_info_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DASHBOARD'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM dashboard d
         LEFT JOIN customer c ON c.id = d.customer_id;

DROP VIEW IF EXISTS asset_info_view CASCADE;
CREATE OR REPLACE VIEW asset_info_view as
SELECT a.*,
       c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = a.id
                             and re.to_type = 'ASSET'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM asset a
         LEFT JOIN customer c ON c.id = a.customer_id;

DROP VIEW IF EXISTS device_info_view CASCADE;
CREATE OR REPLACE VIEW device_info_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DEVICE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id;

DROP VIEW IF EXISTS entity_view_info_view CASCADE;
CREATE OR REPLACE VIEW entity_view_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'ENTITY_VIEW'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM entity_view e
         LEFT JOIN customer c ON c.id = e.customer_id;

DROP VIEW IF EXISTS customer_info_view CASCADE;
CREATE OR REPLACE VIEW customer_info_view as
SELECT c.*, c2.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = c.id
                             and re.to_type = 'CUSTOMER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM customer c
         LEFT JOIN customer c2 ON c2.id = c.parent_customer_id;

DROP VIEW IF EXISTS user_info_view CASCADE;
CREATE OR REPLACE VIEW user_info_view as
SELECT u.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = u.id
                             and re.to_type = 'USER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM tb_user u
         LEFT JOIN customer c ON c.id = u.customer_id;

DROP VIEW IF EXISTS edge_info_view CASCADE;
CREATE OR REPLACE VIEW edge_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
                                entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'EDGE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM edge e
         LEFT JOIN customer c ON c.id = e.customer_id;

DROP VIEW IF EXISTS entity_group_info_view CASCADE;
CREATE OR REPLACE VIEW entity_group_info_view as
SELECT eg.*,
       array_to_json(ARRAY(WITH RECURSIVE owner_ids(id, type, lvl) AS
                                              (SELECT eg.owner_id id, eg.owner_type::varchar(15) as type, 1 as lvl
                                               UNION
                                               SELECT (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN ce2.tenant_id
                                                           ELSE ce2.parent_customer_id END) as id,
                                                      (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN 'TENANT'
                                                           ELSE 'CUSTOMER' END)::varchar(15) as type,
                                                      parent.lvl + 1 as lvl
                                               FROM customer ce2, owner_ids parent WHERE ce2.id = parent.id and eg.owner_type = 'CUSTOMER')
                           SELECT json_build_object('id', id, 'entityType', type) FROM owner_ids order by lvl)) owner_ids
FROM entity_group eg;
