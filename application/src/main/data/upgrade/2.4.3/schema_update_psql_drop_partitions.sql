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

CREATE OR REPLACE PROCEDURE drop_partitions_by_max_ttl(IN partition_type varchar, IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    max_tenant_ttl             bigint;
    max_customer_ttl           bigint;
    max_ttl                    bigint;
    date                       timestamp;
    partition_by_max_ttl_date  varchar;
    partition_by_max_ttl_month varchar;
    partition_by_max_ttl_day   varchar;
    partition_by_max_ttl_year  varchar;
    partition                  varchar;
    partition_year             integer;
    partition_month            integer;
    partition_day              integer;

BEGIN
    SELECT max(attribute_kv.long_v)
    FROM tenant
             INNER JOIN attribute_kv ON tenant.id = attribute_kv.entity_id
    WHERE attribute_kv.attribute_key = 'TTL'
    into max_tenant_ttl;
    SELECT max(attribute_kv.long_v)
    FROM customer
             INNER JOIN attribute_kv ON customer.id = attribute_kv.entity_id
    WHERE attribute_kv.attribute_key = 'TTL'
    into max_customer_ttl;
    max_ttl := GREATEST(system_ttl, max_customer_ttl, max_tenant_ttl);
    if max_ttl IS NOT NULL AND max_ttl > 0 THEN
        date := to_timestamp(EXTRACT(EPOCH FROM current_timestamp) - max_ttl);
        partition_by_max_ttl_date := get_partition_by_max_ttl_date(partition_type, date);
        RAISE NOTICE 'Date by max ttl: %', date;
        RAISE NOTICE 'Partition by max ttl: %', partition_by_max_ttl_date;
        IF partition_by_max_ttl_date IS NOT NULL THEN
            CASE
                WHEN partition_type = 'DAYS' THEN
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_by_max_ttl_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                    partition_by_max_ttl_day := SPLIT_PART(partition_by_max_ttl_date, '_', 5);
                WHEN partition_type = 'MONTHS' THEN
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                    partition_by_max_ttl_month := SPLIT_PART(partition_by_max_ttl_date, '_', 4);
                ELSE
                    partition_by_max_ttl_year := SPLIT_PART(partition_by_max_ttl_date, '_', 3);
                END CASE;
            IF partition_by_max_ttl_year IS NULL THEN
                RAISE NOTICE 'Failed to remove partitions by max ttl date due to partition_by_max_ttl_year is null!';
            ELSE
                IF partition_type = 'YEARS' THEN
                    FOR partition IN SELECT tablename
                                     FROM pg_tables
                                     WHERE schemaname = 'public'
                                       AND tablename like 'ts_kv_' || '%'
                                       AND tablename != 'ts_kv_latest'
                                       AND tablename != 'ts_kv_dictionary'
                                       AND tablename != 'ts_kv_indefinite'
                                       AND tablename != partition_by_max_ttl_date
                        LOOP
                            partition_year := SPLIT_PART(partition, '_', 3)::integer;
                            IF partition_year < partition_by_max_ttl_year::integer THEN
                                RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                deleted := deleted + 1;
                            END IF;
                        END LOOP;
                ELSE
                    IF partition_type = 'MONTHS' THEN
                        IF partition_by_max_ttl_month IS NULL THEN
                            RAISE NOTICE 'Failed to remove months partitions by max ttl date due to partition_by_max_ttl_month is null!';
                        ELSE
                            FOR partition IN SELECT tablename
                                             FROM pg_tables
                                             WHERE schemaname = 'public'
                                               AND tablename like 'ts_kv_' || '%'
                                               AND tablename != 'ts_kv_latest'
                                               AND tablename != 'ts_kv_dictionary'
                                               AND tablename != 'ts_kv_indefinite'
                                               AND tablename != partition_by_max_ttl_date
                                LOOP
                                    partition_year := SPLIT_PART(partition, '_', 3)::integer;
                                    IF partition_year > partition_by_max_ttl_year::integer THEN
                                        RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                        CONTINUE;
                                    ELSE
                                        IF partition_year < partition_by_max_ttl_year::integer THEN
                                            RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                            EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                            deleted := deleted + 1;
                                        ELSE
                                            partition_month := SPLIT_PART(partition, '_', 4)::integer;
                                            IF partition_year = partition_by_max_ttl_year::integer THEN
                                                IF  partition_month >= partition_by_max_ttl_month::integer THEN
                                                    RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                    CONTINUE;
                                                ELSE
                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                    deleted := deleted + 1;
                                                END IF;
                                            END IF;
                                        END IF;
                                    END IF;
                                END LOOP;
                        END IF;
                    ELSE
                        IF partition_type = 'DAYS' THEN
                            IF partition_by_max_ttl_month IS NULL THEN
                                RAISE NOTICE 'Failed to remove days partitions by max ttl date due to partition_by_max_ttl_month is null!';
                            ELSE
                                IF partition_by_max_ttl_day IS NULL THEN
                                    RAISE NOTICE 'Failed to remove days partitions by max ttl date due to partition_by_max_ttl_day is null!';
                                ELSE
                                    FOR partition IN SELECT tablename
                                                     FROM pg_tables
                                                     WHERE schemaname = 'public'
                                                       AND tablename like 'ts_kv_' || '%'
                                                       AND tablename != 'ts_kv_latest'
                                                       AND tablename != 'ts_kv_dictionary'
                                                       AND tablename != 'ts_kv_indefinite'
                                                       AND tablename != partition_by_max_ttl_date
                                        LOOP
                                            partition_year := SPLIT_PART(partition, '_', 3)::integer;
                                            IF partition_year > partition_by_max_ttl_year::integer THEN
                                                RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                CONTINUE;
                                            ELSE
                                                IF partition_year < partition_by_max_ttl_year::integer THEN
                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                    deleted := deleted + 1;
                                                ELSE
                                                    partition_month := SPLIT_PART(partition, '_', 4)::integer;
                                                    IF partition_month > partition_by_max_ttl_month::integer THEN
                                                        RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                        CONTINUE;
                                                    ELSE
                                                        IF partition_month < partition_by_max_ttl_month::integer THEN
                                                            RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                            EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                            deleted := deleted + 1;
                                                        ELSE
                                                            partition_day := SPLIT_PART(partition, '_', 5)::integer;
                                                            IF partition_day >= partition_by_max_ttl_day::integer THEN
                                                                RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                                CONTINUE;
                                                            ELSE
                                                                IF partition_day < partition_by_max_ttl_day::integer THEN
                                                                    RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                                    EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                                    deleted := deleted + 1;
                                                                END IF;
                                                            END IF;
                                                        END IF;
                                                    END IF;
                                                END IF;
                                            END IF;
                                        END LOOP;
                                END IF;
                            END IF;
                        END IF;
                    END IF;
                END IF;
            END IF;
        END IF;
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION get_partition_by_max_ttl_date(IN partition_type varchar, IN date timestamp, OUT partition varchar) AS
$$
BEGIN
    CASE
        WHEN partition_type = 'DAYS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM') || '_' || to_char(date, 'dd');
        WHEN partition_type = 'MONTHS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy') || '_' || to_char(date, 'MM');
        WHEN partition_type = 'YEARS' THEN
            partition := 'ts_kv_' || to_char(date, 'yyyy');
        ELSE
            partition := NULL;
        END CASE;
    IF partition IS NOT NULL THEN
        IF NOT EXISTS(SELECT
                      FROM pg_tables
                      WHERE schemaname = 'public'
                        AND tablename = partition) THEN
            partition := NULL;
            RAISE NOTICE 'Failed to found partition by ttl';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;
