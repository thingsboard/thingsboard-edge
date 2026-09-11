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

CREATE OR REPLACE FUNCTION create_or_update_active_alarm(
                                        t_id uuid, c_id uuid, a_id uuid, a_created_ts bigint,
                                        a_o_id uuid, a_o_type integer, a_type varchar,
                                        a_severity varchar, a_start_ts bigint, a_end_ts bigint,
                                        a_details varchar,
                                        a_propagate boolean, a_propagate_to_owner boolean,
                                        a_propagate_to_tenant boolean, a_propagation_types varchar,
                                        a_creation_enabled boolean)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    null_id constant uuid = '13814000-1dd2-11b2-8080-808080808080'::uuid;
    existing  alarm;
    result    alarm_info;
    row_count integer;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.originator_id = a_o_id AND a.type = a_type AND a.cleared = false ORDER BY a.start_ts DESC FOR UPDATE;
    IF existing.id IS NULL THEN
        IF a_creation_enabled = FALSE THEN
            RETURN json_build_object('success', false)::text;
        END IF;
        IF c_id = null_id THEN
            c_id = NULL;
        end if;
        INSERT INTO alarm
            (tenant_id, customer_id, id, created_time,
             originator_id, originator_type, type,
             severity, start_ts, end_ts,
             additional_info,
             propagate, propagate_to_owner, propagate_to_tenant, propagate_relation_types,
             acknowledged, ack_ts,
             cleared, clear_ts,
             assignee_id, assign_ts)
             VALUES
            (t_id, c_id, a_id, a_created_ts,
             a_o_id, a_o_type, a_type,
             a_severity, a_start_ts, a_end_ts,
             a_details,
             a_propagate, a_propagate_to_owner, a_propagate_to_tenant, a_propagation_types,
             false, 0, false, 0, NULL, 0);
        INSERT INTO alarm_types (tenant_id, type) VALUES (t_id, a_type) ON CONFLICT (tenant_id, type) DO NOTHING;
        SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
        RETURN json_build_object('success', true, 'created', true, 'modified', true, 'alarm', row_to_json(result))::text;
    ELSE
        UPDATE alarm a
        SET severity                 = a_severity,
            start_ts                 = a_start_ts,
            end_ts                   = a_end_ts,
            additional_info          = a_details,
            propagate                = a_propagate,
            propagate_to_owner       = a_propagate_to_owner,
            propagate_to_tenant      = a_propagate_to_tenant,
            propagate_relation_types = a_propagation_types
        WHERE a.id = existing.id
          AND a.tenant_id = t_id
          AND (severity != a_severity OR start_ts != a_start_ts OR end_ts != a_end_ts OR additional_info != a_details
            OR propagate != a_propagate OR propagate_to_owner != a_propagate_to_owner OR
               propagate_to_tenant != a_propagate_to_tenant OR propagate_relation_types != a_propagation_types);
        GET DIAGNOSTICS row_count = ROW_COUNT;
        SELECT * INTO result FROM alarm_info a WHERE a.id = existing.id AND a.tenant_id = t_id;
        IF row_count > 0 THEN
            RETURN json_build_object('success', true, 'modified', true, 'alarm', row_to_json(result), 'old', row_to_json(existing))::text;
        ELSE
            RETURN json_build_object('success', true, 'modified', false, 'alarm', row_to_json(result))::text;
        END IF;
    END IF;
END
$$;

DROP FUNCTION IF EXISTS update_alarm;
CREATE OR REPLACE FUNCTION update_alarm(t_id uuid, a_id uuid, a_severity varchar, a_start_ts bigint, a_end_ts bigint,
                                        a_details varchar,
                                        a_propagate boolean, a_propagate_to_owner boolean,
                                        a_propagate_to_tenant boolean, a_propagation_types varchar)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    existing  alarm;
    result    alarm_info;
    row_count integer;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.id = a_id AND a.tenant_id = t_id FOR UPDATE;
    IF existing IS NULL THEN
        RETURN json_build_object('success', false)::text;
    END IF;
    UPDATE alarm a
    SET severity                 = a_severity,
        start_ts                 = a_start_ts,
        end_ts                   = a_end_ts,
        additional_info          = a_details,
        propagate                = a_propagate,
        propagate_to_owner       = a_propagate_to_owner,
        propagate_to_tenant      = a_propagate_to_tenant,
        propagate_relation_types = a_propagation_types
    WHERE a.id = a_id
      AND a.tenant_id = t_id
      AND (severity != a_severity OR start_ts != a_start_ts OR end_ts != a_end_ts OR additional_info != a_details
        OR propagate != a_propagate OR propagate_to_owner != a_propagate_to_owner OR
           propagate_to_tenant != a_propagate_to_tenant OR propagate_relation_types != a_propagation_types);
    GET DIAGNOSTICS row_count = ROW_COUNT;
    SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
    IF row_count > 0 THEN
        RETURN json_build_object('success', true, 'modified', row_count > 0, 'alarm', row_to_json(result), 'old', row_to_json(existing))::text;
    ELSE
        RETURN json_build_object('success', true, 'modified', row_count > 0, 'alarm', row_to_json(result))::text;
    END IF;
