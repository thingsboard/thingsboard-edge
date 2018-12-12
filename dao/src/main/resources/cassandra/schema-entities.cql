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

CREATE KEYSPACE IF NOT EXISTS thingsboard
WITH replication = {
	'class' : 'SimpleStrategy',
	'replication_factor' : 1
};

CREATE TABLE IF NOT EXISTS thingsboard.user (
	id timeuuid,
	tenant_id timeuuid,
	customer_id timeuuid,
	email text,
	search_text text,
	authority text,
	first_name text,
	last_name text,
	additional_info text,
	PRIMARY KEY (id, tenant_id, customer_id, authority)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_by_email AS
	SELECT *
	from thingsboard.user
	WHERE email IS NOT NULL AND tenant_id IS NOT NULL AND customer_id IS NOT NULL AND id IS NOT NULL AND authority IS NOT
	NULL
	PRIMARY KEY ( email, tenant_id, customer_id, id, authority );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_by_tenant_and_search_text AS
	SELECT *
	from thingsboard.user
	WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND authority IS NOT NULL AND search_text IS NOT NULL AND id
	IS NOT NULL
	PRIMARY KEY ( tenant_id, customer_id, authority, search_text, id )
	WITH CLUSTERING ORDER BY ( customer_id DESC, authority DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_by_customer_and_search_text AS
	SELECT *
	from thingsboard.user
	WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND authority IS NOT NULL AND search_text IS NOT NULL AND id
	IS NOT NULL
	PRIMARY KEY ( customer_id, tenant_id, authority, search_text, id )
	WITH CLUSTERING ORDER BY ( tenant_id DESC, authority DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_by_tenant_authority_and_search_text AS
	SELECT *
	from thingsboard.user
	WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND authority IS NOT NULL AND search_text IS NOT NULL AND id
	IS NOT NULL
	PRIMARY KEY ( tenant_id, authority, search_text, id, customer_id )
	WITH CLUSTERING ORDER BY ( authority DESC, search_text ASC, id DESC);

CREATE TABLE IF NOT EXISTS thingsboard.user_credentials (
	id timeuuid PRIMARY KEY,
	user_id timeuuid,
	enabled boolean,
	password text,
	activate_token text,
	reset_token text
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_credentials_by_user AS
	SELECT *
	from thingsboard.user_credentials
	WHERE user_id IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( user_id, id );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_credentials_by_activate_token AS
	SELECT *
	from thingsboard.user_credentials
	WHERE activate_token IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( activate_token, id );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.user_credentials_by_reset_token AS
	SELECT *
	from thingsboard.user_credentials
	WHERE reset_token IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( reset_token, id );

CREATE TABLE IF NOT EXISTS thingsboard.admin_settings (
	id timeuuid PRIMARY KEY,
	key text,
	json_value text
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.admin_settings_by_key AS
	SELECT *
	from thingsboard.admin_settings
	WHERE key IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( key, id )
	WITH CLUSTERING ORDER BY ( id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.tenant (
	id timeuuid,
	title text,
	search_text text,
	region text,
	country text,
	state text,
	city text,
	address text,
	address2 text,
	zip text,
	phone text,
	email text,
	additional_info text,
	PRIMARY KEY (id, region)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.tenant_by_region_and_search_text AS
	SELECT *
	from thingsboard.tenant
	WHERE region IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( region, search_text, id )
	WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.customer (
	id timeuuid,
	tenant_id timeuuid,
	parent_customer_id timeuuid,
	title text,
	search_text text,
	country text,
	state text,
	city text,
	address text,
	address2 text,
	zip text,
	phone text,
	email text,
	additional_info text,
	PRIMARY KEY (id, tenant_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.customer_by_tenant_and_title AS
	SELECT *
	from thingsboard.customer
	WHERE tenant_id IS NOT NULL AND title IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( tenant_id, title, id )
	WITH CLUSTERING ORDER BY ( title ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.customer_by_tenant_and_search_text AS
	SELECT *
	from thingsboard.customer
	WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( tenant_id, search_text, id )
	WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.device (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_by_tenant_and_name AS
    SELECT *
    from thingsboard.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND name IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, name, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, type, search_text, id, customer_id)
    WITH CLUSTERING ORDER BY ( type ASC, search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_by_customer_and_search_text AS
    SELECT *
    from thingsboard.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, search_text, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_by_customer_by_type_and_search_text AS
    SELECT *
    from thingsboard.device
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, type, search_text, id )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, type ASC, search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.device_credentials (
	id timeuuid PRIMARY KEY,
	device_id timeuuid,
	credentials_type text,
	credentials_id text,
	credentials_value text
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_credentials_by_device AS
	SELECT *
	from thingsboard.device_credentials
	WHERE device_id IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( device_id, id );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.device_credentials_by_credentials_id AS
	SELECT *
	from thingsboard.device_credentials
	WHERE credentials_id IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( credentials_id, id );

CREATE TABLE IF NOT EXISTS thingsboard.asset (
    id timeuuid,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.asset_by_tenant_and_name AS
    SELECT *
    from thingsboard.asset
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND name IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, name, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.asset_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.asset
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id, customer_id, type)
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.asset_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.asset
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, type, search_text, id, customer_id)
    WITH CLUSTERING ORDER BY ( type ASC, search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.asset_by_customer_and_search_text AS
    SELECT *
    from thingsboard.asset
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, search_text, id, type )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.asset_by_customer_by_type_and_search_text AS
    SELECT *
    from thingsboard.asset
    WHERE tenant_id IS NOT NULL AND customer_id IS NOT NULL AND type IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( customer_id, tenant_id, type, search_text, id )
    WITH CLUSTERING ORDER BY ( tenant_id DESC, type ASC, search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.entity_subtype (
    tenant_id timeuuid,
    entity_type text, // (DEVICE, ASSET)
    type text,
    PRIMARY KEY (tenant_id, entity_type, type)
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

CREATE TABLE IF NOT EXISTS thingsboard.alarm (
	id timeuuid,
	tenant_id timeuuid,
	type text,
	originator_id timeuuid,
	originator_type text,
    severity text,
    status text,
	start_ts bigint,
	end_ts bigint,
	ack_ts bigint,
	clear_ts bigint,
	details text,
	propagate boolean,
	PRIMARY KEY ((tenant_id, originator_id, originator_type), type, id)
) WITH CLUSTERING ORDER BY ( type ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.alarm_by_id AS
    SELECT *
    from thingsboard.alarm
    WHERE tenant_id IS NOT NULL AND originator_id IS NOT NULL AND originator_type IS NOT NULL AND type IS NOT NULL
    AND type IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY (id, tenant_id, originator_id, originator_type, type)
    WITH CLUSTERING ORDER BY ( tenant_id ASC, originator_id ASC, originator_type ASC, type ASC);

CREATE TABLE IF NOT EXISTS thingsboard.relation (
	from_id timeuuid,
	from_type text,
	to_id timeuuid,
	to_type text,
	relation_type_group text,
	relation_type text,
	additional_info text,
	PRIMARY KEY ((from_id, from_type), relation_type_group, relation_type, to_id, to_type)
) WITH CLUSTERING ORDER BY ( relation_type_group ASC, relation_type ASC, to_id ASC, to_type ASC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.relation_by_type_and_child_type AS
    SELECT *
    from thingsboard.relation
    WHERE from_id IS NOT NULL AND from_type IS NOT NULL AND relation_type_group IS NOT NULL AND relation_type IS NOT NULL AND to_id IS NOT NULL AND to_type IS NOT NULL
    PRIMARY KEY ((from_id, from_type), relation_type_group, relation_type, to_type, to_id)
    WITH CLUSTERING ORDER BY ( relation_type_group ASC, relation_type ASC, to_type ASC, to_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.reverse_relation AS
    SELECT *
    from thingsboard.relation
    WHERE from_id IS NOT NULL AND from_type IS NOT NULL AND relation_type_group IS NOT NULL AND relation_type IS NOT NULL AND to_id IS NOT NULL AND to_type IS NOT NULL
    PRIMARY KEY ((to_id, to_type), relation_type_group, relation_type, from_id, from_type)
    WITH CLUSTERING ORDER BY ( relation_type_group ASC, relation_type ASC, from_id ASC, from_type ASC);

CREATE TABLE IF NOT EXISTS thingsboard.entity_group (
    id timeuuid,
    type text,
    name text,
    additional_info text,
    configuration text,
    PRIMARY KEY (id, type)
);

CREATE TABLE IF NOT EXISTS thingsboard.widgets_bundle (
    id timeuuid,
    tenant_id timeuuid,
    alias text,
    title text,
    search_text text,
    image blob,
    PRIMARY KEY (id, tenant_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.widgets_bundle_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.widgets_bundle
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id )
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.widgets_bundle_by_tenant_and_alias AS
    SELECT *
    from thingsboard.widgets_bundle
    WHERE tenant_id IS NOT NULL AND alias IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, alias, id )
    WITH CLUSTERING ORDER BY ( alias ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.widget_type (
    id timeuuid,
    tenant_id timeuuid,
    bundle_alias text,
    alias text,
    name text,
    descriptor text,
    PRIMARY KEY (id, tenant_id, bundle_alias)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.widget_type_by_tenant_and_aliases AS
    SELECT *
    from thingsboard.widget_type
    WHERE tenant_id IS NOT NULL AND bundle_alias IS NOT NULL AND alias IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, bundle_alias, alias, id )
    WITH CLUSTERING ORDER BY ( bundle_alias ASC, alias ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.dashboard (
	id timeuuid,
	tenant_id timeuuid,
	customer_id timeuuid,
	title text,
	search_text text,
	assigned_customers text,
	configuration text,
	PRIMARY KEY (id, tenant_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.dashboard_by_tenant_and_search_text AS
	SELECT *
	from thingsboard.dashboard
	WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
	PRIMARY KEY ( tenant_id, search_text, id )
	WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS thingsboard.attributes_kv_cf (
    entity_type text, // (DEVICE, CUSTOMER, TENANT)
    entity_id timeuuid,
    attribute_type text, // (CLIENT_SIDE, SHARED, SERVER_SIDE)
    attribute_key text,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    last_update_ts bigint,
    PRIMARY KEY ((entity_type, entity_id, attribute_type), attribute_key)
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };

CREATE TABLE IF NOT EXISTS  thingsboard.component_descriptor (
    id timeuuid,
    type text,
    scope text,
    name text,
    search_text text,
    clazz text,
    configuration_descriptor text,
    actions text,
    PRIMARY KEY (clazz, id, type, scope)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.component_desc_by_type_search_text AS
    SELECT *
    from thingsboard.component_descriptor
    WHERE type IS NOT NULL AND scope IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL AND clazz IS NOT NULL
    PRIMARY KEY ( type, search_text, id, clazz, scope)
    WITH CLUSTERING ORDER BY ( search_text DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.component_desc_by_scope_type_search_text AS
    SELECT *
    from thingsboard.component_descriptor
    WHERE type IS NOT NULL AND scope IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL AND clazz IS NOT NULL
    PRIMARY KEY ( (scope, type), search_text, id, clazz)
    WITH CLUSTERING ORDER BY ( search_text DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.component_desc_by_id AS
    SELECT *
    from thingsboard.component_descriptor
    WHERE type IS NOT NULL AND scope IS NOT NULL AND id IS NOT NULL AND clazz IS NOT NULL
    PRIMARY KEY ( id, clazz, scope, type )
    WITH CLUSTERING ORDER BY ( clazz ASC, scope ASC, type DESC);

CREATE TABLE IF NOT EXISTS thingsboard.event (
	tenant_id timeuuid, // tenant or system
	id timeuuid,
	event_type text,
	event_uid text,
	entity_type text,
	entity_id timeuuid,
	body text,
	PRIMARY KEY ((tenant_id, entity_type, entity_id), event_type, event_uid)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.event_by_type_and_id AS
    SELECT *
    FROM thingsboard.event
    WHERE tenant_id IS NOT NULL AND entity_type IS NOT NULL AND entity_id IS NOT NULL AND id IS NOT NULL
    AND event_type IS NOT NULL AND event_uid IS NOT NULL
    PRIMARY KEY ((tenant_id, entity_type, entity_id), event_type, id, event_uid)
    WITH CLUSTERING ORDER BY (event_type ASC, id ASC, event_uid ASC);


CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.event_by_id AS
    SELECT *
    FROM thingsboard.event
    WHERE tenant_id IS NOT NULL AND entity_type IS NOT NULL AND entity_id IS NOT NULL AND id IS NOT NULL
    AND event_type IS NOT NULL AND event_uid IS NOT NULL
    PRIMARY KEY ((tenant_id, entity_type, entity_id), id, event_type, event_uid)
    WITH CLUSTERING ORDER BY (id ASC, event_type ASC, event_uid ASC);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_entity_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, entity_id, entity_type), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_customer_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, customer_id), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_user_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, user_id), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_tenant_id (
  tenant_id timeuuid,
  id timeuuid,
  partition bigint,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, partition), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_tenant_id_partitions (
  tenant_id timeuuid,
  partition bigint,
  PRIMARY KEY (( tenant_id ), partition)
) WITH CLUSTERING ORDER BY ( partition ASC )
AND compaction = { 'class' :  'LeveledCompactionStrategy'  };

CREATE TABLE IF NOT EXISTS thingsboard.msg_queue (
    node_id         timeuuid,
    cluster_partition    bigint,
    ts_partition       bigint,
    ts              bigint,
    msg             blob,
	PRIMARY KEY ((node_id, cluster_partition, ts_partition), ts))
WITH CLUSTERING ORDER BY (ts DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};

CREATE TABLE IF NOT EXISTS thingsboard.msg_ack_queue (
    node_id         timeuuid,
    cluster_partition    bigint,
    ts_partition       bigint,
    msg_id              timeuuid,
	PRIMARY KEY ((node_id, cluster_partition, ts_partition), msg_id))
WITH CLUSTERING ORDER BY (msg_id DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};

CREATE TABLE IF NOT EXISTS thingsboard.processed_msg_partitions (
    node_id         timeuuid,
    cluster_partition    bigint,
    ts_partition       bigint,
	PRIMARY KEY ((node_id, cluster_partition), ts_partition))
WITH CLUSTERING ORDER BY (ts_partition DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};

CREATE TABLE IF NOT EXISTS  thingsboard.rule_chain (
    id uuid,
    tenant_id uuid,
    name text,
    search_text text,
    first_rule_node_id uuid,
    root boolean,
    debug_mode boolean,
    configuration text,
    additional_info text,
    PRIMARY KEY (id, tenant_id)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.rule_chain_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.rule_chain
    WHERE tenant_id IS NOT NULL AND search_text IS NOT NULL AND id IS NOT NULL
    PRIMARY KEY ( tenant_id, search_text, id )
    WITH CLUSTERING ORDER BY ( search_text ASC, id DESC );

CREATE TABLE IF NOT EXISTS  thingsboard.rule_node (
    id uuid,
    rule_chain_id uuid,
    type text,
    name text,
    debug_mode boolean,
    search_text text,
    configuration text,
    additional_info text,
    PRIMARY KEY (id)
);

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

CREATE TABLE IF NOT EXISTS thingsboard.entity_view (
    id timeuuid,
    entity_id timeuuid,
    entity_type text,
    tenant_id timeuuid,
    customer_id timeuuid,
    name text,
    type text,
    keys text,
    start_ts bigint,
    end_ts bigint,
    search_text text,
    additional_info text,
    PRIMARY KEY (id, entity_id, tenant_id, customer_id, type)
);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_name AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND name IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, name, id, customer_id, entity_id, type)
    WITH CLUSTERING ORDER BY (name ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_search_text AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, search_text, id, customer_id, entity_id, type)
    WITH CLUSTERING ORDER BY (search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_by_type_and_search_text AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, type, search_text, id, customer_id, entity_id)
    WITH CLUSTERING ORDER BY (type ASC, search_text ASC, id DESC, customer_id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_customer AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, customer_id, search_text, id, entity_id, type)
    WITH CLUSTERING ORDER BY (customer_id DESC, search_text ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_customer_and_type AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, type, customer_id, search_text, id, entity_id)
    WITH CLUSTERING ORDER BY (type ASC, customer_id DESC, search_text ASC, id DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS thingsboard.entity_view_by_tenant_and_entity_id AS
    SELECT *
    from thingsboard.entity_view
    WHERE tenant_id IS NOT NULL
      AND customer_id IS NOT NULL
      AND entity_id IS NOT NULL
      AND type IS NOT NULL
      AND search_text IS NOT NULL
      AND id IS NOT NULL
    PRIMARY KEY (tenant_id, entity_id, customer_id, search_text, id, type)
    WITH CLUSTERING ORDER BY (entity_id DESC, customer_id DESC, search_text ASC, id DESC);

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
