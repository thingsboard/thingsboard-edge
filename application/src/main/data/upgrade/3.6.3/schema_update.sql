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

-- create new attribute_kv table schema
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time:
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'attribute_kv' and column_name='entity_type') THEN
            DROP VIEW IF EXISTS integration_info;
            DROP VIEW IF EXISTS device_info_view;
            DROP VIEW IF EXISTS device_info_active_attribute_view;
            ALTER INDEX IF EXISTS idx_attribute_kv_by_key_and_last_update_ts RENAME TO idx_attribute_kv_by_key_and_last_update_ts_old;
            IF EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'attribute_kv_pkey') THEN
                ALTER TABLE attribute_kv RENAME CONSTRAINT attribute_kv_pkey TO attribute_kv_pkey_old;
            END IF;
            ALTER TABLE attribute_kv RENAME TO attribute_kv_old;
            CREATE TABLE IF NOT EXISTS attribute_kv
            (
                entity_id uuid,
                attribute_type int,
                attribute_key int,
                bool_v boolean,
                str_v varchar(10000000),
                long_v bigint,
                dbl_v double precision,
                json_v json,
                last_update_ts bigint,
                CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key)
            );
        END IF;
    END;
$$;

-- rename ts_kv_dictionary table to key_dictionary or create table if not exists
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'ts_kv_dictionary') THEN
            ALTER TABLE ts_kv_dictionary RENAME CONSTRAINT ts_key_id_pkey TO key_dictionary_id_pkey;
            ALTER TABLE ts_kv_dictionary RENAME TO key_dictionary;
        ELSE CREATE TABLE IF NOT EXISTS key_dictionary(
                key    varchar(255) NOT NULL,
                key_id serial UNIQUE,
                CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key)
                );
        END IF;
    END;
$$;

-- insert keys into key_dictionary
DO
$$
    BEGIN
        IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
            INSERT INTO key_dictionary(key) SELECT DISTINCT attribute_key FROM attribute_kv_old ON CONFLICT DO NOTHING;
        END IF;
    END;
$$;

-- migrate attributes from attribute_kv_old to attribute_kv
DO
$$
DECLARE
    row_num_old integer;
    row_num integer;
BEGIN
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
        INSERT INTO attribute_kv(entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, json_v, last_update_ts)
            SELECT a.entity_id, CASE
                        WHEN a.attribute_type = 'CLIENT_SCOPE' THEN 1
                        WHEN a.attribute_type = 'SERVER_SCOPE' THEN 2
                        WHEN a.attribute_type = 'SHARED_SCOPE' THEN 3
                        ELSE 0
                        END,
                k.key_id,  a.bool_v, a.str_v, a.long_v, a.dbl_v, a.json_v, a.last_update_ts
                FROM attribute_kv_old a INNER JOIN key_dictionary k ON (a.attribute_key = k.key);
        SELECT COUNT(*) INTO row_num_old FROM attribute_kv_old;
        SELECT COUNT(*) INTO row_num FROM attribute_kv;
        RAISE NOTICE 'Migrated % of % rows', row_num, row_num_old;

        IF row_num != 0 THEN
            DROP TABLE IF EXISTS attribute_kv_old;
        ELSE
           RAISE EXCEPTION 'Table attribute_kv is empty';
        END IF;

        CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);
    END IF;
EXCEPTION
    WHEN others THEN
        ROLLBACK;
        RAISE EXCEPTION 'Error during COPY: %', SQLERRM;
END
$$;

-- GROUP PERMISSION INDEX CREATE START

CREATE INDEX IF NOT EXISTS idx_group_permission_tenant_id ON group_permission(tenant_id);

-- GROUP PERMISSION INDEX CREATE END

-- CUSTOM TRANSLATION MIGRATION START

CREATE TABLE IF NOT EXISTS custom_translation (
                                                  tenant_id UUID NOT NULL,
                                                  customer_id UUID NOT NULL default '13814000-1dd2-11b2-8080-808080808080',
                                                  locale_code VARCHAR(10),
    value VARCHAR(1000000),
    CONSTRAINT custom_translation_pkey PRIMARY KEY (tenant_id, customer_id, locale_code));

-- move system settings

DO
$$
DECLARE
insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT json_data.key AS locale,
                                    json_data.value AS value
                             FROM admin_settings a,
                                 json_each_text(((trim('"' FROM a.json_value::json ->> 'value'))::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND  a.key = 'customTranslation';
BEGIN
OPEN insert_cursor;
LOOP
FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
VALUES ('13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', insert_record.locale, insert_record.value);
END LOOP;
CLOSE insert_cursor;
END;
$$;

--move tenant attributes

DO
$$
DECLARE
insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT a.entity_id AS tenant_id, json_data.key AS locale,
                                    json_data.value AS value
                             FROM attribute_kv a,
                                 json_each_text((a.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.attribute_key = 'customTranslation' and a.entity_type = 'TENANT';
BEGIN
OPEN insert_cursor;
LOOP
FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
VALUES (insert_record.tenant_id, '13814000-1dd2-11b2-8080-808080808080', insert_record.locale, insert_record.value);
END LOOP;
CLOSE insert_cursor;
END;
$$;

--move custom attributes

DO
$$
DECLARE
tenantId uuid;
    insert_record  RECORD;
    insert_cursor CURSOR FOR SELECT a.entity_id AS customer_id, json_data.key AS locale,
                                    json_data.value AS value
                             FROM attribute_kv a,
                                                    json_each_text((a.str_v::json ->> 'translationMap')::json) AS json_data
                             WHERE  a.attribute_key = 'customTranslation' and a.entity_type = 'CUSTOMER';
BEGIN
OPEN insert_cursor;
LOOP
FETCH insert_cursor INTO insert_record;
        EXIT WHEN NOT FOUND;
SELECT tenant_id INTO tenantId FROM customer where id = insert_record.customer_id;
INSERT INTO custom_translation(tenant_id, customer_id, locale_code, value)
VALUES (tenantId, insert_record.customer_id, insert_record.locale, insert_record.value);
END LOOP;
CLOSE insert_cursor;
END;
$$;

-- delete settings and attributes

DELETE FROM admin_settings WHERE key = 'customTranslation';

DELETE FROM attribute_kv WHERE entity_type = 'TENANT' AND entity_id IN (SELECT id FROM TENANT)
                           AND attribute_type = 'SERVER_SCOPE' AND  attribute_key = 'customTranslation';

DELETE FROM attribute_kv WHERE entity_type = 'CUSTOMER' AND entity_id IN (SELECT id FROM CUSTOMER)
                           AND attribute_type = 'SERVER_SCOPE' AND  attribute_key = 'customTranslation';

--CUSTOM TRANSLATION MIGRATION END

