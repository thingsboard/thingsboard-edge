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

CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_originator_created_time ON alarm(originator_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time ON alarm(tenant_id, created_time DESC);

-- Drop index by 'status' column and replace with new one that has only active alarms;
CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type_active
    ON alarm USING btree (originator_id, type) WHERE cleared = false;

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_created_time ON alarm(tenant_id, type, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_assignee_created_time ON alarm(tenant_id, assignee_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_created_time ON entity_alarm(tenant_id, entity_id, created_time DESC);

-- Cover index by alarm type to optimize propagated alarm queries;
CREATE INDEX IF NOT EXISTS idx_entity_alarm_entity_id_alarm_type_created_time_alarm_id ON entity_alarm
USING btree (tenant_id, entity_id, alarm_type, created_time DESC) INCLUDE(alarm_id);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_alarm_id ON entity_alarm(alarm_id);

CREATE INDEX IF NOT EXISTS idx_relation_to_id ON relation(relation_type_group, to_type, to_id);

CREATE INDEX IF NOT EXISTS idx_relation_from_id ON relation(relation_type_group, from_type, from_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id ON device(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id_and_type ON device(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_device_type ON device(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_device_device_profile_id ON device(tenant_id, device_profile_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id ON asset(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id_and_type ON asset(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_type ON asset(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time ON audit_log(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_id ON audit_log(id);

CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_and_created_time ON edge_event(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_edge_event_id ON edge_event(id);

CREATE INDEX IF NOT EXISTS idx_entity_group_by_type_name_and_owner_id ON entity_group(type, name, owner_id);

CREATE INDEX IF NOT EXISTS idx_rpc_tenant_id_device_id ON rpc(tenant_id, device_id);

CREATE INDEX IF NOT EXISTS idx_customer_tenant_id_parent_customer_id ON customer(tenant_id, parent_customer_id);

CREATE INDEX IF NOT EXISTS idx_device_external_id ON device(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_device_profile_external_id ON device_profile(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_asset_external_id ON asset(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_entity_view_external_id ON entity_view(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_chain_external_id ON rule_chain(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_type ON rule_node(type);

CREATE INDEX IF NOT EXISTS idx_dashboard_external_id ON dashboard(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_customer_external_id ON customer(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_widgets_bundle_external_id ON widgets_bundle(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_converter_external_id ON converter(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_integration_external_id ON integration(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_role_external_id ON role(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_entity_group_external_id ON entity_group(external_id);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);

CREATE INDEX IF NOT EXISTS idx_scheduler_event_originator_id ON scheduler_event(tenant_id, originator_id);

CREATE INDEX IF NOT EXISTS idx_blob_entity_created_time ON blob_entity(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_blob_entity_id ON blob_entity(id);

CREATE INDEX IF NOT EXISTS idx_alarm_comment_alarm_id ON alarm_comment(alarm_id);

CREATE INDEX IF NOT EXISTS idx_notification_target_tenant_id_created_time ON notification_target(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_template_tenant_id_created_time ON notification_template(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_rule_tenant_id_created_time ON notification_rule(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id_originator_type_created_time ON notification_request(tenant_id, originator_entity_type, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_notification_request_rule_id_originator_entity_id ON notification_request(rule_id, originator_entity_id);

CREATE INDEX IF NOT EXISTS idx_notification_request_status ON notification_request(status);

CREATE INDEX IF NOT EXISTS idx_notification_id_recipient_id ON notification(id, recipient_id);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_status_created_time ON notification(recipient_id, status, created_time DESC);
