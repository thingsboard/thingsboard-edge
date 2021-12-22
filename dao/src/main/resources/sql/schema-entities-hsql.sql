--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

SET DATABASE SQL SYNTAX PGS TRUE;
SET DATABASE TRANSACTION CONTROL MVCC;

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
    customer_id uuid,
    propagate_relation_types varchar,
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS entity_alarm (
    tenant_id uuid NOT NULL,
    entity_type varchar(32),
    entity_id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_type varchar(255) NOT NULL,
    customer_id uuid,
    alarm_id uuid,
    CONSTRAINT entity_alarm_pkey PRIMARY KEY(entity_id, alarm_id),
    CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
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
    allow_create_devices_or_assets boolean,
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
    json_v varchar(10000000),
    last_update_ts bigint,
    CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_type, entity_id, attribute_type, attribute_key)
);

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
    title varchar(255),
    mobile_hide boolean DEFAULT false,
    mobile_order int,
    image varchar(1000000)
);

CREATE TABLE IF NOT EXISTS rule_chain (
    id uuid NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    type varchar(255),
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

CREATE TABLE IF NOT EXISTS rule_node_state (
    id uuid NOT NULL CONSTRAINT rule_node_state_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    rule_node_id uuid NOT NULL,
    entity_type varchar(32) NOT NULL,
    entity_id uuid NOT NULL,
    state_data varchar(16384) NOT NULL,
    CONSTRAINT rule_node_state_unq_key UNIQUE (rule_node_id, entity_id),
    CONSTRAINT fk_rule_node_state_node_id FOREIGN KEY (rule_node_id) REFERENCES rule_node(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ota_package (
    id uuid NOT NULL CONSTRAINT ota_package_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_profile_id uuid ,
    type varchar(32) NOT NULL,
    title varchar(255) NOT NULL,
    version varchar(255) NOT NULL,
    tag varchar(255),
    url varchar(255),
    file_name varchar(255),
    content_type varchar(255),
    checksum_algorithm varchar(32),
    checksum varchar(1020),
    data binary,
    data_size bigint,
    additional_info varchar,
    search_text varchar(255),
    CONSTRAINT ota_package_tenant_title_version_unq_key UNIQUE (tenant_id, title, version)
);

CREATE TABLE IF NOT EXISTS device_profile (
    id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    type varchar(255),
    image varchar(1000000),
    transport_type varchar(255),
    provision_type varchar(255),
    profile_data jsonb,
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    firmware_id uuid,
    software_id uuid,
    default_rule_chain_id uuid,
    default_dashboard_id uuid,
    default_queue_name varchar(255),
    provision_device_key varchar,
    CONSTRAINT device_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT device_provision_key_unq_key UNIQUE (provision_device_key),
    CONSTRAINT fk_default_rule_chain_device_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id),
    CONSTRAINT fk_default_dashboard_device_profile FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id),
    CONSTRAINT fk_firmware_device_profile FOREIGN KEY (firmware_id) REFERENCES ota_package(id),
    CONSTRAINT fk_software_device_profile FOREIGN KEY (software_id) REFERENCES ota_package(id)
);

CREATE TABLE IF NOT EXISTS device (
    id uuid NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    device_profile_id uuid NOT NULL,
    device_data jsonb,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    firmware_id uuid,
    software_id uuid,
    CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT fk_device_profile FOREIGN KEY (device_profile_id) REFERENCES device_profile(id),
    CONSTRAINT fk_firmware_device FOREIGN KEY (firmware_id) REFERENCES ota_package(id),
    CONSTRAINT fk_software_device FOREIGN KEY (software_id) REFERENCES ota_package(id)
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

CREATE TABLE IF NOT EXISTS tenant_profile (
    id uuid NOT NULL CONSTRAINT tenant_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    profile_data jsonb,
    description varchar,
    search_text varchar(255),
    is_default boolean,
    isolated_tb_core boolean,
    isolated_tb_rule_engine boolean,
    CONSTRAINT tenant_profile_name_unq_key UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS tenant (
    id uuid NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    tenant_profile_id uuid NOT NULL,
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
    CONSTRAINT fk_tenant_profile FOREIGN KEY (tenant_profile_id) REFERENCES tenant_profile(id)
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
    tenant_id uuid,
    image varchar(1000000),
    description varchar(255)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255),
    image varchar(1000000),
    description varchar(255)
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

CREATE TABLE IF NOT EXISTS ts_kv_latest (
    entity_id uuid NOT NULL,
    key int NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v varchar(10000000),
    long_v bigint,
    dbl_v double precision,
    json_v varchar(10000000),
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);

CREATE TABLE IF NOT EXISTS ts_kv_dictionary (
    key varchar(255) NOT NULL,
    key_id int GENERATED BY DEFAULT AS IDENTITY(start with 0 increment by 1) UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
);

CREATE TABLE IF NOT EXISTS oauth2_params (
    id uuid NOT NULL CONSTRAINT oauth2_params_pkey PRIMARY KEY,
    enabled boolean,
    tenant_id uuid,
    created_time bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_registration (
    id uuid NOT NULL CONSTRAINT oauth2_registration_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    additional_info varchar,
    client_id varchar(255),
    client_secret varchar(2048),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    platforms varchar(255),
    user_info_uri varchar(255),
    user_name_attribute_name varchar(255),
    jwk_set_uri varchar(255),
    client_authentication_method varchar(255),
    login_button_label varchar(255),
    login_button_icon varchar(255),
    allow_user_creation boolean,
    activate_user boolean,
    type varchar(31),
    basic_email_attribute_key varchar(31),
    basic_first_name_attribute_key varchar(31),
    basic_last_name_attribute_key varchar(31),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(255),
    basic_customer_name_pattern varchar(255),
    basic_default_dashboard_name varchar(255),
    basic_always_full_screen boolean,
    basic_parent_customer_name_pattern varchar(255),
    basic_user_groups_name_pattern varchar(1024),
    custom_url varchar(255),
    custom_username varchar(255),
    custom_password varchar(255),
    custom_send_token boolean,
    CONSTRAINT fk_registration_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth2_domain (
    id uuid NOT NULL CONSTRAINT oauth2_domain_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    CONSTRAINT fk_domain_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE,
    CONSTRAINT oauth2_domain_unq_key UNIQUE (oauth2_params_id, domain_name, domain_scheme)
);

CREATE TABLE IF NOT EXISTS oauth2_mobile (
    id uuid NOT NULL CONSTRAINT oauth2_mobile_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    pkg_name varchar(255),
    app_secret varchar(2048),
    CONSTRAINT fk_mobile_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE,
    CONSTRAINT oauth2_mobile_unq_key UNIQUE (oauth2_params_id, pkg_name)
);

CREATE TABLE IF NOT EXISTS oauth2_client_registration_template (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_template_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    provider_id varchar(255),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    user_info_uri varchar(255),
    user_name_attribute_name varchar(255),
    jwk_set_uri varchar(255),
    client_authentication_method varchar(255),
    type varchar(31),
    basic_email_attribute_key varchar(31),
    basic_first_name_attribute_key varchar(31),
    basic_last_name_attribute_key varchar(31),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(255),
    basic_customer_name_pattern varchar(255),
    basic_default_dashboard_name varchar(255),
    basic_always_full_screen boolean,
    basic_parent_customer_name_pattern varchar(255),
    basic_user_groups_name_pattern varchar(1024),
    comment varchar,
    login_button_icon varchar(255),
    login_button_label varchar(255),
    help_link varchar(255),
    CONSTRAINT oauth2_template_provider_id_unq_key UNIQUE (provider_id)
);

-- Deprecated
CREATE TABLE IF NOT EXISTS oauth2_client_registration_info (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_info_pkey PRIMARY KEY,
    enabled boolean,
    created_time bigint NOT NULL,
    additional_info varchar,
    client_id varchar(255),
    client_secret varchar(255),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    user_info_uri varchar(255),
    user_name_attribute_name varchar(255),
    jwk_set_uri varchar(255),
    client_authentication_method varchar(255),
    login_button_label varchar(255),
    login_button_icon varchar(255),
    allow_user_creation boolean,
    activate_user boolean,
    type varchar(31),
    basic_email_attribute_key varchar(31),
    basic_first_name_attribute_key varchar(31),
    basic_last_name_attribute_key varchar(31),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(255),
    basic_customer_name_pattern varchar(255),
    basic_default_dashboard_name varchar(255),
    basic_always_full_screen boolean,
    basic_parent_customer_name_pattern varchar(255),
    basic_user_groups_name_pattern varchar(1024),
    custom_url varchar(255),
    custom_username varchar(255),
    custom_password varchar(255),
    custom_send_token boolean
);

-- Deprecated
CREATE TABLE IF NOT EXISTS oauth2_client_registration (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    client_registration_info_id uuid
);

CREATE TABLE IF NOT EXISTS api_usage_state (
    id uuid NOT NULL CONSTRAINT usage_record_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    entity_type varchar(32),
    entity_id uuid,
    transport varchar(32),
    db_storage varchar(32),
    re_exec varchar(32),
    js_exec varchar(32),
    email_exec varchar(32),
    sms_exec varchar(32),
    alarm_exec varchar(32),
    CONSTRAINT api_usage_state_unq_key UNIQUE (tenant_id, entity_id)
);

CREATE TABLE IF NOT EXISTS resource (
    id uuid NOT NULL CONSTRAINT resource_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    resource_type varchar(32) NOT NULL,
    resource_key varchar(255) NOT NULL,
    search_text varchar(255),
    file_name varchar(255) NOT NULL,
    data varchar,
    CONSTRAINT resource_unq_key UNIQUE (tenant_id, resource_type, resource_key)
);

CREATE TABLE IF NOT EXISTS edge (
    id uuid NOT NULL CONSTRAINT edge_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    root_rule_chain_id uuid,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    routing_key varchar(255),
    secret varchar(255),
    edge_license_key varchar(30),
    cloud_endpoint varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT edge_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT edge_routing_key_unq_key UNIQUE (routing_key)
);

-- TODO: voba add entity_group_id to upgrade from CE
CREATE TABLE IF NOT EXISTS edge_event (
    id uuid NOT NULL CONSTRAINT edge_event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    edge_id uuid,
    edge_event_type varchar(255),
    edge_event_uid varchar(255),
    entity_id uuid,
    edge_event_action varchar(255),
    body varchar(10000000),
    tenant_id uuid,
    entity_group_id uuid,
    ts bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS edge (
    id uuid NOT NULL CONSTRAINT edge_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    root_rule_chain_id uuid,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    routing_key varchar(255),
    secret varchar(255),
    edge_license_key varchar(30),
    cloud_endpoint varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT edge_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT edge_routing_key_unq_key UNIQUE (routing_key)
);

CREATE TABLE IF NOT EXISTS edge_event (
    id uuid NOT NULL CONSTRAINT edge_event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    edge_id uuid,
    edge_event_type varchar(255),
    edge_event_uid varchar(255),
    entity_id uuid,
    edge_event_action varchar(255),
    body varchar(10000000),
    tenant_id uuid,
    entity_group_id uuid,
    ts bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS device_group_ota_package (
    id uuid NOT NULL CONSTRAINT entity_group_firmware_pkey PRIMARY KEY,
    group_id uuid NOT NULL,
    ota_package_type varchar(32) NOT NULL,
    ota_package_id uuid NOT NULL,
    ota_package_update_time bigint NOT NULL,
    CONSTRAINT device_group_ota_package_unq_key UNIQUE (group_id, ota_package_type),
    CONSTRAINT fk_ota_package_device_group_ota_package FOREIGN KEY (ota_package_id) REFERENCES ota_package(id),
    CONSTRAINT fk_entity_group_device_group_ota_package FOREIGN KEY (group_id) REFERENCES entity_group(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rpc (
    id uuid NOT NULL CONSTRAINT rpc_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_id uuid NOT NULL,
    expiration_time bigint NOT NULL,
    request varchar(10000000) NOT NULL,
    response varchar(10000000),
    additional_info varchar(10000000),
    status varchar(255) NOT NULL
);