END
$$;

DROP FUNCTION IF EXISTS acknowledge_alarm;
CREATE OR REPLACE FUNCTION acknowledge_alarm(t_id uuid, a_id uuid, a_ts bigint)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    existing alarm;
    result   alarm_info;
    modified boolean = FALSE;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.id = a_id AND a.tenant_id = t_id FOR UPDATE;
    IF existing IS NULL THEN
        RETURN json_build_object('success', false)::text;
    END IF;

    IF NOT (existing.acknowledged) THEN
        modified = TRUE;
        UPDATE alarm a SET acknowledged = true, ack_ts = a_ts WHERE a.id = a_id AND a.tenant_id = t_id;
    END IF;
    SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
    RETURN json_build_object('success', true, 'modified', modified, 'alarm', row_to_json(result), 'old', row_to_json(existing))::text;
END
$$;

DROP FUNCTION IF EXISTS clear_alarm;
CREATE OR REPLACE FUNCTION clear_alarm(t_id uuid, a_id uuid, a_ts bigint, a_details varchar)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    existing alarm;
    result   alarm_info;
    cleared boolean = FALSE;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.id = a_id AND a.tenant_id = t_id FOR UPDATE;
    IF existing IS NULL THEN
        RETURN json_build_object('success', false)::text;
    END IF;
    IF NOT(existing.cleared) THEN
        cleared = TRUE;
        IF a_details IS NULL THEN
            UPDATE alarm a SET cleared = true, clear_ts = a_ts WHERE a.id = a_id AND a.tenant_id = t_id;
        ELSE
            UPDATE alarm a SET cleared = true, clear_ts = a_ts, additional_info = a_details WHERE a.id = a_id AND a.tenant_id = t_id;
        END IF;
    END IF;
    SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
    RETURN json_build_object('success', true, 'cleared', cleared, 'alarm', row_to_json(result))::text;
END
$$;

DROP FUNCTION IF EXISTS assign_alarm;
CREATE OR REPLACE FUNCTION assign_alarm(t_id uuid, a_id uuid, u_id uuid, a_ts bigint)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    existing alarm;
    result   alarm_info;
    modified boolean = FALSE;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.id = a_id AND a.tenant_id = t_id FOR UPDATE;
    IF existing IS NULL THEN
        RETURN json_build_object('success', false)::text;
    END IF;
    IF existing.assignee_id IS NULL OR existing.assignee_id != u_id THEN
        modified = TRUE;
        UPDATE alarm a SET assignee_id = u_id, assign_ts = a_ts WHERE a.id = a_id AND a.tenant_id = t_id;
    END IF;
    SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
    RETURN json_build_object('success', true, 'modified', modified, 'alarm', row_to_json(result))::text;
END
$$;

DROP FUNCTION IF EXISTS unassign_alarm;
CREATE OR REPLACE FUNCTION unassign_alarm(t_id uuid, a_id uuid, a_ts bigint)
    RETURNS varchar
    LANGUAGE plpgsql
AS
$$
DECLARE
    existing alarm;
    result   alarm_info;
    modified boolean = FALSE;
BEGIN
    SELECT * INTO existing FROM alarm a WHERE a.id = a_id AND a.tenant_id = t_id FOR UPDATE;
    IF existing IS NULL THEN
        RETURN json_build_object('success', false)::text;
    END IF;
    IF existing.assignee_id IS NOT NULL THEN
        modified = TRUE;
        UPDATE alarm a SET assignee_id = NULL, assign_ts = a_ts WHERE a.id = a_id AND a.tenant_id = t_id;
    END IF;
    SELECT * INTO result FROM alarm_info a WHERE a.id = a_id AND a.tenant_id = t_id;
    RETURN json_build_object('success', true, 'modified', modified, 'alarm', row_to_json(result))::text;
