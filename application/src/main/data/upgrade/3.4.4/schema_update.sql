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

CREATE TABLE IF NOT EXISTS alarm_comment (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_id uuid NOT NULL,
    user_id uuid,
    type varchar(255) NOT NULL,
    comment varchar(10000),
    CONSTRAINT fk_alarm_comment_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_alarm_comment_alarm_id ON alarm_comment(alarm_id);

CREATE TABLE IF NOT EXISTS notification_target (
    id UUID NOT NULL CONSTRAINT notification_target_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NULL CONSTRAINT fk_notification_target_tenant_id REFERENCES tenant(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    configuration VARCHAR(10000) NOT NULL,
    CONSTRAINT uq_notification_target_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_target_tenant_id_created_time ON notification_target(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_template (
    id UUID NOT NULL CONSTRAINT notification_template_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NULL CONSTRAINT fk_notification_template_tenant_id REFERENCES tenant(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    configuration VARCHAR(10000) NOT NULL,
    CONSTRAINT uq_notification_template_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_template_tenant_id_created_time ON notification_template(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_rule (
    id UUID NOT NULL CONSTRAINT notification_rule_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NULL CONSTRAINT fk_notification_rule_tenant_id REFERENCES tenant(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    template_id UUID NOT NULL CONSTRAINT fk_notification_rule_template_id REFERENCES notification_template(id),
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config VARCHAR(1000) NOT NULL,
    recipients_config VARCHAR(10000) NOT NULL,
    additional_config VARCHAR(255),
    CONSTRAINT uq_notification_rule_name UNIQUE (tenant_id, name)
);
CREATE INDEX IF NOT EXISTS idx_notification_rule_tenant_id_created_time ON notification_rule(tenant_id, created_time DESC);

CREATE TABLE IF NOT EXISTS notification_request (
    id UUID NOT NULL CONSTRAINT notification_request_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NULL CONSTRAINT fk_notification_request_tenant_id REFERENCES tenant(id) ON DELETE CASCADE,
    targets VARCHAR(10000) NOT NULL,
    template_id UUID,
    template VARCHAR(10000),
    info VARCHAR(1000),
    additional_config VARCHAR(1000),
    originator_entity_id UUID,
    originator_entity_type VARCHAR(32),
    rule_id UUID NULL,
    status VARCHAR(32),
    stats VARCHAR(10000)
);
CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id_originator_type_created_time ON notification_request(tenant_id, originator_entity_type, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_notification_request_rule_id_originator_entity_id ON notification_request(rule_id, originator_entity_id);
CREATE INDEX IF NOT EXISTS idx_notification_request_status ON notification_request(status);

CREATE TABLE IF NOT EXISTS notification (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    request_id UUID NULL CONSTRAINT fk_notification_request_id REFERENCES notification_request(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL CONSTRAINT fk_notification_recipient_id REFERENCES tb_user(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    text VARCHAR(1000) NOT NULL,
    additional_config VARCHAR(1000),
    info VARCHAR(1000),
    status VARCHAR(32)
) PARTITION BY RANGE (created_time);
CREATE INDEX IF NOT EXISTS idx_notification_id_recipient_id ON notification(id, recipient_id);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_status_created_time ON notification(recipient_id, status, created_time DESC);

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS phone VARCHAR(255);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL CONSTRAINT user_settings_pkey PRIMARY KEY,
    settings varchar(100000),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);
