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


-- create new attribute_kv table schema
DO
$$
    BEGIN
        -- in case of running the upgrade script a second time:
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name = 'attribute_kv' and column_name='entity_type') THEN
            IF EXISTS(SELECT 1 FROM pg_indexes WHERE indexname = 'idx_attribute_kv_by_key_and_last_update_ts') THEN
                ALTER INDEX idx_attribute_kv_by_key_and_last_update_ts RENAME TO idx_attribute_kv_by_key_and_last_update_ts_old;
            END IF;
            IF EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'attribute_kv_pkey') THEN
                ALTER TABLE attribute_kv RENAME CONSTRAINT attribute_kv_pkey TO attribute_kv_pkey_old;
            END IF;
            ALTER TABLE attribute_kv
                RENAME TO attribute_kv_old;
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
            ALTER TABLE ts_kv_dictionary
                RENAME CONSTRAINT ts_key_id_pkey TO key_id_pkey;
            ALTER TABLE ts_kv_dictionary
                RENAME TO key_dictionary;
        ELSE CREATE TABLE IF NOT EXISTS key_dictionary(
                key    varchar(255) NOT NULL,
                key_id serial UNIQUE,
                CONSTRAINT key_id_pkey PRIMARY KEY (key)
                );
        END IF;
    END;
$$;

-- create to_attribute_type_id
CREATE OR REPLACE FUNCTION to_attribute_type_id(IN attribute_type varchar, OUT attribute_type_id int) AS
$$
BEGIN
    CASE
       WHEN attribute_type = 'CLIENT_SCOPE' THEN
          attribute_type_id := 1;
       WHEN attribute_type = 'SERVER_SCOPE' THEN
          attribute_type_id := 2;
       WHEN attribute_type = 'SHARED_SCOPE' THEN
          attribute_type_id := 3;
    END CASE;
END;
$$ LANGUAGE plpgsql;


-- insert keys into key_dictionary
DO
$$
DECLARE
    insert_record RECORD;
    key_cursor refcursor;
BEGIN
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
        OPEN key_cursor FOR SELECT DISTINCT attribute_key
                          FROM attribute_kv_old
                          ORDER BY attribute_key;
        LOOP
            FETCH key_cursor INTO insert_record;
            EXIT WHEN NOT FOUND;
            IF NOT EXISTS(SELECT key FROM key_dictionary WHERE key = insert_record.attribute_key) THEN
                INSERT INTO key_dictionary(key) VALUES (insert_record.attribute_key);
            END IF;
        END LOOP;
        CLOSE key_cursor;
    END IF;
END;
$$;

-- create procedure to migrate all rows from attribute_kv_old to attribute_kv
CREATE OR REPLACE PROCEDURE insert_into_attribute_kv(IN path_to_file varchar)
    LANGUAGE plpgsql AS
$$
DECLARE
    row_num_old integer;
    row_num integer;
    attribute_scope_array text[];
BEGIN
    attribute_scope_array :=  ARRAY['SERVER_SCOPE', 'CLIENT_SCOPE', 'SHARED_SCOPE'];
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = 'attribute_kv_old') THEN
        EXECUTE format('COPY (SELECT records.entity_id                           AS entity_id,
                               to_attribute_type_id(records.attribute_type)  AS attribute_type,
                               records.attribute_key                         AS attribute_key,
                               records.bool_v                                AS bool_v,
                               records.str_v                                 AS str_v,
                               records.long_v                                AS long_v,
                               records.dbl_v                                 AS dbl_v,
                               records.json_v                                AS json_v,
                               records.last_update_ts                        AS last_update_ts
                   FROM (SELECT entity_id,
                               attribute_type,
                               key_id AS attribute_key,
                               bool_v,
                               str_v,
                               long_v,
                               dbl_v,
                               json_v,
                               last_update_ts
                        FROM attribute_kv_old INNER JOIN key_dictionary ON (attribute_kv_old.attribute_key = key_dictionary.key)
                        WHERE attribute_type= ANY(%L)) AS records) TO %L;', attribute_scope_array, path_to_file);
        EXECUTE format('COPY attribute_kv FROM %L', path_to_file);
        SELECT COUNT(*) INTO row_num_old FROM attribute_kv_old;
        SELECT COUNT(*) INTO row_num FROM attribute_kv;
        RAISE NOTICE 'Migrated % of % rows', row_num, row_num_old;
    END IF;
EXCEPTION
    WHEN others THEN
        ROLLBACK;
        RAISE EXCEPTION 'Error during COPY: %', SQLERRM;
END
$$;

CREATE OR REPLACE PROCEDURE drop_attribute_kv_old_table()
    LANGUAGE plpgsql AS
$$
DECLARE
    row_num integer;
BEGIN
    SELECT COUNT(*) INTO row_num FROM attribute_kv;
    IF row_num != 0 then
        DROP TABLE IF EXISTS attribute_kv_old;
        DROP PROCEDURE IF EXISTS insert_into_attribute_kv(IN path_to_file varchar);
    ELSE
        RAISE EXCEPTION 'Table attribute_kv is empty';
    END IF;
    RETURN;
END;
$$;

