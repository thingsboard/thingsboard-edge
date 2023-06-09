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

CREATE TABLE IF NOT EXISTS entity_alarm (
    tenant_id uuid NOT NULL,
    entity_type varchar(32),
    entity_id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_type varchar(255) NOT NULL,
    customer_id uuid,
    alarm_id uuid,
    CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id),
    CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_status_created_time ON alarm(tenant_id, status, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_entity_alarm_created_time ON entity_alarm(tenant_id, entity_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_entity_alarm_alarm_id ON entity_alarm(alarm_id);

INSERT INTO entity_alarm(tenant_id, entity_type, entity_id, created_time, alarm_type, customer_id, alarm_id)
SELECT tenant_id,
       CASE
           WHEN originator_type = 0 THEN 'TENANT'
           WHEN originator_type = 1 THEN 'CUSTOMER'
           WHEN originator_type = 2 THEN 'USER'
           WHEN originator_type = 3 THEN 'DASHBOARD'
           WHEN originator_type = 4 THEN 'ASSET'
           WHEN originator_type = 5 THEN 'DEVICE'
           WHEN originator_type = 6 THEN 'ALARM'
           WHEN originator_type = 7 THEN 'ENTITY_GROUP'
           WHEN originator_type = 8 THEN 'CONVERTER'
           WHEN originator_type = 9 THEN 'INTEGRATION'
           WHEN originator_type = 10 THEN 'RULE_CHAIN'
           WHEN originator_type = 11 THEN 'RULE_NODE'
           WHEN originator_type = 12 THEN 'SCHEDULER_EVENT'
           WHEN originator_type = 13 THEN 'BLOB_ENTITY'
           WHEN originator_type = 14 THEN 'ENTITY_VIEW'
           WHEN originator_type = 15 THEN 'WIDGETS_BUNDLE'
           WHEN originator_type = 16 THEN 'WIDGET_TYPE'
           WHEN originator_type = 17 THEN 'ROLE'
           WHEN originator_type = 18 THEN 'GROUP_PERMISSION'
           WHEN originator_type = 19 THEN 'TENANT_PROFILE'
           WHEN originator_type = 20 THEN 'DEVICE_PROFILE'
           WHEN originator_type = 21 THEN 'API_USAGE_STATE'
           WHEN originator_type = 22 THEN 'TB_RESOURCE'
           WHEN originator_type = 23 THEN 'OTA_PACKAGE'
           WHEN originator_type = 24 THEN 'EDGE'
           WHEN originator_type = 25 THEN 'RPC'
           else 'UNKNOWN'
           END,
       originator_id,
       created_time,
       type,
       customer_id,
       id
FROM alarm
ON CONFLICT DO NOTHING;

INSERT INTO entity_alarm(tenant_id, entity_type, entity_id, created_time, alarm_type, customer_id, alarm_id)
SELECT a.tenant_id, r.from_type, r.from_id, created_time, type, customer_id, id
FROM alarm a
         INNER JOIN relation r ON r.relation_type_group = 'ALARM' and r.relation_type = 'ANY' and a.id = r.to_id
ON CONFLICT DO NOTHING;

DELETE FROM relation r WHERE r.relation_type_group = 'ALARM';