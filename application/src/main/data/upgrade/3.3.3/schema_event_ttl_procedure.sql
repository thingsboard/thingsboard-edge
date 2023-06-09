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


DROP PROCEDURE IF EXISTS public.cleanup_events_by_ttl(bigint, bigint, bigint);

CREATE OR REPLACE PROCEDURE cleanup_events_by_ttl(
    IN regular_events_start_ts bigint,
    IN regular_events_end_ts bigint,
    IN debug_events_start_ts bigint,
    IN debug_events_end_ts bigint,
    INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    ttl_deleted_count bigint DEFAULT 0;
    debug_ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF regular_events_start_ts > 0 AND regular_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type != %L::varchar AND event_type != %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', regular_events_start_ts, regular_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN') into ttl_deleted_count;
    END IF;
    IF debug_events_start_ts > 0 AND debug_events_end_ts > 0 THEN
        EXECUTE format(
                'WITH deleted AS (DELETE FROM event WHERE id in (SELECT id from event WHERE ts > %L::bigint AND ts < %L::bigint AND ' ||
                '(event_type = %L::varchar OR event_type = %L::varchar)) RETURNING *) ' ||
                'SELECT count(*) FROM deleted', debug_events_start_ts, debug_events_end_ts,
                'DEBUG_RULE_NODE', 'DEBUG_RULE_CHAIN') into debug_ttl_deleted_count;
    END IF;
    RAISE NOTICE 'Events removed by ttl: %', ttl_deleted_count;
    RAISE NOTICE 'Debug Events removed by ttl: %', debug_ttl_deleted_count;
    deleted := ttl_deleted_count + debug_ttl_deleted_count;
END
$$;
