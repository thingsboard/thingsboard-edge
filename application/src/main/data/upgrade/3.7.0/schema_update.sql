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

-- OAUTH2 UPDATE START

ALTER TABLE IF EXISTS oauth2_mobile RENAME TO mobile_app;
ALTER TABLE IF EXISTS oauth2_domain RENAME TO domain;
ALTER TABLE IF EXISTS oauth2_registration RENAME TO oauth2_client;

ALTER TABLE domain ADD COLUMN IF NOT EXISTS oauth2_enabled boolean,
                   ADD COLUMN IF NOT EXISTS edge_enabled boolean,
                   ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080',
                   DROP COLUMN IF EXISTS domain_scheme;

-- rename column domain_name to name
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='domain' and column_name='domain_name') THEN
            ALTER TABLE domain RENAME COLUMN domain_name TO name;
        END IF;
    END
$$;

-- delete duplicated domains
DELETE FROM domain d1 USING (
    SELECT MIN(ctid) as ctid, name
    FROM domain
    GROUP BY name HAVING COUNT(*) > 1
) d2 WHERE d1.name = d2.name AND d1.ctid <> d2.ctid;

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS oauth2_enabled boolean,
    ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080';

-- delete duplicated apps
DELETE FROM mobile_app m1 USING (
    SELECT MIN(ctid) as ctid, pkg_name
    FROM mobile_app
    GROUP BY pkg_name HAVING COUNT(*) > 1
) m2 WHERE m1.pkg_name = m2.pkg_name AND m1.ctid <> m2.ctid;

ALTER TABLE oauth2_client ADD COLUMN IF NOT EXISTS tenant_id uuid DEFAULT '13814000-1dd2-11b2-8080-808080808080',
                          ADD COLUMN IF NOT EXISTS title varchar(100);
UPDATE oauth2_client SET title = additional_info::jsonb->>'providerName' WHERE additional_info IS NOT NULL;

