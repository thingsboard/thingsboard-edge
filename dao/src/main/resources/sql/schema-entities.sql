--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

CREATE TABLE IF NOT EXISTS tb_schema_settings
(
    schema_version bigint NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);

CREATE OR REPLACE PROCEDURE insert_tb_schema_settings()
    LANGUAGE plpgsql AS
$$
BEGIN
    IF (SELECT COUNT(*) FROM tb_schema_settings) = 0 THEN
        INSERT INTO tb_schema_settings (schema_version) VALUES (3001000);
    END IF;
END;
$$;

call insert_tb_schema_settings();

CREATE TABLE IF NOT EXISTS admin_settings (
    id uuid NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    json_value varchar(10000000),
    key varchar(255)
);

CREATE TABLE IF NOT EXISTS alarm (
    id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id uuid,
    originator_type integer,
    propagate boolean,
    severity varchar(255),
    start_ts bigint,
    status varchar(255),
    tenant_id uuid,
    propagate_relation_types varchar,
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS asset (
    id uuid NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    CONSTRAINT asset_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS integration (
    id uuid NOT NULL CONSTRAINT integration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    debug_mode boolean,
    enabled boolean,
    is_remote boolean,
    name varchar(255),
    secret varchar(255),
    converter_id uuid,
    downlink_converter_id uuid,
    routing_key varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255)
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
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL CONSTRAINT audit_log_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    entity_id uuid,
    entity_type varchar(255),
    entity_name varchar(255),
    user_id uuid,
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type varchar(255),
  entity_id uuid,
  attribute_type varchar(255),
  attribute_key varchar(255),
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  json_v json,
  last_update_ts bigint,
  CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_type, entity_id, attribute_type, attribute_key)
);

