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

DROP MATERIALIZED VIEW IF EXISTS thingsboard.rule_by_plugin_token;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.rule_by_tenant_and_search_text;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.plugin_by_api_token;
DROP MATERIALIZED VIEW IF EXISTS thingsboard.plugin_by_tenant_and_search_text;

DROP TABLE IF EXISTS thingsboard.rule;
DROP TABLE IF EXISTS thingsboard.plugin;
