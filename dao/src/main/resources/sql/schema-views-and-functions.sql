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

CREATE OR REPLACE VIEW integration_info as
SELECT created_time, id, tenant_id, name, type, debug_mode, enabled, is_remote,
       allow_create_devices_or_assets, is_edge_template,
       (SELECT cast(json_agg(element) as varchar)
        FROM (SELECT sum(se.e_messages_processed + se.e_errors_occurred) element
              FROM stats_event se
              WHERE se.tenant_id = i.tenant_id
                AND se.entity_id = i.id
                AND ts >= (EXTRACT(EPOCH FROM current_timestamp) * 1000 - 24 * 60 * 60 * 1000)::bigint

              GROUP BY ts / 3600000
              ORDER BY ts / 3600000) stats) as stats,
       (CASE WHEN i.enabled THEN
                 (SELECT cast(json_v as varchar)
                  FROM attribute_kv
                  WHERE entity_type = 'INTEGRATION'
                    AND entity_id = i.id
                    AND attribute_type = 'SERVER_SCOPE'
                    AND attribute_key LIKE 'integration_status_%'
                  ORDER BY last_update_ts
                 LIMIT 1) END) as status
FROM integration i;

CREATE OR REPLACE VIEW dashboard_info_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DASHBOARD'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM dashboard d
         LEFT JOIN customer c ON c.id = d.customer_id;

CREATE OR REPLACE VIEW asset_info_view as
SELECT a.*,
       c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = a.id
                             and re.to_type = 'ASSET'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM asset a
         LEFT JOIN customer c ON c.id = a.customer_id;

DROP VIEW IF EXISTS device_info_active_attribute_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_attribute_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DEVICE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups,
       COALESCE(da.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN attribute_kv da ON da.entity_type = 'DEVICE' AND da.entity_id = d.id AND da.attribute_type = 'SERVER_SCOPE' AND da.attribute_key = 'active';

DROP VIEW IF EXISTS device_info_active_ts_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_ts_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DEVICE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups,
       COALESCE(dt.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN ts_kv_latest dt ON dt.entity_id = d.id and dt.key = (select key_id from ts_kv_dictionary where key = 'active');

DROP VIEW IF EXISTS device_info_view CASCADE;
CREATE OR REPLACE VIEW device_info_view AS SELECT * FROM device_info_active_attribute_view;

CREATE OR REPLACE VIEW entity_view_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'ENTITY_VIEW'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM entity_view e
         LEFT JOIN customer c ON c.id = e.customer_id;

CREATE OR REPLACE VIEW customer_info_view as
SELECT c.*, c2.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = c.id
                             and re.to_type = 'CUSTOMER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM customer c
         LEFT JOIN customer c2 ON c2.id = c.parent_customer_id;

CREATE OR REPLACE VIEW user_info_view as
SELECT u.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = u.id
                             and re.to_type = 'USER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM tb_user u
         LEFT JOIN customer c ON c.id = u.customer_id;

CREATE OR REPLACE VIEW edge_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'EDGE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM edge e
         LEFT JOIN customer c ON c.id = e.customer_id;

CREATE OR REPLACE VIEW entity_group_info_view as
SELECT eg.*,
       array_to_json(ARRAY(WITH RECURSIVE owner_ids(id, type, lvl) AS
                                              (SELECT eg.owner_id id, eg.owner_type::varchar(15) as type, 1 as lvl
                                               UNION
                                               SELECT (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN ce2.tenant_id
                                                           ELSE ce2.parent_customer_id END) as id,
                                                      (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN 'TENANT'
                                                           ELSE 'CUSTOMER' END)::varchar(15) as type,
                                                      parent.lvl + 1 as lvl
                                               FROM customer ce2, owner_ids parent WHERE ce2.id = parent.id and eg.owner_type = 'CUSTOMER')
                           SELECT json_build_object('id', id, 'entityType', type) FROM owner_ids order by lvl)) owner_ids
FROM entity_group eg;

CREATE OR REPLACE VIEW owner_info_view as
(SELECT t.id as id, t.created_time as created_time, '13814000-1dd2-11b2-8080-808080808080'::uuid as tenant_id, 'TENANT' as entity_type, t.title as name, false as is_public from tenant t
UNION
SELECT c.id as id, c.created_time as created_time, c.tenant_id as tenant_id, 'CUSTOMER' as entity_type, c.title as name,
       (CASE
            WHEN c.additional_info is not null and c.additional_info::json ->> 'isPublic' = 'true' THEN true
            ELSE false END) as is_public
FROM customer c);

DROP VIEW IF EXISTS alarm_info CASCADE;
CREATE VIEW alarm_info AS
SELECT a.*,
       (CASE WHEN a.acknowledged AND a.cleared THEN 'CLEARED_ACK'
             WHEN NOT a.acknowledged AND a.cleared THEN 'CLEARED_UNACK'
             WHEN a.acknowledged AND NOT a.cleared THEN 'ACTIVE_ACK'
             WHEN NOT a.acknowledged AND NOT a.cleared THEN 'ACTIVE_UNACK' END) as status,
       COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
                     WHEN a.originator_type = 1 THEN (select title from customer where id = a.originator_id)
                     WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
                     WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
                     WHEN a.originator_type = 4 THEN (select name from asset where id = a.originator_id)
                     WHEN a.originator_type = 5 THEN (select name from device where id = a.originator_id)
                     WHEN a.originator_type = 8 THEN (select name from converter where id = a.originator_id)
                     WHEN a.originator_type = 9 THEN (select name from integration where id = a.originator_id)
                     WHEN a.originator_type = 14 THEN (select name from entity_view where id = a.originator_id)
                     WHEN a.originator_type = 20 THEN (select name from device_profile where id = a.originator_id)
                     WHEN a.originator_type = 21 THEN (select name from asset_profile where id = a.originator_id)
                     WHEN a.originator_type = 25 THEN (select name from edge where id = a.originator_id) END
           , 'Deleted') originator_name,
       COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
                     WHEN a.originator_type = 1 THEN (select COALESCE(NULLIF(title, ''), email) from customer where id = a.originator_id)
                     WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
                     WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
                     WHEN a.originator_type = 4 THEN (select COALESCE(NULLIF(label, ''), name) from asset where id = a.originator_id)
                     WHEN a.originator_type = 5 THEN (select COALESCE(NULLIF(label, ''), name) from device where id = a.originator_id)
                     WHEN a.originator_type = 8 THEN (select name from converter where id = a.originator_id)
                     WHEN a.originator_type = 9 THEN (select name from integration where id = a.originator_id)
                     WHEN a.originator_type = 14 THEN (select name from entity_view where id = a.originator_id)
                     WHEN a.originator_type = 20 THEN (select name from device_profile where id = a.originator_id)
                     WHEN a.originator_type = 21 THEN (select name from asset_profile where id = a.originator_id)
                     WHEN a.originator_type = 25 THEN (select COALESCE(NULLIF(label, ''), name) from edge where id = a.originator_id) END
           , 'Deleted') as originator_label,
       u.first_name as assignee_first_name, u.last_name as assignee_last_name, u.email as assignee_email
