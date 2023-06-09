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

-- PROCEDURE: public.cleanup_events_by_ttl(bigint, bigint, bigint)

DROP PROCEDURE IF EXISTS public.cleanup_events_by_ttl(bigint, bigint, bigint);

CREATE OR REPLACE PROCEDURE public.cleanup_events_by_ttl(
	ttl bigint,
	debug_ttl bigint,
	INOUT deleted bigint)
LANGUAGE 'plpgsql'
AS $BODY$
DECLARE
    ttl_ts bigint;
    debug_ttl_ts bigint;
    ttl_deleted_count bigint DEFAULT 0;
    debug_ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF ttl > 0 THEN
        ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - ttl::bigint * 1000)::bigint;

        DELETE FROM event
		  WHERE ts < ttl_ts
		    AND NOT event_type IN ('DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION');

		GET DIAGNOSTICS ttl_deleted_count = ROW_COUNT;
    END IF;

    IF debug_ttl > 0 THEN
        debug_ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - debug_ttl::bigint * 1000)::bigint;

		DELETE FROM event
		  WHERE ts < debug_ttl_ts
		    AND     event_type IN ('DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN', 'DEBUG_CONVERTER', 'DEBUG_INTEGRATION');

		GET DIAGNOSTICS debug_ttl_deleted_count = ROW_COUNT;
    END IF;

    RAISE NOTICE 'Events removed by ttl: %', ttl_deleted_count;
    RAISE NOTICE 'Debug Events removed by ttl: %', debug_ttl_deleted_count;
    deleted := ttl_deleted_count + debug_ttl_deleted_count;
END
$BODY$;


-- Index: idx_event_ts

DROP INDEX IF EXISTS public.idx_event_ts;

-- Hint: add CONCURRENTLY to CREATE INDEX query in case of more then 1 million records or during live update
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_event_ts
CREATE INDEX IF NOT EXISTS idx_event_ts
    ON public.event
    (ts DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_ts
    IS 'This index helps to delete events by TTL using timestamp';


-- Index: idx_event_tenant_entity_type_entity_event_type_created_time_des

DROP INDEX IF EXISTS public.idx_event_tenant_entity_type_entity_event_type_created_time_des;

-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_event_tenant_entity_type_entity_event_type_created_time_des
CREATE INDEX IF NOT EXISTS idx_event_tenant_entity_type_entity_event_type_created_time_des
    ON public.event
    (tenant_id ASC, entity_type ASC, entity_id ASC, event_type ASC, created_time DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_tenant_entity_type_entity_event_type_created_time_des
    IS 'This index helps to open latest events on UI fast';

-- Index: idx_event_type_entity_id
-- Description: replaced with more suitable idx_event_tenant_entity_type_entity_event_type_created_time_des
DROP INDEX IF EXISTS public.idx_event_type_entity_id;