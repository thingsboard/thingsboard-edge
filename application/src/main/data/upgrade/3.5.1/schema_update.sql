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

ALTER TABLE notification_rule ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- NOTIFICATION CONFIGS VERSION CONTROL START

ALTER TABLE notification_template
    ADD COLUMN IF NOT EXISTS external_id UUID;
DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'uq_notification_template_external_id') THEN
            ALTER TABLE notification_template ADD CONSTRAINT uq_notification_template_external_id UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

ALTER TABLE notification_target
    ADD COLUMN IF NOT EXISTS external_id UUID;
DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'uq_notification_target_external_id') THEN
            ALTER TABLE notification_target ADD CONSTRAINT uq_notification_target_external_id UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

ALTER TABLE notification_rule
    ADD COLUMN IF NOT EXISTS external_id UUID;
DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'uq_notification_rule_external_id') THEN
            ALTER TABLE notification_rule ADD CONSTRAINT uq_notification_rule_external_id UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

-- NOTIFICATION CONFIGS VERSION CONTROL END

-- EDGE EVENTS MIGRATION START
DO
$$
    DECLARE table_partition RECORD;
    BEGIN
        -- in case of running the upgrade script a second time:
        IF NOT (SELECT exists(SELECT FROM pg_tables WHERE tablename = 'old_edge_event')) THEN
            ALTER TABLE edge_event RENAME TO old_edge_event;
            CREATE INDEX IF NOT EXISTS idx_old_edge_event_created_time_tmp ON old_edge_event(created_time);
            ALTER INDEX IF EXISTS idx_edge_event_tenant_id_and_created_time RENAME TO idx_old_edge_event_tenant_id_and_created_time;

            FOR table_partition IN SELECT tablename AS name, split_part(tablename, '_', 3) AS partition_ts
                                   FROM pg_tables WHERE tablename LIKE 'edge_event_%'
                LOOP
                    EXECUTE format('ALTER TABLE %s RENAME TO old_edge_event_%s', table_partition.name, table_partition.partition_ts);
                END LOOP;
        ELSE
            RAISE NOTICE 'Table old_edge_event already exists, leaving as is';
        END IF;
    END;
$$;

CREATE TABLE IF NOT EXISTS edge_event (
    seq_id INT GENERATED ALWAYS AS IDENTITY,
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    edge_id uuid,
    edge_event_type varchar(255),
    edge_event_uid varchar(255),
    entity_id uuid,
    edge_event_action varchar(255),
    body varchar(10000000),
    tenant_id uuid,
    ts bigint NOT NULL
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_and_created_time ON edge_event(tenant_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_edge_event_id ON edge_event(id);
ALTER TABLE IF EXISTS edge_event ALTER COLUMN seq_id SET CYCLE;

CREATE OR REPLACE PROCEDURE migrate_edge_event(IN start_time_ms BIGINT, IN end_time_ms BIGINT, IN partition_size_ms BIGINT)
    LANGUAGE plpgsql AS
$$
DECLARE
    p RECORD;
    partition_end_ts BIGINT;
BEGIN
    FOR p IN SELECT DISTINCT (created_time - created_time % partition_size_ms) AS partition_ts FROM old_edge_event
             WHERE created_time >= start_time_ms AND created_time < end_time_ms
        LOOP
            partition_end_ts = p.partition_ts + partition_size_ms;
            RAISE NOTICE '[edge_event] Partition to create : [%-%]', p.partition_ts, partition_end_ts;
            EXECUTE format('CREATE TABLE IF NOT EXISTS edge_event_%s PARTITION OF edge_event ' ||
                           'FOR VALUES FROM ( %s ) TO ( %s )', p.partition_ts, p.partition_ts, partition_end_ts);
        END LOOP;

    INSERT INTO edge_event (id, created_time, edge_id, edge_event_type, edge_event_uid, entity_id, edge_event_action, body, tenant_id, ts)
    SELECT id, created_time, edge_id, edge_event_type, edge_event_uid, entity_id, edge_event_action, body, tenant_id, ts
    FROM old_edge_event
    WHERE created_time >= start_time_ms AND created_time < end_time_ms;
END;
$$;
-- EDGE EVENTS MIGRATION END

ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS etag varchar;

UPDATE resource
    SET etag = encode(sha256(decode(resource.data, 'base64')),'hex') WHERE resource.data is not null;

