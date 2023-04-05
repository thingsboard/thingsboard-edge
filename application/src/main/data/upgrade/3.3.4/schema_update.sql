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

ALTER TABLE device
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE asset
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE rule_chain
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE rule_node
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE dashboard
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE widgets_bundle
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE entity_view
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE entity_group
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE converter
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE integration
    ADD COLUMN IF NOT EXISTS external_id UUID;
ALTER TABLE role
    ADD COLUMN IF NOT EXISTS external_id UUID;

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);
CREATE INDEX IF NOT EXISTS idx_entity_group_external_id ON entity_group(external_id);
CREATE INDEX IF NOT EXISTS idx_rule_node_type ON rule_node(type);

ALTER TABLE admin_settings
    ADD COLUMN IF NOT EXISTS tenant_id uuid NOT NULL DEFAULT '13814000-1dd2-11b2-8080-808080808080';

CREATE TABLE IF NOT EXISTS queue (
    id uuid NOT NULL CONSTRAINT queue_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    name varchar(255),
    topic varchar(255),
    poll_interval int,
    partitions int,
    consumer_per_partition boolean,
    pack_processing_timeout bigint,
    submit_strategy varchar(255),
    processing_strategy varchar(255),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS user_auth_settings (
    id uuid NOT NULL CONSTRAINT user_auth_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    user_id uuid UNIQUE NOT NULL CONSTRAINT fk_user_auth_settings_user_id REFERENCES tb_user(id),
    two_fa_settings varchar
);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);

DELETE FROM relation WHERE relation_type_group = 'TO_ENTITY_GROUP';

ALTER TABLE converter
    ADD COLUMN IF NOT EXISTS is_edge_template boolean DEFAULT false;

ALTER TABLE integration
    ADD COLUMN IF NOT EXISTS is_edge_template boolean DEFAULT false;

UPDATE integration SET converter_id = NULL WHERE converter_id IS NOT NULL AND NOT EXISTS (SELECT id FROM converter where converter.id = converter_id);

UPDATE integration SET downlink_converter_id = NULL WHERE downlink_converter_id IS NOT NULL AND NOT EXISTS (SELECT id FROM converter where converter.id = downlink_converter_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT id FROM integration WHERE converter_id IS NULL) THEN
        ALTER TABLE integration ALTER COLUMN converter_id SET NOT NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_integration_converter') THEN
        ALTER TABLE integration ADD CONSTRAINT fk_integration_converter FOREIGN KEY (converter_id) REFERENCES converter(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_integration_downlink_converter') THEN
        ALTER TABLE integration ADD CONSTRAINT fk_integration_downlink_converter FOREIGN KEY (downlink_converter_id) REFERENCES converter(id);
    END IF;
END;
$$;

ALTER TABLE tenant_profile DROP COLUMN IF EXISTS isolated_tb_core;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'device_external_id_unq_key') THEN
            ALTER TABLE device ADD CONSTRAINT device_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'device_profile_external_id_unq_key') THEN
            ALTER TABLE device_profile ADD CONSTRAINT device_profile_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'asset_external_id_unq_key') THEN
            ALTER TABLE asset ADD CONSTRAINT asset_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'rule_chain_external_id_unq_key') THEN
            ALTER TABLE rule_chain ADD CONSTRAINT rule_chain_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;


DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'dashboard_external_id_unq_key') THEN
            ALTER TABLE dashboard ADD CONSTRAINT dashboard_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'customer_external_id_unq_key') THEN
            ALTER TABLE customer ADD CONSTRAINT customer_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'widgets_bundle_external_id_unq_key') THEN
            ALTER TABLE widgets_bundle ADD CONSTRAINT widgets_bundle_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'entity_view_external_id_unq_key') THEN
            ALTER TABLE entity_view ADD CONSTRAINT entity_view_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'converter_external_id_unq_key') THEN
            ALTER TABLE converter ADD CONSTRAINT converter_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'integration_external_id_unq_key') THEN
            ALTER TABLE integration ADD CONSTRAINT integration_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'role_external_id_unq_key') THEN
            ALTER TABLE role ADD CONSTRAINT role_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;
