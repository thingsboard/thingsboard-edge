--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

-- This file describes PostgreSQL-specific indexes that not supported by hsql
-- It is not a stand-alone file! Run schema-entities-idx.sql before!
-- Note: Hibernate DESC order translates to native SQL "ORDER BY .. DESC NULLS LAST"
--       While creating index PostgreSQL transforms short notation (ts DESC) to the full (DESC NULLS FIRST)
--       That difference between NULLS LAST and NULLS FIRST prevents to hit index while querying latest by ts
--       That why we need to define DESC index explicitly as (ts DESC NULLS LAST)

CREATE INDEX IF NOT EXISTS idx_event_ts
    ON public.event
    (ts DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_ts
    IS 'This index helps to delete events by TTL using timestamp';

CREATE INDEX IF NOT EXISTS idx_event_tenant_entity_type_entity_event_type_created_time_des
    ON public.event
    (tenant_id ASC, entity_type ASC, entity_id ASC, event_type ASC, created_time DESC NULLS LAST)
    WITH (FILLFACTOR=95);

COMMENT ON INDEX public.idx_event_tenant_entity_type_entity_event_type_created_time_des
    IS 'This index helps to open latest events on UI fast';
