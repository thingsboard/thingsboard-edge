The service can export all data related to certain tenant from PostgreSQL database and Cassandra,
and then import previously exported data.

The service has 4 modes (one of them must be specified as `MIGRATOR_MODE` env variable):

#### SQL_DATA_EXPORT
Mandatory configuration params:
- `WORKING_DIRECTORY` - the directory where the data should be saved to
- `DATABASE_URL` - JDBC url
- `DATABASE_USERNAME` - DB username, 'postgres' by default 
- `DATABASE_PASSWORD` - DB password, 'postgres' by default
- `EXPORTED_TENANT_ID` - id of the tenant to export data for

Additional configuration:
- `SKIPPED_TABLES` - comma-separated list of table identifiers that should not be exported (all available table identifiers are listed later below)
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

#### CASSANDRA_DATA_EXPORT
Configuration:
- `WORKING_DIRECTORY` - the directory where the data should be saved to
- `CASSANDRA_URL` - Cassandra contact points
- `CASSANDRA_LOCAL_DATACENTER` - datacenter, 'datacenter1' by default
- `CASSANDRA_KEYSPACE_NAME` - keyspace name, 'thingsboard' by default
- `CASSANDRA_REQUEST_PAGE_SIZE` - batch size to retrieve at a time, 1000 by default
- `CASSANDRA_REQUEST_TIMEOUT` - timeout for Cassandra request, connect and query init, 10 seconds by default

With this mode, the service reads previously exported latest_kv data, so make sure you've run SQL data export first.
Timeseries data will be saved to 'ts_kv.gz' file in a Gzip format.

#### SQL_DATA_IMPORT
Configuration:
- `WORKING_DIRECTORY` - the directory where the exported data is stored
- `DATABASE_URL` - JDBC url
- `DATABASE_USERNAME` - DB username, 'postgres' by default
- `DATABASE_PASSWORD` - DB password, 'postgres' by default
- `SKIPPED_TABLES` - skip importing for these tables
- `IMPORT_SQL_DELAY_BETWEEN_QUERIES` - delay between INSERT queries, 0 by default
- `IMPORT_SQL_ENABLE_PARTITION_CREATION` - enabled creation (if not exist) of partitions, true by default.
  If enabled, review partition sizes configuration listed above for `SQL_DATA_EXPORT`
- `IMPORT_SQL_UPDATE_TENANT_PROFILE` - replace tenant profile of the tenant with default tenant profile, true by default
- `IMPORT_SQL_UPDATE_TS_KV_DICTIONARY` - when importing latest kv, to insert unknown keys into ts_kv_dictionary, true by default
- `IMPORT_SQL_RESOLVE_UNKNOWN_ROLES` - replace unknown role id (usually system role) with the one found by name, true by default

#### CASSANDRA_DATA_IMPORT
- `WORKING_DIRECTORY` - the directory where the 'ts_kv.gz' is located
- `CASSANDRA_URL` - Cassandra contact points
- `CASSANDRA_LOCAL_DATACENTER` - datacenter, 'datacenter1' by default
- `CASSANDRA_KEYSPACE_NAME` - keyspace name, 'thingsboard' by default
- `CASSANDRA_REQUEST_PAGE_SIZE` - batch size to retrieve at a time, 1000 by default
- `CASSANDRA_REQUEST_TIMEOUT` - timeout for Cassandra request, connect and query init, 10 seconds by default
- `CASSANDRA_TS_KV_TTL` - ttl to set for records, 2 years by default
- `PARALLELISM_LEVEL` - thread pool size to execute Cassandra queries, 4 by default
- `CASSANDRA_STATS_PRINT_INTERVAL` - log the amount of total inserted records and the last row inserted after each n records, 10000 by default

The list of supported tables and their identifiers:

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


#### Building

cd to tenant-migrator and build using `mvn clean install`

Executable jar `tenant-migrator.jar` will be inside the target directory.


#### Usage example

##### Export

Create configuration file:
```shell
export MIGRATOR_MODE='SQL_DATA_EXPORT'
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
source your-conf-file.conf
java -jar tenant-migrator.jar
```

Edit the conf file to execute Cassandra data export:
```shell
export MIGRATOR_MODE='CASSANDRA_DATA_EXPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export CASSANDRA_URL='localhost:9042'
```
Then, apply the config and run the jar again.

##### Import

Config file for importing PostgreSQL data:
```shell
export MIGRATOR_MODE='SQL_DATA_IMPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export DATABASE_URL='jdbc:postgresql://localhost:5432/thingsboard'
export DATABASE_USERNAME='postgres'
export DATABASE_PASSWORD='postgres'

export SKIPPED_TABLES='RULE_NODE_DEBUG_EVENT,CONVERTER_DEBUG_EVENT,INTEGRATION_DEBUG_EVENT'
export IMPORT_SQL_DELAY_BETWEEN_QUERIES=15
```

To import Cassandra data:
```shell
export MIGRATOR_MODE='CASSANDRA_DATA_IMPORT'
export WORKING_DIRECTORY='/home/ubuntu/migration/data'
export CASSANDRA_URL='localhost:9042'

export CASSANDRA_TS_KV_TTL=0
export CASSANDRA_STATS_PRINT_INTERVAL=5000
```
