--
-- Copyright © 2016-2024 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE INDEX IF NOT EXISTS idx_cloud_event_tenant_id_entity_id_event_type_event_action_crt ON cloud_event
    (tenant_id, entity_id, cloud_event_type, cloud_event_action, created_time DESC);

-- KV VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS attribute_kv_version_seq cache 1;
CREATE SEQUENCE IF NOT EXISTS ts_kv_latest_version_seq cache 1;

ALTER TABLE attribute_kv ADD COLUMN IF NOT EXISTS version bigint default 0;
ALTER TABLE ts_kv_latest ADD COLUMN IF NOT EXISTS version bigint default 0;

-- KV VERSIONING UPDATE END

-- RELATION VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS relation_version_seq cache 1;
ALTER TABLE relation ADD COLUMN IF NOT EXISTS version bigint default 0;

-- RELATION VERSIONING UPDATE END


-- ENTITIES VERSIONING UPDATE START

ALTER TABLE device ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE device_profile ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE device_credentials ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE asset_profile ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE entity_view ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE edge ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE widget_type ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE widgets_bundle ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- ENTITIES VERSIONING UPDATE END

-- edge-only: create new ts_kv_cloud_event table schema

CREATE TABLE IF NOT EXISTS ts_kv_cloud_event (
    seq_id INT GENERATED ALWAYS AS IDENTITY,
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    cloud_event_type varchar(255),
    entity_id uuid,
    cloud_event_action varchar(255),
    entity_body varchar(10000000),
    tenant_id uuid,
    ts bigint NOT NULL
    ) PARTITION BY RANGE(created_time);

ALTER TABLE IF EXISTS ts_kv_cloud_event ALTER COLUMN seq_id SET CYCLE;

CREATE INDEX IF NOT EXISTS idx_ts_kv_cloud_event_tenant_id_and_created_time ON ts_kv_cloud_event(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_ts_kv_cloud_event_tenant_id_entity_id_event_type_event_action_crt ON ts_kv_cloud_event
    (tenant_id, entity_id, cloud_event_type, cloud_event_action, created_time DESC);

-- edge-only: create new ts_kv_cloud_event table schema end
