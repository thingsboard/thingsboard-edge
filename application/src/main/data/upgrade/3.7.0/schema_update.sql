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