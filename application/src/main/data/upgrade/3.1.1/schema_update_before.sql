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

CREATE TABLE IF NOT EXISTS oauth2_client_registration (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    client_registration_info_id uuid
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


CREATE TABLE IF NOT EXISTS device_profile (
    id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    type varchar(255),
    transport_type varchar(255),
    provision_type varchar(255),
    profile_data jsonb,
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    default_rule_chain_id uuid,
    default_queue_name varchar(255),
    provision_device_key varchar,
    CONSTRAINT device_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT device_provision_key_unq_key UNIQUE (provision_device_key),
    CONSTRAINT fk_default_rule_chain_device_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id)
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

CREATE OR REPLACE PROCEDURE update_tenant_profiles()
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = false AND isolated_tb_rule_engine = false) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = false AND t.isolated_tb_rule_engine = false;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = true AND isolated_tb_rule_engine = false) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = true AND t.isolated_tb_rule_engine = false;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = false AND isolated_tb_rule_engine = true) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = false AND t.isolated_tb_rule_engine = true;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = true AND isolated_tb_rule_engine = true) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = true AND t.isolated_tb_rule_engine = true;
END;
$$;

CREATE OR REPLACE PROCEDURE update_device_profiles()
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE device as d SET device_profile_id = p.id, device_data = '{"configuration":{"type":"DEFAULT"}, "transportConfiguration":{"type":"DEFAULT"}}'
        FROM
           (SELECT id, tenant_id, name from device_profile) as p
                WHERE d.device_profile_id IS NULL AND p.tenant_id = d.tenant_id AND d.type = p.name;
END;
$$;
