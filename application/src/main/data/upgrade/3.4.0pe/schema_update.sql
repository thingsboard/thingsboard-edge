--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
    configuration varchar(10000000)
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

ALTER TABLE admin_settings ALTER COLUMN json_value SET DATA TYPE varchar(10000000);

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

CREATE OR REPLACE PROCEDURE cleanup_events_by_ttl(
    IN regular_events_start_ts bigint,
    IN regular_events_end_ts bigint,
    IN debug_events_start_ts bigint,
    IN debug_events_end_ts bigint,
    INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    ttl_deleted_count bigint DEFAULT 0;
    debug_ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF regular_events_start_ts > 0 AND regular_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type != %L::varchar AND event_type != %L::varchar AND event_type != %L::varchar AND event_type != %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', regular_events_start_ts, regular_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION') into ttl_deleted_count;
    END IF;
    IF debug_events_start_ts > 0 AND debug_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type = %L::varchar OR event_type = %L::varchar OR event_type = %L::varchar OR event_type = %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', debug_events_start_ts, debug_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION') into debug_ttl_deleted_count;
    END IF;
    RAISE NOTICE 'Events removed by ttl: %', ttl_deleted_count;
    RAISE NOTICE 'Debug Events removed by ttl: %', debug_ttl_deleted_count;
    deleted := ttl_deleted_count + debug_ttl_deleted_count;
END
$$;
