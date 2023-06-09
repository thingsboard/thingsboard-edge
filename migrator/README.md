
## Modes
The service has the following modes (one of them must be specified as `MIGRATOR_MODE` env variable):

<br/>

#### _POSTGRES_TENANT_DATA_EXPORT_ - to export tenant-related data from Postgres

Mandatory configuration params:

- `WORKING_DIRECTORY` - the directory where the data should be saved to
- `DATABASE_URL` - JDBC url
- `DATABASE_USERNAME` - DB username, 'postgres' by default
- `DATABASE_PASSWORD` - DB password, 'postgres' by default
- `EXPORTED_TENANT_ID` - id of the tenant to export data for

Additional configuration:

- `SKIPPED_TABLES` - comma-separated list of table identifiers that should not be exported (all available table
  identifiers are listed later below)
- `EXPORT_SQL_BATCH_SIZE` - batch size to retrieve at a time, 500 by default
- `EXPORT_SQL_DELAY_BETWEEN_QUERIES` - delay between SELECT queries in milliseconds, 0 by default

When exporting data from partitioned tables, the export is done partition by partition.
For that, you should check the defaults and reconfigure the following env variables:

- `EVENTS_PARTITION_SIZE` - 168 hours by default
- `DEBUG_EVENTS_PARTITION_SIZE` - 1 hour by default
- `EDGE_EVENTS_PARTITION_SIZE`- 168 hours by default
- `AUDIT_LOGS_PARTITION_SIZE` - 168 hours by default
- `BLOB_ENTITIES_PARTITION_SIZE` - 168 hours by default

Data for each table is stored in a separate file named same as the table.
The content is the list of JSON objects, each one representing the row.

#### _POSTGRES_TENANT_DATA_IMPORT_ - to import previously exported tenant-related data

Configuration:

- `WORKING_DIRECTORY` - the directory where the exported data is stored
- `DATABASE_URL` - JDBC url
- `DATABASE_USERNAME` - DB username, 'postgres' by default
- `DATABASE_PASSWORD` - DB password, 'postgres' by default
- `SKIPPED_TABLES` - skip importing for these tables
- `IMPORT_SQL_DELAY_BETWEEN_QUERIES` - delay between INSERT queries, 0 by default
- `IMPORT_SQL_ENABLE_PARTITION_CREATION` - enabled creation (if not exist) of partitions, true by default.
  If enabled, review partition sizes configuration listed above for `POSTGRES_TENANT_DATA_EXPORT`
- `IMPORT_SQL_UPDATE_TENANT_PROFILE` - replace tenant profile of the tenant with default tenant profile, true by default
- `IMPORT_SQL_UPDATE_TS_KV_DICTIONARY` - when importing latest kv, to insert unknown keys into ts_kv_dictionary, true by
  default
- `IMPORT_SQL_RESOLVE_UNKNOWN_ROLES` - replace unknown role id (usually system role) with the one found by name, true by
  default

The list of supported tables (their identifiers to use in configuration):

`TENANT` ('tenant'), `CUSTOMER` ('customer'), `ADMIN_SETTINGS` ('admin_settings'), `QUEUE` ('queue'),
`RPC` ('rpc'), `RULE_CHAIN` ('rule_chain'), `DEVICE_PROFILE` ('device_profile'), `OTA_PACKAGE` ('ota_package'),
`RESOURCE` ('resource'), `API_USAGE_STATE` ('api_usage_state'), `ROLE` ('role'), `ENTITY_GROUP` ('entity_group'),
`DEVICE_GROUP_OTA_PACKAGE` ('device_group_ota_package'), `GROUP_PERMISSION` ('group_permission'),
`BLOB_ENTITY` ('blob_entity'), `SCHEDULER_EVENT` ('scheduler_event'), `RULE_CHAIN_DEBUG_EVENT` ('rule_chain_debug_event'),
`RULE_NODE` ('rule_node'), `RULE_NODE_DEBUG_EVENT` ('rule_node_debug_event'), `CONVERTER` ('converter'),
`CONVERTER_DEBUG_EVENT` ('converter_debug_event'), `INTEGRATION` ('integration'),
`INTEGRATION_DEBUG_EVENT` ('integration_debug_event'), `USER` ('tb_user'), `USER_CREDENTIALS` ('user_credentials'),
`USER_AUTH_SETTINGS` ('user_auth_settings'), `EDGE` ('edge'), `EDGE_EVENT` ('edge_event'),
`WIDGETS_BUNDLE` ('widgets_bundle'), `WIDGET_TYPE` ('widget_type'), `DASHBOARD` ('dashboard'), `DEVICE` ('device'),
`DEVICE_CREDENTIALS` ('device_credentials'), `ASSET_PROFILE` ('asset_profile'), `ASSET` ('asset'),
`ENTITY_VIEW` ('entity_view'), `ALARM` ('alarm'), `ENTITY_ALARM` ('entity_alarm'), `ERROR_EVENT` ('error_event'),
`LC_EVENT` ('lc_event'), `RAW_DATA_EVENT` ('raw_data_event'), `STATS_EVENT` ('stats_event'),
`OAUTH2_PARAMS` ('oauth2_params'), `OAUTH2_DOMAIN` ('oauth2_domain'), `OAUTH2_MOBILE` ('oauth2_mobile'),
`OAUTH2_REGISTRATION` ('oauth2_registration'), `RULE_NODE_STATE` ('rule_node_state'), `AUDIT_LOG` ('audit_log'),
`RELATION` ('relation'), `ATTRIBUTE` ('attribute_kv'), `LATEST_KV` ('ts_kv_latest'),

