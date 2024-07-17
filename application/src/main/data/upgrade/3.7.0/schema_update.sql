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

-- KV VERSIONING UPDATE START

CREATE SEQUENCE IF NOT EXISTS attribute_kv_version_seq cache 1000;
CREATE SEQUENCE IF NOT EXISTS ts_kv_latest_version_seq cache 1000;

ALTER TABLE attribute_kv ADD COLUMN version bigint default 0;
ALTER TABLE ts_kv_latest ADD COLUMN version bigint default 0;

-- KV VERSIONING UPDATE END


-- Optimistic locking update START

ALTER TABLE device ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE device_profile ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE device_credentials ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE asset_profile ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE entity_view ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE edge ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE rule_chain ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE dashboard ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE widget_type ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE widgets_bundle ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE entity_group ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE converter ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE integration ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE role ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;

/*
TODO:
  custom translation
  white-labeling
*/
-- Optimistic locking update END
