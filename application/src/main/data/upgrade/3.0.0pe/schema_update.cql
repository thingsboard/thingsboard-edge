--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

CREATE TABLE IF NOT EXISTS thingsboard.role (
    id timeuuid,
    tenant_id timeuuid,
    name text,
    type text,
    permissions text,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_and_name AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL
      AND type IS NOT NULL
      AND name IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, name, id, type)
    WITH CLUSTERING ORDER BY (name ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, search_text, id, type)
    WITH CLUSTERING ORDER BY (search_text ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, type, search_text, id)
    WITH CLUSTERING ORDER BY (type ASC, search_text ASC, id DESC);
