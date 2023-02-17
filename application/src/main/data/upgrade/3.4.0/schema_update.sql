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

CREATE TABLE IF NOT EXISTS rule_node_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL ,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar,
    e_type varchar,
    e_entity_id uuid,
    e_entity_type varchar,
    e_msg_id uuid,
    e_msg_type varchar,
    e_data_type varchar,
    e_relation_type varchar,
    e_data varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS rule_chain_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_message varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS stats_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_messages_processed bigint NOT NULL,
    e_errors_occurred bigint NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS lc_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar NOT NULL,
    e_success boolean NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS error_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_method varchar NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS converter_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar,
    e_in_message_type varchar,
    e_in_message varchar,
    e_out_message_type varchar,
    e_out_message varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS integration_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar,
    e_message_type varchar,
    e_message varchar,
    e_status varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS raw_data_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_uuid varchar,
    e_message_type varchar,
    e_message varchar
) PARTITION BY RANGE (ts);

CREATE INDEX IF NOT EXISTS idx_rule_node_debug_event_main
    ON rule_node_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_rule_chain_debug_event_main
    ON rule_chain_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_stats_event_main
    ON stats_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_lc_event_main
    ON lc_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_error_event_main
    ON error_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_converter_debug_event_main
    ON converter_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_integration_debug_event_main
    ON integration_debug_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE INDEX IF NOT EXISTS idx_raw_data_event_main
    ON raw_data_event (tenant_id ASC, entity_id ASC, ts DESC NULLS LAST) WITH (FILLFACTOR=95);

CREATE OR REPLACE FUNCTION to_safe_json(p_json text) RETURNS json
LANGUAGE plpgsql AS
$$
BEGIN
  return REPLACE(p_json, '\u0000', '' )::json;
EXCEPTION
  WHEN OTHERS THEN
  return '{}'::json;
END;
$$;

-- Useful to migrate old events to the new table structure;
CREATE OR REPLACE PROCEDURE migrate_regular_events(IN start_ts_in_ms bigint, IN end_ts_in_ms bigint, IN partition_size_in_hours int)
    LANGUAGE plpgsql AS
$$
DECLARE
    partition_size_in_ms bigint;
    p record;
    table_name varchar;
BEGIN
    partition_size_in_ms = partition_size_in_hours * 3600 * 1000;

    FOR p IN SELECT DISTINCT event_type as event_type, (created_time - created_time % partition_size_in_ms) as partition_ts
    FROM event e WHERE e.event_type in ('STATS', 'LC_EVENT', 'ERROR', 'RAW_DATA') and ts >= start_ts_in_ms and ts < end_ts_in_ms
    LOOP
        IF p.event_type = 'STATS' THEN
            table_name := 'stats_event';
        ELSEIF p.event_type = 'LC_EVENT' THEN
            table_name := 'lc_event';
        ELSEIF p.event_type = 'ERROR' THEN
            table_name := 'error_event';
        ELSEIF p.event_type = 'RAW_DATA' THEN
            table_name := 'raw_data_event';
        END IF;
        RAISE NOTICE '[%] Partition to create : [%-%]', table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms);
        EXECUTE format('CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM ( %s ) TO ( %s )', table_name, p.partition_ts, table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms));
    END LOOP;

    INSERT INTO stats_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           (body ->> 'messagesProcessed')::bigint,
           (body ->> 'errorsOccurred')::bigint
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'STATS' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO lc_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'event',
           (body ->> 'success')::boolean,
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'LC_EVENT' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO error_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'method',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'ERROR' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO raw_data_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'uuid',
           body ->> 'messageType',
           body ->> 'message'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'RAW_DATA' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

END
$$;

-- Useful to migrate old debug events to the new table structure;
CREATE OR REPLACE PROCEDURE migrate_debug_events(IN start_ts_in_ms bigint, IN end_ts_in_ms bigint, IN partition_size_in_hours int)
    LANGUAGE plpgsql AS
$$
DECLARE
    partition_size_in_ms bigint;
    p record;
    table_name varchar;
BEGIN
    partition_size_in_ms = partition_size_in_hours * 3600 * 1000;

    FOR p IN SELECT DISTINCT event_type as event_type, (created_time - created_time % partition_size_in_ms) as partition_ts
    FROM event e WHERE e.event_type in ('DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION') and ts >= start_ts_in_ms and ts < end_ts_in_ms
    LOOP
        IF p.event_type = 'DEBUG_RULE_NODE' THEN
            table_name := 'rule_node_debug_event';
        ELSEIF p.event_type = 'DEBUG_RULE_CHAIN' THEN
            table_name := 'rule_chain_debug_event';
        ELSEIF p.event_type = 'DEBUG_CONVERTER' THEN
            table_name := 'converter_debug_event';
        ELSEIF p.event_type = 'DEBUG_INTEGRATION' THEN
            table_name := 'integration_debug_event';
        END IF;
        RAISE NOTICE '[%] Partition to create : [%-%]', table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms);
        EXECUTE format('CREATE TABLE IF NOT EXISTS %s_%s PARTITION OF %s FOR VALUES FROM ( %s ) TO ( %s )', table_name, p.partition_ts, table_name, p.partition_ts, (p.partition_ts + partition_size_in_ms));
    END LOOP;

    INSERT INTO rule_node_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'type',
           (body ->> 'entityId')::uuid,
           body ->> 'entityName',
           (body ->> 'msgId')::uuid,
           body ->> 'msgType',
           body ->> 'dataType',
           body ->> 'relationType',
           body ->> 'data',
           body ->> 'metadata',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'DEBUG_RULE_NODE' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO rule_chain_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'message',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'DEBUG_RULE_CHAIN' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO converter_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'type',
           body ->> 'inMessageType',
           body ->> 'in',
           body ->> 'outMessageType',
           body ->> 'out',
           body ->> 'metadata',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'DEBUG_CONVERTER' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;

    INSERT INTO integration_debug_event
    SELECT id,
           tenant_id,
           ts,
           entity_id,
           body ->> 'server',
           body ->> 'messageType',
           body ->> 'message',
           body ->> 'status',
           body ->> 'error'
    FROM
    (select id, tenant_id, ts, entity_id, to_safe_json(body) as body
     FROM event WHERE ts >= start_ts_in_ms AND ts < end_ts_in_ms AND event_type = 'DEBUG_INTEGRATION' AND to_safe_json(body) ->> 'server' IS NOT NULL
    ) safe_event
    ON CONFLICT DO NOTHING;
END
$$;

UPDATE tb_user
    SET additional_info = REPLACE(additional_info, '"lang":"ja_JA"', '"lang":"ja_JP"')
    WHERE additional_info LIKE '%"lang":"ja_JA"%';

UPDATE admin_settings
    SET json_value = REPLACE(json_value, '"ja_JA"', '"ja_JP"')
    WHERE key='customTranslation' AND json_value LIKE '%"ja_JA"%';

UPDATE attribute_kv
    SET str_v = REPLACE(str_v, '"ja_JA"', '"ja_JP"')
    WHERE entity_type='TENANT' AND entity_id in (select id from tenant) AND
        attribute_type='SERVER_SCOPE' AND attribute_key='customTranslation' AND str_v LIKE '%"ja_JA"%';

UPDATE attribute_kv
    SET str_v = REPLACE(str_v, '"ja_JA"', '"ja_JP"')
    WHERE entity_type='CUSTOMER' AND entity_id in (select id from customer) AND
        attribute_type='SERVER_SCOPE' AND attribute_key='customTranslation' AND str_v LIKE '%"ja_JA"%';