<br/>
<br/>

#### _CASSANDRA_TENANT_DATA_EXPORT_ - to export tenant-related timeseries data from Cassandra

Configuration:

- `WORKING_DIRECTORY` - the directory where the data should be saved to
- `CASSANDRA_URL` - Cassandra contact points
- `CASSANDRA_LOCAL_DATACENTER` - datacenter, 'datacenter1' by default
- `CASSANDRA_KEYSPACE_NAME` - keyspace name, 'thingsboard' by default
- `CASSANDRA_REQUEST_PAGE_SIZE` - batch size to retrieve at a time, 1000 by default
- `CASSANDRA_REQUEST_TIMEOUT` - timeout for Cassandra request, connect and query init, 10 seconds by default

With this mode, the service reads previously exported latest_kv data, so **make sure you've run
POSTGRES_TENANT_DATA_EXPORT first** (with `LATEST_KV` not skipped).
Timeseries data will be saved to 'ts_kv.gz' file in a Gzip format.

#### _CASSANDRA_TENANT_DATA_IMPORT_ - to import previously exported timeseries data

Configuration:

- `WORKING_DIRECTORY` - the directory where the 'ts_kv.gz' is located
- `CASSANDRA_URL` - Cassandra contact points
- `CASSANDRA_LOCAL_DATACENTER` - datacenter, 'datacenter1' by default
- `CASSANDRA_KEYSPACE_NAME` - keyspace name, 'thingsboard' by default
- `CASSANDRA_REQUEST_PAGE_SIZE` - batch size to retrieve at a time, 1000 by default
- `CASSANDRA_REQUEST_TIMEOUT` - timeout for Cassandra request, connect and query init, 10 seconds by default
- `CASSANDRA_TS_KV_TTL` - ttl to set for records, 2 years by default
- `PARALLELISM_LEVEL` - thread pool size to execute Cassandra queries, 4 by default

<br/>
<br/>

#### _CASSANDRA_LATEST_KV_EXPORT_ - to export all latest kv data from Cassandra

Configuration:

- `WORKING_DIRECTORY` - the directory where the data should be saved to
- `CASSANDRA_URL` - Cassandra contact points
- `CASSANDRA_LOCAL_DATACENTER` - datacenter, 'datacenter1' by default
- `CASSANDRA_KEYSPACE_NAME` - keyspace name, 'thingsboard' by default
- `CASSANDRA_REQUEST_PAGE_SIZE` - batch size to retrieve at a time, 1000 by default
- `CASSANDRA_REQUEST_TIMEOUT` - timeout for Cassandra request, connect and query init, 10 seconds by default

#### _POSTGRES_LATEST_KV_IMPORT_ - to import previously exported latest kv data from Cassandra to Postgres

Configuration:

- `WORKING_DIRECTORY` - the directory where the exported data is stored
- `DATABASE_URL` - JDBC url
- `DATABASE_USERNAME` - DB username, 'postgres' by default
- `DATABASE_PASSWORD` - DB password, 'postgres' by default
- `IMPORT_SQL_DELAY_BETWEEN_QUERIES` - delay between INSERT queries, 0 by default
- `IMPORT_SQL_IGNORE_CONFLICTS` - ignore conflicts when some records already exist

<br/>

## Usage

`cd` to `migrator` directory and build using `mvn clean install`.

Executable jar `migrator.jar` will be inside the `target` directory.

Then, `source` your .conf file and run the jar using `java -jar migrator.jar`.

<br/>

### Example

##### Export

Create configuration file:

```shell
export MIGRATOR_MODE='POSTGRES_TENANT_DATA_EXPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export DATABASE_URL='jdbc:postgresql://localhost:5432/thingsboard'
export DATABASE_USERNAME='postgres'
export DATABASE_PASSWORD='postgres'
export EXPORTED_TENANT_ID='46bc3940-3e5b-11ed-93f9-996bd9a9d291'

export SKIPPED_TABLES='RULE_NODE_DEBUG_EVENT,CONVERTER_DEBUG_EVENT,INTEGRATION_DEBUG_EVENT'
export EXPORT_SQL_BATCH_SIZE=500
export EXPORT_SQL_DELAY_BETWEEN_QUERIES=15
```

After that, apply the config and run the jar:

```shell
source migrator.conf
java -jar migrator.jar
```

Edit the conf file to execute Cassandra data export:

```shell
export MIGRATOR_MODE='CASSANDRA_TENANT_DATA_EXPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export CASSANDRA_URL='localhost:9042'
```

Then, apply the config and run the jar again.

##### Import

Config file for importing PostgreSQL data:

```shell
export MIGRATOR_MODE='POSTGRES_TENANT_DATA_IMPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export DATABASE_URL='jdbc:postgresql://localhost:5432/thingsboard'
export DATABASE_USERNAME='postgres'
export DATABASE_PASSWORD='postgres'

export SKIPPED_TABLES='RULE_NODE_DEBUG_EVENT,CONVERTER_DEBUG_EVENT,INTEGRATION_DEBUG_EVENT'
export IMPORT_SQL_DELAY_BETWEEN_QUERIES=15
```

To import Cassandra data:

```shell
export MIGRATOR_MODE='CASSANDRA_TENANT_DATA_IMPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export CASSANDRA_URL='localhost:9042'

export CASSANDRA_TS_KV_TTL=0
```