CREATE TABLE IF NOT EXISTS domain_oauth2_client (
    domain_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (domain_id) REFERENCES domain(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mobile_app_oauth2_client (
    mobile_app_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (mobile_app_id) REFERENCES mobile_app(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
);

-- migrate oauth2_params table
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'oauth2_params') THEN
            UPDATE domain SET oauth2_enabled = p.enabled,
                              edge_enabled = p.edge_enabled
            FROM oauth2_params p WHERE p.id = domain.oauth2_params_id;

            UPDATE mobile_app SET oauth2_enabled = p.enabled
            FROM oauth2_params p WHERE p.id = mobile_app.oauth2_params_id;

            INSERT INTO domain_oauth2_client(domain_id, oauth2_client_id)
                (SELECT d.id, r.id FROM domain d LEFT JOIN oauth2_client r on d.oauth2_params_id = r.oauth2_params_id
                 WHERE r.platforms IS NULL OR r.platforms IN ('','WEB'));

            INSERT INTO mobile_app_oauth2_client(mobile_app_id, oauth2_client_id)
                (SELECT m.id, r.id FROM mobile_app m LEFT JOIN oauth2_client r on m.oauth2_params_id = r.oauth2_params_id
                 WHERE r.platforms IS NULL OR r.platforms IN ('','ANDROID','IOS'));

            ALTER TABLE mobile_app RENAME CONSTRAINT oauth2_mobile_pkey TO mobile_app_pkey;
            ALTER TABLE domain RENAME CONSTRAINT oauth2_domain_pkey TO domain_pkey;
            ALTER TABLE oauth2_client RENAME CONSTRAINT oauth2_registration_pkey TO oauth2_client_pkey;

            ALTER TABLE domain DROP COLUMN oauth2_params_id;
            ALTER TABLE mobile_app DROP COLUMN oauth2_params_id;
            ALTER TABLE oauth2_client DROP COLUMN oauth2_params_id;

            ALTER TABLE mobile_app ADD CONSTRAINT mobile_app_unq_key UNIQUE (pkg_name);
            ALTER TABLE domain ADD CONSTRAINT domain_unq_key UNIQUE (name);

            DROP TABLE IF EXISTS oauth2_params;
            -- drop deprecated tables
            DROP TABLE IF EXISTS oauth2_client_registration_info;
            DROP TABLE IF EXISTS oauth2_client_registration;
        END IF;
    END
$$;

-- OAUTH2 UPDATE END

-- EDGE RELATED

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

CREATE INDEX IF NOT EXISTS idx_cloud_event_tenant_id_entity_id_event_type_event_action_crt ON cloud_event
    (tenant_id, entity_id, cloud_event_type, cloud_event_action, created_time DESC);

-- EDGE RELATED END

-- CUSTOM MENU MIGRATION START

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS custom_menu (
    id uuid NOT NULL CONSTRAINT custom_menu_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL default '13814000-1dd2-11b2-8080-808080808080',
    menu_name varchar(255) NOT NULL,
    scope VARCHAR(16),
    assignee_type VARCHAR(16),
    settings VARCHAR(10000000)
);

-- migrate sys admin custom menu settings
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM admin_settings WHERE key = 'customMenu') THEN
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
                (SELECT uuid_generate_v4(), s.created_time, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                        'System default menu', 'SYSTEM', 'ALL', trim('"' FROM s.json_value::json ->> 'value') FROM admin_settings s
                 WHERE key = 'customMenu');
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
                (SELECT uuid_generate_v4(), s.created_time, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                        'Tenant default menu', 'TENANT', 'ALL', trim('"' FROM s.json_value::json ->> 'value') FROM admin_settings s
                 WHERE key = 'customMenu');
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
                (SELECT uuid_generate_v4(), s.created_time, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                        'Customer default menu', 'CUSTOMER', 'ALL', trim('"' FROM s.json_value::json ->> 'value') FROM admin_settings s
                 WHERE key = 'customMenu');
            DELETE FROM admin_settings WHERE key = 'customMenu';
        END IF;
    END;
$$;

-- migrate tenant customMenu attributes
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT uuid_generate_v4(), a.last_update_ts, a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'Tenant default menu', 'TENANT', 'ALL', COALESCE(a.str_v, a.json_v::text) FROM tenant t
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'customMenu'));
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT uuid_generate_v4(), a.last_update_ts, a.entity_id, '13814000-1dd2-11b2-8080-808080808080', 'Customer default menu', 'CUSTOMER', 'ALL', COALESCE(a.str_v, a.json_v::text) FROM tenant t
        INNER JOIN attribute_kv a ON t.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'customMenu'));

-- migrate customer customMenu attributes
INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type, settings)
    (SELECT uuid_generate_v4(), a.last_update_ts, c.tenant_id, c.id, 'Customer default menu', 'CUSTOMER', 'ALL' , COALESCE(a.str_v, a.json_v::text) FROM customer c
        INNER JOIN attribute_kv a ON c.id = a.entity_id AND a.attribute_type = 2 AND a.attribute_key = (select key_id from key_dictionary  where key = 'customMenu'));

DELETE FROM attribute_kv WHERE attribute_key = (select key_id from key_dictionary where key LIKE 'customMenu');
-- delete not valid records
DELETE FROM custom_menu WHERE settings IS NULL OR settings = '';

CREATE OR REPLACE FUNCTION update_menu_item_with_visible_and_type(json_element jsonb, disabled_ids text[])
    RETURNS jsonb
    LANGUAGE plpgsql AS
$$
DECLARE
    updated_element jsonb;