FROM alarm a
         LEFT JOIN tb_user u ON u.id = a.assignee_id;

CREATE OR REPLACE FUNCTION create_or_update_active_alarm(
                                        t_id uuid, c_id uuid, a_id uuid, a_created_ts bigint,
                                        a_o_id uuid, a_o_type integer, a_type varchar,
                                        a_severity varchar, a_start_ts bigint, a_end_ts bigint,
                                        a_details varchar,
                                        a_propagate boolean, a_propagate_to_owner boolean, a_propagate_to_owner_hierarchy boolean,
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
 propagate, propagate_to_owner, propagate_to_owner_hierarchy, propagate_to_tenant, propagate_relation_types,
 acknowledged, ack_ts,
 cleared, clear_ts,
 assignee_id, assign_ts)
VALUES
    (t_id, c_id, a_id, a_created_ts,
     a_o_id, a_o_type, a_type,
     a_severity, a_start_ts, a_end_ts,
     a_details,
     a_propagate, a_propagate_to_owner, a_propagate_to_owner_hierarchy, a_propagate_to_tenant, a_propagation_types,
     false, 0, false, 0, NULL, 0);
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
    propagate_to_owner_hierarchy       = a_propagate_to_owner_hierarchy,
    propagate_to_tenant      = a_propagate_to_tenant,
    propagate_relation_types = a_propagation_types
WHERE a.id = existing.id
  AND a.tenant_id = t_id
  AND (severity != a_severity OR start_ts != a_start_ts OR end_ts != a_end_ts OR additional_info != a_details
            OR propagate != a_propagate OR propagate_to_owner != a_propagate_to_owner OR propagate_to_owner_hierarchy != a_propagate_to_owner_hierarchy
            OR propagate_to_tenant != a_propagate_to_tenant OR propagate_relation_types != a_propagation_types);
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
                                        a_propagate boolean, a_propagate_to_owner boolean, a_propagate_to_owner_hierarchy boolean,
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
    propagate_to_owner_hierarchy = a_propagate_to_owner_hierarchy,
    propagate_to_tenant      = a_propagate_to_tenant,
    propagate_relation_types = a_propagation_types
WHERE a.id = a_id
  AND a.tenant_id = t_id
  AND (severity != a_severity OR start_ts != a_start_ts OR end_ts != a_end_ts OR additional_info != a_details
        OR propagate != a_propagate OR propagate_to_owner != a_propagate_to_owner OR propagate_to_owner_hierarchy != a_propagate_to_owner_hierarchy
        OR propagate_to_tenant != a_propagate_to_tenant OR propagate_relation_types != a_propagation_types);
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