END
$$;

CREATE OR REPLACE PROCEDURE cleanup_timeseries_by_ttl(IN null_uuid uuid,
                                                      IN system_ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    tenant_cursor CURSOR FOR select tenant.id as tenant_id
                             from tenant;
    tenant_id_record     uuid;
    customer_id_record   uuid;
    tenant_ttl           bigint;
    customer_ttl         bigint;
    deleted_for_entities bigint;
    tenant_ttl_ts        bigint;
    customer_ttl_ts      bigint;
BEGIN
    OPEN tenant_cursor;
    FETCH tenant_cursor INTO tenant_id_record;
    WHILE FOUND
        LOOP
            EXECUTE format(
                    'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = (select key_id from key_dictionary where key = %L)',
                    tenant_id_record, 'TTL') INTO tenant_ttl;
            if tenant_ttl IS NULL THEN
                tenant_ttl := system_ttl;
            END IF;
            IF tenant_ttl > 0 THEN
                tenant_ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - tenant_ttl::bigint * 1000)::bigint;
                deleted_for_entities := delete_device_records_from_ts_kv(tenant_id_record, null_uuid, tenant_ttl_ts);
                deleted := deleted + deleted_for_entities;
                RAISE NOTICE '% telemetry removed for devices where tenant_id = %', deleted_for_entities, tenant_id_record;
                deleted_for_entities := delete_asset_records_from_ts_kv(tenant_id_record, null_uuid, tenant_ttl_ts);
                deleted := deleted + deleted_for_entities;
                RAISE NOTICE '% telemetry removed for assets where tenant_id = %', deleted_for_entities, tenant_id_record;
            END IF;
            FOR customer_id_record IN
                SELECT customer.id AS customer_id FROM customer WHERE customer.tenant_id = tenant_id_record
                LOOP
                    EXECUTE format(
                            'select attribute_kv.long_v from attribute_kv where attribute_kv.entity_id = %L and attribute_kv.attribute_key = (select key_id from key_dictionary where key = %L)',
                            customer_id_record, 'TTL') INTO customer_ttl;
                    IF customer_ttl IS NULL THEN
                        customer_ttl_ts := tenant_ttl_ts;
                    ELSE
                        IF customer_ttl > 0 THEN
                            customer_ttl_ts :=
                                    (EXTRACT(EPOCH FROM current_timestamp) * 1000 -
                                     customer_ttl::bigint * 1000)::bigint;
                        END IF;
                    END IF;
                    IF customer_ttl_ts IS NOT NULL AND customer_ttl_ts > 0 THEN
                        deleted_for_entities :=
                                delete_customer_records_from_ts_kv(tenant_id_record, customer_id_record,
                                                                   customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for customer with id = % where tenant_id = %', deleted_for_entities, customer_id_record, tenant_id_record;
                        deleted_for_entities :=
                                delete_device_records_from_ts_kv(tenant_id_record, customer_id_record,
                                                                 customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for devices where tenant_id = % and customer_id = %', deleted_for_entities, tenant_id_record, customer_id_record;
                        deleted_for_entities := delete_asset_records_from_ts_kv(tenant_id_record,
                                                                                customer_id_record,
                                                                                customer_ttl_ts);
                        deleted := deleted + deleted_for_entities;
                        RAISE NOTICE '% telemetry removed for assets where tenant_id = % and customer_id = %', deleted_for_entities, tenant_id_record, customer_id_record;
                    END IF;
                END LOOP;
            FETCH tenant_cursor INTO tenant_id_record;
        END LOOP;
END
$$;

-- edge-only START
-- The two timeseries TTL routines below are duplicated from schema-ts-psql.sql. On the Cloud they live only
-- in schema-ts-psql.sql; on Edge they are needed here as well, because schema-ts-psql.sql is executed only on
-- a fresh install while this file is re-executed on every offline upgrade via ThingsboardInstallService ->
-- createOrUpdateViewsAndFunctions(), with no version or LTS-family filtering. That is what carries a corrected
-- body to already-installed databases, including across families such as 4.2.x -> 4.3.x.
-- Any change here MUST be mirrored in schema-ts-psql.sql - TimeseriesTtlRoutinesDuplicationTest fails otherwise.

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
-- edge-only END