BEGIN
    -- Check if the element has an "id" and if it's in the disabled_ids array
    IF json_element ? 'id' THEN
        IF (json_element ->> 'id') = 'home' THEN
            updated_element := json_element::jsonb || '{"visible":true, "type":"HOME", "homeType":"DEFAULT"}'::jsonb;
        ELSE
            IF (json_element ->> 'id') = ANY (disabled_ids) THEN
            updated_element := json_element::jsonb || '{"visible":false, "type": "DEFAULT"}'::jsonb;
            ELSE
                updated_element := json_element::jsonb || '{"visible":true, "type": "DEFAULT"}'::jsonb;
            END IF;
        END IF;
    ELSE
        IF (json_element ? 'pages' AND jsonb_array_length(json_element #> '{pages}') != 0) THEN
            updated_element := json_element::jsonb || '{"visible":true, "type": "CUSTOM", "menuItemType": "SECTION"}'::jsonb;
        ELSE
            IF (json_element->'dashboardId' IS NOT NULL AND json_element->>'dashboardId' <> '') THEN
                updated_element := json_element::jsonb || '{"visible":true, "type": "CUSTOM", "menuItemType": "LINK", "linkType": "DASHBOARD"}'::jsonb;
            ELSE
                updated_element := json_element::jsonb || '{"visible":true, "type": "CUSTOM", "menuItemType": "LINK", "linkType": "URL"}'::jsonb;
            END IF;
        END IF;
    END IF;
    -- If the element has 'childMenuItems', recursively apply the function
    IF json_element ? 'pages' AND jsonb_array_length(json_element #> '{pages}') != 0 THEN
        updated_element := jsonb_set(
                updated_element,
                '{pages}',
                (SELECT jsonb_agg(update_menu_item_with_visible_and_type(child, disabled_ids))
                 FROM jsonb_array_elements(json_element -> 'pages') AS child)
            );
    END IF;
    RETURN updated_element;
END;
$$;

DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF (SELECT settings::jsonb -> 'disabledMenuItems' FROM custom_menu LIMIT 1) IS NOT NULL THEN
            -- rename childMenuItems -> pages, materialIcon -> icon, iframeUrl -> url
            UPDATE custom_menu SET settings = replace(settings::TEXT, 'childMenuItems', 'pages')::jsonb;
            UPDATE custom_menu SET settings = replace(settings::TEXT, 'materialIcon', 'icon')::jsonb;
            UPDATE custom_menu SET settings = replace(settings::TEXT, 'iframeUrl', 'url')::jsonb;

            -- add predefined menu items to sys admin custom menu
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                '[
                  {"id": "home"},
                  {"id": "tenants"},
                  {"id": "tenant_profiles"},
                  {
                    "id": "resources",
                    "pages": [
                      {
                        "id": "widget_library",
                        "pages": [
                          {"id": "widget_types"},
                          {"id": "widgets_bundles"}
                        ]
                      },
                      {"id": "images"},
                      {"id": "scada_symbols"},
                      {"id": "resources_library"}
                    ]
                  },
                  {
                    "id": "notifications_center",
                    "pages": [
                      {"id": "notification_inbox"},
                      {"id": "notification_sent"},
                      {"id": "notification_recipients"},
                      {"id": "notification_templates"},
                      {"id": "notification_rules"}
                    ]
                  },
                  {
                    "id": "white_labeling",
                    "pages": [
                      {"id": "white_labeling_general"},
                      {"id": "login_white_labeling"},
                      {"id": "mail_templates"},
                      {"id": "custom_translation"},
                      {"id": "custom_menu"}
                    ]
                  },
                  {
                    "id": "settings",
                    "pages": [
                      {"id": "general"},
                      {"id": "mail_server"},
                      {"id": "notification_settings"},
                      {"id": "queues"},
                      {"id": "mobile_app_settings"}
                    ]
                  },
                  {
                    "id": "security_settings",
                    "pages": [
                      {"id": "security_settings_general"},
                      {"id": "two_fa"},
                      {"id": "oauth2"}
                    ]
                  }
                ]'::jsonb || (coalesce(settings::jsonb #> '{menuItems}', '[]'::jsonb)))
            WHERE settings IS NOT NULL and tenant_id = '13814000-1dd2-11b2-8080-808080808080';

            -- add predefined menu items to tenant custom menus
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                 '[
                   {"id": "home"},
                   {"id": "alarms"},
                   {
                     "id": "dashboards",
                     "pages": [
                       {"id": "dashboard_all"},
                       {"id": "dashboard_groups"},
                       {"id": "dashboard_shared"}
                     ]
                   },
                   {"id": "solution_templates"},
                   {
                     "id": "entities",
                     "pages": [
                       {
                         "id": "devices",
                         "pages": [
                           {"id": "device_all"},
                           {"id": "device_groups"},
                           {"id": "device_shared"}
                         ]
                       },
                       {
                         "id": "assets",
                         "pages": [
                           {"id": "asset_all"},
                           {"id": "asset_groups"},
                           {"id": "asset_shared"}
                         ]
                       },
                       {
                         "id": "entity_views",
                         "pages": [
                           {"id": "entity_view_all"},
                           {"id": "entity_view_groups"},
                           {"id": "entity_view_shared"}
                         ]
                       }
                     ]
                   },
                   {
                     "id": "profiles",
                     "pages": [
                       {"id": "device_profiles"},
                       {"id": "asset_profiles"}
                     ]
                   },
                   {
                     "id": "customers",
                     "pages": [
                       {"id": "customer_all"},
                       {"id": "customer_groups"},
                       {"id": "customer_shared"},
                       {"id": "customers_hierarchy"}
                     ]
                   },
                   {
                     "id": "users",
                     "pages": [
                       {"id": "user_all"},
                       {"id": "user_groups"}
                     ]
                   },
                   {
                     "id": "integrations_center",
                     "pages": [
                       {"id": "integrations"},
                       {"id": "converters"}
                     ]
                   },
                   {"id": "rule_chains"},
                   {
                     "id": "edge_management",
                     "pages": [
                       {
                         "id": "edges",
                         "pages": [
                           {"id": "edge_all"},
                           {"id": "edge_groups"},
                           {"id": "edge_shared"}
                         ]
                       },
                       {"id": "rulechain_templates"},
                       {"id": "integration_templates"},
                       {"id": "converter_templates"}
                     ]
                   },
                   {
                     "id": "features",
                     "pages": [
                       {"id": "otaUpdates"},
                       {"id": "version_control"},
                       {"id": "scheduler"}
                     ]
                   },
                   {
                     "id": "resources",
                     "pages": [
                       {
                         "id": "widget_library",
                         "pages": [
                           {"id": "widget_types"},
                           {"id": "widgets_bundles"}
                         ]
                       },
                       {"id": "images"},
                       {"id": "scada_symbols"},
                       {"id": "resources_library"}
                     ]
                   },
                   {
                     "id": "notifications_center",
                     "pages": [
                       {"id": "notification_inbox"},
                       {"id": "notification_sent"},
                       {"id": "notification_recipients"},
                       {"id": "notification_templates"},
                       {"id": "notification_rules"}
                     ]
                   },
                   {"id": "api_usage"},
                   {
                     "id": "white_labeling",
                     "pages": [
                       {"id": "white_labeling_general"},
                       {"id": "login_white_labeling"},
                       {"id": "mail_templates"},
                       {"id": "custom_translation"},
                       {"id": "custom_menu"}
                     ]
                   },
                   {
                     "id": "settings",
                     "pages": [
                       {"id": "home_settings"},
                       {"id": "mail_server"},
                       {"id": "notification_settings"},
                       {"id": "repository_settings"},
                       {"id": "auto_commit_settings"},
                       {"id": "mobile_app_settings"}
                     ]
                   },
                   {
                     "id": "security_settings",
                     "pages": [
                       {"id": "two_fa"},
                       {"id": "roles"},
                       {"id": "self_registration"},
                       {"id": "audit_log"}
                     ]
                   }
                 ]'::jsonb ||(coalesce(settings::jsonb #> '{menuItems}', '[]'::jsonb)))
            WHERE settings IS NOT NULL and customer_id = '13814000-1dd2-11b2-8080-808080808080' and tenant_id != '13814000-1dd2-11b2-8080-808080808080';

            -- add predefined menu items to customer custom menus
            UPDATE custom_menu SET settings = jsonb_set(settings::jsonb, '{menuItems}',
                 '[
                     {"id": "home"},
                     {"id": "alarms"},
                     {
                       "id": "dashboards",
                       "pages": [
                         {"id": "dashboard_all"},
                         {"id": "dashboard_groups"},
                         {"id": "dashboard_shared"}
                       ]
                     },
                     {
                       "id": "entities",
                       "pages": [
                         {
                           "id": "devices",
                           "pages": [
                             {"id": "device_all"},
                             {"id": "device_groups"},
                             {"id": "device_shared"}
                           ]
                         },
                         {
                           "id": "assets",
                           "pages": [
                             {"id": "asset_all"},
                             {"id": "asset_groups"},
                             {"id": "asset_shared"}
                           ]
                         },
                         {
                           "id": "entity_views",
                           "pages": [
                             {"id": "entity_view_all"},
                             {"id": "entity_view_groups"},
                             {"id": "entity_view_shared"}
                           ]
                         }
                       ]
                     },
                     {
                       "id": "customers",
                       "pages": [
                         {"id": "customer_all"},
                         {"id": "customer_groups"},
                         {"id": "customer_shared"},
                         {"id": "customers_hierarchy"}
                       ]
                     },
                     {
                       "id": "users",
                       "pages": [
                         {"id": "user_all"},
                         {"id": "user_groups"}
                       ]
                     },
                     {
                       "id": "edge_instances",
                       "pages": [
                         {"id": "edge_all"},
                         {"id": "edge_groups"},
                         {"id": "edge_shared"}
                       ]
                     },
                     {
                       "id": "resources",
                       "pages": [
                         {"id": "images"},
                         {"id": "scada_symbols"}
                       ]
                     },
                     {
                       "id": "notifications_center",
                       "pages": [
                         {"id": "notification_inbox"}
                       ]
                     },
                     {"id": "scheduler"},
                     {
                       "id": "white_labeling",
                       "pages": [
                         {"id": "white_labeling_general"},
                         {"id": "login_white_labeling"},
                         {"id": "custom_translation"},
                         {"id": "custom_menu"}
                       ]
                     },
                     {
                       "id": "settings",
                       "pages": [
                         {"id": "home_settings"}
                       ]
                     },
                   {
                     "id": "security_settings",
                       "pages": [
                         {"id": "roles"},
                         {"id": "audit_log"}
                       ]
                     }
                 ]'::jsonb || (coalesce(settings::jsonb #> '{menuItems}', '[]'::jsonb)))
            WHERE settings IS NOT NULL and customer_id != '13814000-1dd2-11b2-8080-808080808080';

            -- for each item add visible/not visible, add type: home, default or custom
            UPDATE custom_menu SET settings = json_build_object('items', (SELECT jsonb_agg(update_menu_item_with_visible_and_type(elem,
                (SELECT array_agg(disableIds) FROM jsonb_array_elements_text(settings::jsonb #> '{disabledMenuItems}') AS disableIds)))
                                                FROM jsonb_array_elements(settings::jsonb #> '{menuItems}') AS elem));
        END IF;
    END;
$$;

DROP FUNCTION update_menu_item_with_visible_and_type;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_tb_user_custom_menu') THEN
            ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS custom_menu_id UUID;
            ALTER TABLE tb_user ADD CONSTRAINT fk_tb_user_custom_menu FOREIGN KEY (custom_menu_id) REFERENCES custom_menu(id);

            ALTER TABLE customer ADD COLUMN IF NOT EXISTS custom_menu_id UUID;
            ALTER TABLE customer ADD CONSTRAINT fk_customer_custom_menu FOREIGN KEY (custom_menu_id) REFERENCES custom_menu(id);
        END IF;
    END;
$$;

-- create default system menu if not exists
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF NOT EXISTS(SELECT 1 FROM custom_menu WHERE tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND scope = 'SYSTEM' AND assignee_type = 'ALL') THEN
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type)
            VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                    'System default menu', 'SYSTEM', 'ALL');
        END IF;
    END;
$$;

-- create default menu for tenant admins
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF NOT EXISTS(SELECT 1 FROM custom_menu WHERE tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND scope = 'TENANT' AND assignee_type = 'ALL') THEN
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type)
            VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                    'Tenant default menu', 'TENANT', 'ALL');
        END IF;
    END;
$$;

-- create default menu for customer users
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time
        IF NOT EXISTS(SELECT 1 FROM custom_menu WHERE tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND scope = 'CUSTOMER' AND assignee_type = 'ALL') THEN
            INSERT INTO custom_menu(id, created_time, tenant_id, customer_id, menu_name, scope, assignee_type)
            VALUES (uuid_generate_v4(), (extract(epoch from now()) * 1000), '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080',
                    'Customer default menu', 'CUSTOMER', 'ALL');
        END IF;
    END;
$$;

-- CUSTOM MENU MIGRATION END
