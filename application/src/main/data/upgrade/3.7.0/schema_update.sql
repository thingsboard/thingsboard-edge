--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

-- UPDATE RESOURCE SUB TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'resource' AND column_name = 'resource_sub_type'
        ) THEN
            ALTER TABLE resource ADD COLUMN resource_sub_type varchar(32);
            UPDATE resource SET resource_sub_type = 'IMAGE' WHERE resource_type = 'IMAGE';
        END IF;
    END;
$$;

-- UPDATE RESOURCE SUB TYPE END

-- UPDATE WIDGETS BUNDLE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widgets_bundle' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widgets_bundle ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGETS BUNDLE END

-- UPDATE WIDGET TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widget_type' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widget_type ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGET TYPE END

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
ALTER TABLE entity_group ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE converter ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE integration ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE role ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
ALTER TABLE scheduler_event ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- ENTITIES VERSIONING UPDATE END

-- CUSTOM MENU MIGRATION START

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS custom_menu (
    id uuid NOT NULL CONSTRAINT custom_menu_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL default '13814000-1dd2-11b2-8080-808080808080',
    menu_name varchar(255) NOT NULL UNIQUE,
    scope VARCHAR(16),
    assignee_type VARCHAR(16),
    settings VARCHAR(10000000)
);

-- migrate sys admin custom menu settings
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT s.id, s.created_time, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
            'Default system menu', 'SYSTEM', 'ALL', trim('"' FROM s.json_value::json ->> 'value') FROM admin_settings s
     WHERE key = 'customMenu') ON CONFLICT DO NOTHING;

-- migrate tenant customMenu attributes
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT uuid_generate_v4(), a.last_update_ts, entity_id, '13814000-1dd2-11b2-8080-808080808080', t.title || ' default menu','TENANT', 'ALL', str_v FROM tenant t
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'customMenu'))
ON CONFLICT DO NOTHING;

-- migrate customer customMenu attributes
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT uuid_generate_v4(), a.last_update_ts, c.tenant_id, c.id, c.title || ' default menu', 'CUSTOMER', 'ALL', str_v FROM customer c
        INNER JOIN attribute_kv a ON c.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'customMenu'))
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION update_menu_item_if_disabled(json_element jsonb, disabled_ids text[])
    RETURNS jsonb
    LANGUAGE plpgsql AS
$$
DECLARE
    updated_element jsonb;
