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

-- UPDATE PUBLIC CUSTOMERS START

ALTER TABLE customer ADD COLUMN IF NOT EXISTS is_public boolean DEFAULT false;
UPDATE customer SET is_public = true WHERE title = 'Public';

-- UPDATE PUBLIC CUSTOMERS END

-- UPDATE CUSTOMERS WITH SAME TITLE START

CREATE OR REPLACE PROCEDURE update_customers_with_the_same_title()
    LANGUAGE plpgsql
AS
$$
DECLARE
    customer_record RECORD;
    title_exists    BOOLEAN;
    new_title       TEXT;
BEGIN
    RAISE NOTICE 'Starting the customer and edge entity groups update process.';

    FOR customer_record IN
        SELECT id, tenant_id, title, duplicate_number
        FROM (
                 SELECT
                     id,
                     tenant_id,
                     title,
                     ROW_NUMBER() OVER(PARTITION BY tenant_id, title ORDER BY id) AS duplicate_number
                 FROM customer
             ) AS duplicate_customers
        WHERE duplicate_number > 1
        LOOP
            -- Attempt with 'duplicate' suffix
            new_title := customer_record.title || ' duplicate ' || (customer_record.duplicate_number - 1)::TEXT;

            -- Check if new_title already exists for the same tenant_id
            SELECT EXISTS (
                SELECT 1
                FROM customer
                WHERE tenant_id = customer_record.tenant_id
                  AND title = new_title
            ) INTO title_exists;

            -- If generated title exists, use customer id instead to create a unique title
            IF title_exists THEN
                new_title := customer_record.title || ' duplicate ' || customer_record.id::TEXT;
            END IF;

            -- Update the customer title
            UPDATE customer
            SET title = new_title
            WHERE id = customer_record.id;
            RAISE NOTICE 'Updated customer with id: % with new title: %', customer_record.id, new_title;

            -- Update edge entity groups where the ownerId matches
            UPDATE entity_group
            SET name = CONCAT('[Edge][', new_title, ']', SUBSTRING(name FROM LENGTH('[Edge][' || customer_record.title || ']') + 1))
            WHERE owner_id = customer_record.id
              AND owner_type = 'CUSTOMER'
              AND name LIKE CONCAT('[Edge][', customer_record.title, ']%');

            RAISE NOTICE 'Updated edge entity groups for customer with id: % to reflect new title.', customer_record.id;
        END LOOP;
    RAISE NOTICE 'Customers and edge entity groups update process completed successfully!';
END;
$$;

call update_customers_with_the_same_title();

DROP PROCEDURE IF EXISTS update_customers_with_the_same_title;

-- UPDATE CUSTOMERS WITH SAME TITLE END

-- CUSTOMER UNIQUE CONSTRAINT UPDATE START

ALTER TABLE customer DROP CONSTRAINT IF EXISTS customer_title_unq_key;
ALTER TABLE customer ADD CONSTRAINT customer_title_unq_key UNIQUE (tenant_id, title);

-- CUSTOMER UNIQUE CONSTRAINT UPDATE END

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

-- OAUTH2 PARAMS ALTER TABLE START

ALTER TABLE oauth2_params
    ADD COLUMN IF NOT EXISTS edge_enabled boolean DEFAULT false;

-- OAUTH2 PARAMS ALTER TABLE END

-- QUEUE STATS UPDATE START

CREATE TABLE IF NOT EXISTS queue_stats (
    id uuid NOT NULL CONSTRAINT queue_stats_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    queue_name varchar(255) NOT NULL,
    service_id varchar(255) NOT NULL,
    CONSTRAINT queue_stats_name_unq_key UNIQUE (tenant_id, queue_name, service_id)
);

INSERT INTO queue_stats
SELECT id, created_time, tenant_id, substring(name FROM 1 FOR position('_' IN name) - 1) AS queue_name,
       substring(name FROM position('_' IN name) + 1) AS service_id
FROM asset
WHERE type = 'TbServiceQueue' and name LIKE '%\_%';

DELETE FROM asset WHERE type='TbServiceQueue';
DELETE FROM asset_profile WHERE name ='TbServiceQueue';

-- QUEUE STATS UPDATE END
