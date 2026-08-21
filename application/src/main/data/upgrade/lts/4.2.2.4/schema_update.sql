--
-- Copyright © 2016-2026 The Thingsboard Authors
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

-- FIX TIMESERIES TTL PARTITION CLEANUP ON EDGE START
--
-- Re-creates 'drop_partitions_by_system_ttl' with the corrected exclusion of 'ts_kv_cloud_event' and its
-- 'ts_kv_cloud_event_<epoch_ms>' range partitions, together with its unchanged helper
-- 'get_partition_by_system_ttl_date' so the script is self-contained.
--
-- This exists for the NO-DOWNTIME upgrade path only. SystemPatchApplier applies LTS migrations on a normal
-- service start and then refreshes schema-views.sql alone - it never runs schema-functions.sql - so this script
-- is the only thing that corrects the procedure on a same-family patch restart (e.g. 4.2.2.3 -> 4.2.2.4).
-- The offline installer path is covered separately by schema-functions.sql, which it re-executes unconditionally.
--
-- FROZEN: this is released migration history, not a live definition. Do NOT edit it to match a later change to
-- schema-ts-psql.sql / schema-functions.sql - a future correction ships as a new version directory instead.
-- (TimeseriesTtlRoutinesDuplicationTest deliberately excludes these snapshots for that reason.)

CREATE OR REPLACE FUNCTION get_partition_by_system_ttl_date(IN partition_type varchar, IN date timestamp, OUT partition varchar) AS
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
                      WHERE schemaname = current_schema()
                        AND tablename = partition) THEN
            partition := NULL;
            RAISE NOTICE 'Failed to found partition by ttl';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- edge-only START
-- 'ts_kv_cloud_event' does not exist on the Cloud. It is range-partitioned, so besides the parent table the schema
-- also holds 'ts_kv_cloud_event_<epoch_ms>' children (see SqlPartitioningRepository). They share the 'ts_kv_' prefix
-- but are not telemetry partitions, so the three loops below skip the whole prefix - otherwise
-- SPLIT_PART(partition, '_', 3) casts 'cloud' to integer and aborts the entire procedure, leaving every expired
-- ts_kv partition on disk. Cleanup of those tables is owned by CloudEventsCleanUpService, not by this procedure.
-- edge-only END
CREATE OR REPLACE PROCEDURE drop_partitions_by_system_ttl(IN partition_type varchar, IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
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
    if system_ttl IS NOT NULL AND system_ttl > 0 THEN
        date := to_timestamp(EXTRACT(EPOCH FROM current_timestamp) - system_ttl);
        partition_by_max_ttl_date := get_partition_by_system_ttl_date(partition_type, date);
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
                                     WHERE schemaname = current_schema()
                                       AND tablename like 'ts_kv_' || '%'
                                       AND tablename != 'ts_kv_latest'
                                       AND tablename != 'key_dictionary'
                                       AND tablename != 'ts_kv_indefinite'
                                       -- edge-only START
                                       AND tablename NOT LIKE 'ts_kv_cloud_event%'
                                       -- edge-only END
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
                                             WHERE schemaname = current_schema()
                                               AND tablename like 'ts_kv_' || '%'
                                               AND tablename != 'ts_kv_latest'
                                               AND tablename != 'key_dictionary'
                                               AND tablename != 'ts_kv_indefinite'
                                               -- edge-only START
                                               AND tablename NOT LIKE 'ts_kv_cloud_event%'
                                               -- edge-only END
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
                                            -- A 'ts_kv_<year>' table has no month part. It shows up once a
                                            -- year-granularity partition has been detached from ts_kv but not
                                            -- dropped, and its range covers the ttl cut-off, so it stays. Without
                                            -- NULLIF the empty token aborts the whole procedure and nothing at all
                                            -- gets dropped.
                                            partition_month := NULLIF(SPLIT_PART(partition, '_', 4), '')::integer;
                                            IF partition_month IS NULL THEN
                                                RAISE NOTICE 'Skip iteration! Partition: % is not a month partition!', partition;
                                                CONTINUE;
                                            END IF;
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
                                                     WHERE schemaname = current_schema()
                                                       AND tablename like 'ts_kv_' || '%'
                                                       AND tablename != 'ts_kv_latest'
                                                       AND tablename != 'key_dictionary'
                                                       AND tablename != 'ts_kv_indefinite'
                                                       -- edge-only START
                                                       AND tablename NOT LIKE 'ts_kv_cloud_event%'
                                                       -- edge-only END
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
                                                    -- See the MONTHS branch: a coarser 'ts_kv_<year>' table has no
                                                    -- month part and covers the ttl cut-off, so it stays.
                                                    partition_month := NULLIF(SPLIT_PART(partition, '_', 4), '')::integer;
                                                    IF partition_month IS NULL THEN
                                                        RAISE NOTICE 'Skip iteration! Partition: % is not a month partition!', partition;
                                                        CONTINUE;
                                                    END IF;
                                                    IF partition_month > partition_by_max_ttl_month::integer THEN
                                                        RAISE NOTICE 'Skip iteration! Partition: % is valid!', partition;
                                                        CONTINUE;
                                                    ELSE
                                                        IF partition_month < partition_by_max_ttl_month::integer THEN
                                                            RAISE NOTICE 'Partition to delete by max ttl: %', partition;
                                                            EXECUTE format('DROP TABLE IF EXISTS %I', partition);
                                                            deleted := deleted + 1;
                                                        ELSE
                                                            -- Same for a coarser 'ts_kv_<year>_<month>' table: no day
                                                            -- part, and its range covers the ttl cut-off day.
                                                            partition_day := NULLIF(SPLIT_PART(partition, '_', 5), '')::integer;
                                                            IF partition_day IS NULL THEN
                                                                RAISE NOTICE 'Skip iteration! Partition: % is not a day partition!', partition;
                                                                CONTINUE;
                                                            END IF;
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

-- FIX TIMESERIES TTL PARTITION CLEANUP ON EDGE END
