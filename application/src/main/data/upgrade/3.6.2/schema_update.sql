--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

-- RULE NODE INDEXES UPDATE START

DROP INDEX IF EXISTS idx_rule_node_type;
DROP INDEX IF EXISTS idx_rule_node_type_configuration_version;
CREATE INDEX IF NOT EXISTS idx_rule_node_type_id_configuration_version ON rule_node(type, id, configuration_version);

-- RULE NODE INDEXES UPDATE END

-- RULE NODE QUEUE UPDATE START

ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS queue_name varchar(255);
ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS has_queue_name boolean DEFAULT false;

-- RULE NODE QUEUE UPDATE END

-- SCHEDULER EVENT UPDATE START

ALTER TABLE scheduler_event ADD COLUMN IF NOT EXISTS enabled boolean default true;

-- SCHEDULER EVENT UPDATE END

-- QUEUE STATS UPDATE START

CREATE TABLE IF NOT EXISTS queue_stats (
    id uuid NOT NULL CONSTRAINT queue_stats_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    queue_name varchar(255) NOT NULL,
    service_id varchar(255) NOT NULL,
    CONSTRAINT queue_stats_name_unq_key UNIQUE (tenant_id, queue_name, service_id));

INSERT INTO queue_stats
    SELECT id, created_time, tenant_id, substring(name FROM 1 FOR position('_' IN name) - 1) AS queue_name,
           substring(name FROM position('_' IN name) + 1) AS service_id
    FROM asset
    WHERE type = 'TbServiceQueue' and name LIKE '%\_%';

DELETE FROM asset WHERE type='TbServiceQueue';
DELETE FROM asset_profile WHERE name ='TbServiceQueue';

-- QUEUE STATS UPDATE END