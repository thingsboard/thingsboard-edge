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

CREATE TABLE IF NOT EXISTS thingsboard.entity_group (
    id timeuuid,
    type text,
    name text,
    additional_info text,
    configuration text,
    PRIMARY KEY (id, type)
);

CREATE TABLE IF NOT EXISTS thingsboard.integration (
    id timeuuid,
    tenant_id timeuuid,
    name text,
    converter_id timeuuid,
    downlink_converter_id timeuuid,
    type text,
    routing_key text,
    search_text text,
    configuration text,
    debug_mode boolean,
    additional_info text,
    PRIMARY KEY (id, tenant_id)
);

CREATE TABLE IF NOT EXISTS thingsboard.converter (
    id timeuuid,
    tenant_id timeuuid,
    name text,
    type text,
    search_text text,
    configuration text,
    debug_mode boolean,
    additional_info text,
    PRIMARY KEY (id, tenant_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.integration_by_routing_key AS
    SELECT *
    from thingsboard.integration
    WHERE tenant_id IS NOT NULL AND id IS NOT NULL AND routing_key IS NOT NULL
    PRIMARY KEY (routing_key, id, tenant_id) WITH CLUSTERING ORDER BY (id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.integration_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.integration
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id )
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.integration_by_converter_id AS
    SELECT *
    from thingsboard.integration
    WHERE tenant_id IS NOT NULL AND id IS NOT NULL AND converter_id IS NOT NULL
    PRIMARY KEY (converter_id, id, tenant_id) WITH CLUSTERING ORDER BY (id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.converter_by_tenant_and_name AS
    SELECT *
    from thingsboard.converter
    WHERE tenant_id IS NOT NULL AND name IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, name, id)
    WITH CLUSTERING ORDER BY ( name ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.converter_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.converter
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id )
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC);
