--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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


CREATE TABLE IF NOT EXISTS  thingsboard.scheduler_event (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    search_text text,
    schedule text,
    configuration text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.scheduler_event_by_tenant AS
    SELECT *
    from thingsboard.scheduler_event
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.scheduler_event_by_tenant_and_type AS
    SELECT *
    from thingsboard.scheduler_event
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, type, id, customer_id)
    WITH CLUSTERING ORDER BY ( type ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.scheduler_event_by_customer AS
    SELECT *
    from thingsboard.scheduler_event
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.scheduler_event_by_customer_and_type AS
    SELECT *
    from thingsboard.scheduler_event
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, type, id )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, type ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.blob_entity (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    content_type text,
    search_text text,
    data blob,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.blob_entity_by_tenant AS
    SELECT *
    from thingsboard.blob_entity
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, id, type, customer_id)
    WITH CLUSTERING ORDER BY (id ASC, type ASC, customer_id ASC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.blob_entity_by_tenant_and_type AS
    SELECT *
    from thingsboard.blob_entity
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, type, id, customer_id)
    WITH CLUSTERING ORDER BY ( type ASC, id ASC, customer_id ASC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.blob_entity_by_customer AS
    SELECT *
    from thingsboard.blob_entity
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( (tenant_id, customer_id), id, type )
    WITH CLUSTERING ORDER BY ( id ASC, type ASC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.blob_entity_by_customer_and_type AS
    SELECT *
    from thingsboard.blob_entity
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( (tenant_id, customer_id), type, id )
    WITH CLUSTERING ORDER BY ( type ASC, id ASC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_by_tenant_authority_and_search_text AS
	SELECT *
	from thingsboard.user
	WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND authority IS NOT NULL AND search_text IS NOT NULL AND id
	IS NOT NULL
	PRIMARY KEY ( tenant_id, authority, search_text, id, customer_id )
	WITH CLUSTERING ORDER BY ( authority DESC, search_text ASC, id DESC);

CREATE TABLE IF NOT EXISTS thingsboard.role (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    permissions text,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_and_customer_and_name AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND name IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, name, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, name ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_and_customer_and_search_text AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, search_text, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.role_by_tenant_customer_type_and_search_text AS
    SELECT *
    from thingsboard.role
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, type, search_text, id )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, type ASC, search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.group_permission (
    id timeuuid,
    tenant_id timeuuid,
    role_id timeuuid,
    user_group_id timeuuid,
    entity_group_id timeuuid,
    entity_group_type text,
    is_public boolean,
    PRIMARY KEY (id, tenant_id, user_group_id, entity_group_id, role_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_tenant_id AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND id IS NOT NULL AND user_group_id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY (tenant_id, id, user_group_id, entity_group_id, role_id)
    WITH CLUSTERING ORDER BY (id DESC, user_group_id DESC, entity_group_id DESC, role_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_user_group_id AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND user_group_id IS NOT NULL AND id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY ((tenant_id, user_group_id), id, entity_group_id, role_id)
    WITH CLUSTERING ORDER BY (id DESC, entity_group_id DESC, role_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_user_group_id_and_role_id AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND user_group_id IS NOT NULL AND id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY ((tenant_id, user_group_id, role_id), id, entity_group_id)
    WITH CLUSTERING ORDER BY (id DESC, entity_group_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_group_user_group_and_role AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND user_group_id IS NOT NULL AND id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY ((tenant_id, entity_group_id, user_group_id, role_id), id)
    WITH CLUSTERING ORDER BY (id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_entity_group_id AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND user_group_id IS NOT NULL AND id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY ((tenant_id, entity_group_id), id, user_group_id, role_id)
    WITH CLUSTERING ORDER BY (id DESC, user_group_id DESC, role_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.group_permission_by_role_id AS
    SELECT *
    from thingsboard.group_permission
    WHERE tenant_id IS NOT NULL AND user_group_id IS NOT NULL AND id IS NOT NULL AND entity_group_id IS NOT NULL AND role_id IS NOT NULL
    PRIMARY KEY ((tenant_id, role_id), id, user_group_id, entity_group_id)
    WITH CLUSTERING ORDER BY (id DESC, user_group_id DESC, entity_group_id DESC);