CREATE INDEX IF NOT EXISTS attribute_kv_last_key ON attribute_kv(entity_id, attribute_key, last_update_ts desc);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id uuid NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    actions varchar(255),
    clazz varchar UNIQUE,
    configuration_descriptor varchar,
    name varchar(255),
    scope varchar(255),
    search_text varchar(255),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS customer (
    id uuid NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    search_text varchar(255),
    state varchar(255),
    tenant_id uuid,
    parent_customer_id uuid,
    title varchar(255),
    zip varchar(255)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id uuid NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    configuration varchar(10000000),
    assigned_customers varchar(1000000),
    search_text varchar(255),
    tenant_id uuid,
    customer_id uuid,
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS device (
    id uuid NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS device_credentials (
    id uuid NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    credentials_id varchar,
    credentials_type varchar(255),
    credentials_value varchar,
    device_id uuid,
    CONSTRAINT device_credentials_id_unq_key UNIQUE (credentials_id),
    CONSTRAINT device_credentials_device_id_unq_key UNIQUE (device_id)
);

CREATE TABLE IF NOT EXISTS event (
    id uuid NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    body varchar(10000000),
    entity_id uuid,
    entity_type varchar(255),
    event_type varchar(255),
    event_uid varchar(255),
    tenant_id uuid,
    ts bigint NOT NULL,
    CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid)
);

CREATE TABLE IF NOT EXISTS relation (
    from_id uuid,
    from_type varchar(255),
    to_id uuid,
    to_type varchar(255),
    relation_type_group varchar(255),
    relation_type varchar(255),
    additional_info varchar,
    CONSTRAINT relation_pkey PRIMARY KEY (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
);
-- ) PARTITION BY LIST (relation_type_group);

-- CREATE TABLE other_relations PARTITION OF relation DEFAULT;
-- CREATE TABLE common_relations PARTITION OF relation FOR VALUES IN ('COMMON');
-- CREATE TABLE alarm_relations PARTITION OF relation FOR VALUES IN ('ALARM');
-- CREATE TABLE dashboard_relations PARTITION OF relation FOR VALUES IN ('DASHBOARD');
-- CREATE TABLE rule_relations PARTITION OF relation FOR VALUES IN ('RULE_CHAIN', 'RULE_NODE');
-- CREATE TABLE from_group_relations PARTITION OF relation FOR VALUES IN ('FROM_ENTITY_GROUP');
-- CREATE TABLE to_group_relations PARTITION OF relation FOR VALUES IN ('TO_ENTITY_GROUP');

CREATE TABLE IF NOT EXISTS tb_user (
    id uuid NOT NULL CONSTRAINT tb_user_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    authority varchar(255),
    customer_id uuid,
    email varchar(255) UNIQUE,
    first_name varchar(255),
    last_name varchar(255),
    search_text varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS tenant (
    id uuid NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    region varchar(255),
    search_text varchar(255),
    state varchar(255),
    title varchar(255),
    zip varchar(255),
    isolated_tb_core boolean,
    isolated_tb_rule_engine boolean
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id uuid NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    activate_token varchar(255) UNIQUE,
    enabled boolean,
    password varchar(255),
    reset_token varchar(255) UNIQUE,
    user_id uuid UNIQUE
);

CREATE TABLE IF NOT EXISTS widget_type (
    id uuid NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    bundle_alias varchar(255),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS entity_group (
    id uuid NOT NULL CONSTRAINT entity_group_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    type varchar(255) NOT NULL,
    name varchar(255),
    owner_id uuid,
    owner_type varchar(255),
    additional_info varchar,
    configuration varchar(10000000),
    CONSTRAINT group_name_per_owner_unq_key UNIQUE (owner_id, owner_type, type, name)
);

CREATE TABLE IF NOT EXISTS rule_chain (
    id uuid NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    first_rule_node_id uuid,
    root boolean,
    debug_mode boolean,
    search_text varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS rule_node (
    id uuid NOT NULL CONSTRAINT rule_node_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    rule_chain_id uuid,
    additional_info varchar,
    configuration varchar(10000000),
    type varchar(255),
    name varchar(255),
    debug_mode boolean,
    search_text varchar(255)
);

CREATE TABLE IF NOT EXISTS scheduler_event (
    id uuid NOT NULL CONSTRAINT scheduler_event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
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

CREATE TABLE IF NOT EXISTS entity_view (
    id uuid NOT NULL CONSTRAINT entity_view_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    entity_id uuid,
    entity_type varchar(255),
    tenant_id uuid,
    customer_id uuid,
    type varchar(255),
    name varchar(255),
    keys varchar(10000000),
    start_ts bigint,
    end_ts bigint,
    search_text varchar(255),
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
    additional_info varchar
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

CREATE TABLE IF NOT EXISTS ts_kv_latest
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);

CREATE TABLE IF NOT EXISTS ts_kv_dictionary
(
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
);

CREATE OR REPLACE PROCEDURE cleanup_events_by_ttl(IN ttl bigint, IN debug_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    ttl_ts bigint;
    debug_ttl_ts bigint;
    ttl_deleted_count bigint DEFAULT 0;
    debug_ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF ttl > 0 THEN
        ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - ttl::bigint * 1000)::bigint;
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE ts < %L::bigint AND (event_type != %L::varchar AND event_type != %L::varchar AND event_type != %L::varchar AND event_type != %L::varchar) RETURNING *) SELECT count(*) FROM deleted', ttl_ts, 'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION') into ttl_deleted_count;
    END IF;
    IF debug_ttl > 0 THEN
        debug_ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - debug_ttl::bigint * 1000)::bigint;
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE ts < %L::bigint AND (event_type = %L::varchar OR event_type = %L::varchar OR event_type = %L::varchar OR event_type = %L::varchar) RETURNING *) SELECT count(*) FROM deleted', debug_ttl_ts, 'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION') into debug_ttl_deleted_count;
    END IF;
    RAISE NOTICE 'Events removed by ttl: %', ttl_deleted_count;
    RAISE NOTICE 'Debug Events removed by ttl: %', debug_ttl_deleted_count;
    deleted := ttl_deleted_count + debug_ttl_deleted_count;
END
$$;

CREATE OR REPLACE FUNCTION to_uuid(IN entity_id varchar, OUT uuid_id uuid) AS
$$
BEGIN
    uuid_id := substring(entity_id, 8, 8) || '-' || substring(entity_id, 4, 4) || '-1' || substring(entity_id, 1, 3) ||
               '-' || substring(entity_id, 16, 4) || '-' || substring(entity_id, 20, 12);
END;
$$ LANGUAGE plpgsql;