BEGIN
    -- Check if the element has an "id" and if it's in the disabled_ids array
    IF json_element ? 'id' AND (json_element ->> 'id') = ANY (disabled_ids) THEN
        updated_element := jsonb_set(json_element, '{enabled}', 'false'::jsonb);
    ELSE
        updated_element := json_element;
    END IF;
    -- If the element has 'childMenuItems', recursively apply the function
    IF json_element ? 'childMenuItems' AND jsonb_array_length(json_element #> '{childMenuItems}') != 0 THEN
        updated_element := jsonb_set(
                updated_element,
                '{childMenuItems}',
                (SELECT jsonb_agg(update_menu_item_if_disabled(child, disabled_ids))
                 FROM jsonb_array_elements(json_element -> 'childMenuItems') AS child)
            );
    END IF;
    RETURN updated_element;
END;
$$;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_tb_user_custom_menu') THEN
            -- add predefined menu items to sys admin custom menu
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                '[{"id":"home","enabled":true},{"id":"tenants","enabled":true},{"id":"tenant_profiles","enabled":true},
                  {"id":"resources","enabled":true,"childMenuItems":[{"id":"widget_library","enabled":true},{"id":"images","enabled":true},
                  {"id":"resources_library","enabled":true}]}, {"id": "notifications_center","enabled":true}, {"id":"white_labeling","enabled":true},
                  {"id": "settings","enabled":true}, {"id":"security_settings","enabled":true,
                    "childMenuItems": [{"id":"security_settings_general","enabled":true},{"id":"2fa","enabled":true}, {"id":"oauth2","enabled":true}]}]'::jsonb
                    || (settings::jsonb #> '{menuItems}'))
            WHERE settings IS NOT NULL and tenant_id = '13814000-1dd2-11b2-8080-808080808080';

            -- add predefined menu items to tenant custom menus
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                 '[{ "id": "home","enabled":true},{"id": "alarms","enabled":true},{"id": "dashboards","enabled":true},
                  {"id": "solution_templates","enabled":true},{"id": "entities", "enabled":true, "childMenuItems":[{"id": "devices"},{"id": "assets"},{"id": "entity_views"}]},
                  {"id": "profiles", "childMenuItems":[{"id": "device_profiles"},{"id": "asset_profiles"}]},{"id": "customers"},{"id": "users"},
                  {"id": "integrations_center","enabled":true, "childMenuItems": [{"id": "integrations"},{"id": "converters"}]},{"id": "rule_chains"},{"id": "rule_chains"},
                  {"id": "edge_management", "enabled":true, "childMenuItems":[{"id": "edges"},{"id": "rulechain_templates"},{"id": "integration_templates"},
                  {"id": "integration_templates","enabled":true},{"id":"converter_templates","enabled":true}]},{"id": "features","enabled":true, "childMenuItems":[{"id": "otaUpdates","enabled":true},{"id": "version_control","enabled":true},
                  {"id": "scheduler","enabled":true}]},{"id": "resources","enabled":true, "childMenuItems":[{"id": "widget_library","enabled":true},{"id": "images","enabled":true},{"id": "resources_library","enabled":true}]},
                  {"id": "notifications_center","enabled":true},{"id": "api_usage","enabled":true},{"id": "white_labeling","enabled":true},{"id": "settings","enabled":true},
                  {"id": "security_settings","enabled":true,"childMenuItems": [{"id":"2fa","enabled":true},{"id":"roles","enabled":true},{"id":"self_registration","enabled":true}, {"id":"audit_log","enabled":true}]}]'::jsonb
                     ||(settings::jsonb #> '{menuItems}'))
            WHERE settings IS NOT NULL and customer_id = '13814000-1dd2-11b2-8080-808080808080' and tenant_id != '13814000-1dd2-11b2-8080-808080808080';

            -- add predefined menu items to customer custom menus
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                 '[{ "id": "home","enabled":true},{"id": "alarms","enabled":true},{"id": "dashboards","enabled":true},
                  {"id": "entities", "enabled":true, "childMenuItems":[{"id": "devices"},{"id": "assets"},{"id": "entity_views"}]},
                  {"id": "customers"},{"id": "users"}, {"id": "edges"},{"id": "resources","enabled":true, "childMenuItems":[{"id": "images","enabled":true}]},
                  {"id": "notifications_center","enabled":true},{"id": "scheduler","enabled":true},{"id": "white_labeling","enabled":true},{"id": "settings","enabled":true},
                  {"id": "security_settings","enabled":true,"childMenuItems": [{"id":"roles","enabled":true}, {"id":"audit_log","enabled":true}]}]'::jsonb
                     || (settings::jsonb #> '{menuItems}'))
            WHERE settings IS NOT NULL and customer_id != '13814000-1dd2-11b2-8080-808080808080';

            -- mark disabled elements
            UPDATE custom_menu SET settings = ( SELECT jsonb_agg(update_menu_item_if_disabled(elem,
                (SELECT array_agg(disableIds) FROM jsonb_array_elements_text(settings::jsonb #> '{disabledMenuItems}') AS disableIds)))
                                                FROM jsonb_array_elements(settings::jsonb #> '{menuItems}') AS elem);

            ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS custom_menu_id UUID;
            ALTER TABLE tb_user ADD CONSTRAINT fk_tb_user_custom_menu FOREIGN KEY (custom_menu_id) REFERENCES custom_menu(id);

            ALTER TABLE customer ADD COLUMN IF NOT EXISTS custom_menu_id UUID;
            ALTER TABLE customer ADD CONSTRAINT fk_customer_custom_menu FOREIGN KEY (custom_menu_id) REFERENCES custom_menu(id);
        END IF;
    END;
$$;

-- create default system menu
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
        'Default system menu', 'SYSTEM', 'ALL',
        '[{"id":"home","enabled":true},{"id":"tenants","enabled":true},{"id":"tenant_profiles","enabled":true},
        {"id":"resources","enabled":true,"childMenuItems":[{"id":"widget_library","enabled":true},{"id":"images","enabled":true},{"id":"resources_library","enabled":true}]},
        {"id": "notifications_center","enabled":true},{"id":"white_labeling","enabled":true},{"id": "settings","enabled":true},{"id":"security_settings","enabled":true,
        "childMenuItems": [{"id":"security_settings_general","enabled":true},{"id":"2fa","enabled":true},{"id":"oauth2","enabled":true}]}]')
ON CONFLICT DO NOTHING;

-- create default menu for tenant admins
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
        'Default tenant menu', 'TENANT', 'ALL',
        '[{ "id": "home","enabled":true},{"id": "alarms","enabled":true},{"id": "dashboards","enabled":true},
        {"id": "solution_templates","enabled":true},{"id": "entities", "enabled":true, "childMenuItems":[{"id": "devices"},{"id": "assets"},{"id": "entity_views"}]},
        {"id": "profiles", "childMenuItems":[{"id": "device_profiles"},{"id": "asset_profiles"}]},{"id": "customers"},{"id": "users"},
        {"id": "integrations_center","enabled":true, "childMenuItems": [{"id": "integrations"},{"id": "converters"}]},{"id": "rule_chains"},{"id": "rule_chains"},
        {"id": "edge_management", "enabled":true, "childMenuItems":[{"id": "edges"},{"id": "rulechain_templates"},{"id": "integration_templates"},
        {"id": "integration_templates","enabled":true},{"id":"converter_templates","enabled":true}]},{"id": "features","enabled":true, "childMenuItems":[{"id": "otaUpdates","enabled":true},{"id": "version_control","enabled":true},
        {"id": "scheduler","enabled":true}]},{"id": "resources","enabled":true, "childMenuItems":[{"id": "widget_library","enabled":true},{"id": "images","enabled":true},{"id": "resources_library","enabled":true}]},
        {"id": "notifications_center","enabled":true},{"id": "api_usage","enabled":true},{"id": "white_labeling","enabled":true},{"id": "settings","enabled":true},
        {"id": "security_settings","enabled":true,"childMenuItems": [{"id":"2fa","enabled":true},{"id":"roles","enabled":true},{"id":"self_registration","enabled":true}, {"id":"audit_log","enabled":true}]}]')
ON CONFLICT DO NOTHING;

-- create default menu for customer users
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
        'Default customer menu', 'CUSTOMER', 'ALL',
        '[{ "id": "home","enabled":true},{"id": "alarms","enabled":true},{"id": "dashboards","enabled":true},
        {"id": "entities", "enabled":true, "childMenuItems":[{"id": "devices"},{"id": "assets"},{"id": "entity_views"}]},
        {"id": "customers"},{"id": "users"}, {"id": "edges"},{"id": "resources","enabled":true, "childMenuItems":[{"id": "images","enabled":true}]},
        {"id": "notifications_center","enabled":true},{"id": "scheduler","enabled":true},{"id": "white_labeling","enabled":true},{"id": "settings","enabled":true},
        {"id": "security_settings","enabled":true,"childMenuItems": [{"id":"roles","enabled":true}, {"id":"audit_log","enabled":true}]}]')
ON CONFLICT DO NOTHING;

-- clear
-- DELETE FROM admin_settings WHERE key = 'customMenu';
-- DELETE FROM attribute_kv WHERE attribute_key = (select key_id from key_dictionary where key LIKE 'customMenu');
-- DROP FUNCTION update_menu_item_if_disabled;

-- CUSTOM MENU MIGRATION END
