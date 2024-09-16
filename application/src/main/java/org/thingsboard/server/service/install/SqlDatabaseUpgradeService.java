/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.sql.integration.IntegrationRepository;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLWarning;
import java.util.concurrent.TimeUnit;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    private final InstallScripts installScripts;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SqlDatabaseUpgradeService(InstallScripts installScripts, JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.installScripts = installScripts;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout((int) TimeUnit.MINUTES.toSeconds(120));
    }

    @Lazy
    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    public void upgradeDatabase(String fromVersion) {
        switch (fromVersion) {
            case "3.5.0" -> updateSchema("3.5.0", 3005000, "3.5.1", 3005001);
            case "3.5.1" -> {
                updateSchema("3.5.1", 3005001, "3.6.0", 3006000);

                String[] tables = new String[]{"converter", "integration", "device", "component_descriptor", "customer", "dashboard",
                        "rule_chain", "rule_node", "asset_profile", "asset", "device_profile", "tb_user", "tenant_profile", "tenant",
                        "widgets_bundle", "scheduler_event", "blob_entity", "entity_view", "role", "edge", "ota_package"};
                for (String table : tables) {
                    execute("ALTER TABLE " + table + " DROP COLUMN IF EXISTS search_text CASCADE");
                }
                execute(
                        "ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;",
                        "ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;",
                        "CREATE INDEX IF NOT EXISTS idx_rule_node_type_configuration_version ON rule_node(type, configuration_version);",
                        "UPDATE rule_node SET " +
                                "configuration = (configuration::jsonb || '{\"updateAttributesOnlyOnValueChange\": \"false\"}'::jsonb)::varchar, " +
                                "configuration_version = 1 " +
                                "WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version < 1;",
                        "CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_unread ON notification(recipient_id) WHERE status <> 'READ';"
                );
            }
            case "3.6.0" -> updateSchema("3.6.0", 3006000, "3.6.1", 3006001);
            case "3.6.1" -> {
                updateSchema("3.6.1", 3006001, "3.6.2", 3006002);

                try {
                    Path saveAttributesNodeUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.6.1", "save_attributes_node_update.sql");
                    loadSql(saveAttributesNodeUpdateFile);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for save attributes rule nodes due to: ", e);
                }
                try {
                    execute("UPDATE rule_node SET " +
                            "configuration = (configuration::jsonb - 'groupOwnerId')::varchar, " +
                            "configuration_version = 1 " +
                            "WHERE type = 'org.thingsboard.rule.engine.transform.TbDuplicateMsgToGroupNode' AND configuration_version < 1;", false);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for duplicate to group rule nodes due to: ", e);
                }
                execute("CREATE INDEX IF NOT EXISTS idx_asset_profile_id ON asset(tenant_id, asset_profile_id);");
            }
            case "3.6.2" -> {
                updateSchema("3.6.2", 3006002, "3.6.3", 3006003);

                try {
                    execute("UPDATE entity_group SET " +
                            "name = CONCAT('[Edge][', customer.title, ']', SUBSTRING(entity_group.name, POSITION(']' IN entity_group.name) + 1)) " +
                            "FROM customer " +
                            "WHERE entity_group.owner_id = customer.id " +
                            "AND entity_group.owner_type = 'CUSTOMER' " +
                            "AND entity_group.name LIKE '[Edge]%All'" +
                            "AND NOT entity_group.name LIKE CONCAT('[Edge][', customer.title, ']%');", false);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for edge entity group All for customer level due to: ", e);
                }
            }
            case "3.6.3" -> updateSchema("3.6.3", 3006003, "3.6.4", 3006004);
            case "3.6.4" -> updateSchema("3.6.4", 3006004, "3.7.0", 3007000);
            case "3.7.0" -> {
                updateSchema("3.7.0", 3007000, "3.7.1", 3007001);

                try {
                    execute("UPDATE rule_node SET " +
                            "configuration = CASE " +
                            "  WHEN (configuration::jsonb ->> 'persistAlarmRulesState') = 'false'" +
                            "  THEN (configuration::jsonb || '{\"fetchAlarmRulesStateOnStart\": \"false\"}'::jsonb)::varchar " +
                            "  ELSE configuration " +
                            "END, " +
                            "configuration_version = 1 " +
                            "WHERE type = 'org.thingsboard.rule.engine.profile.TbDeviceProfileNode' " +
                            "AND configuration_version < 1;", false);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for device profile rule nodes due to: ", e);
                }
            }
            case "3.7.1" -> updateSchema("3.7.1", 3007001, "3.7.2", 3007002);
            case "ce" -> {
                log.info("Updating schema ...");
                try {
                    loadSql(getSchemaUpdateFile("pe"));

                    String[] tables = new String[]{"device"};
                    for (String table : tables) {
                        execute("ALTER TABLE " + table + " DROP COLUMN IF EXISTS search_text CASCADE");
                    }
                    execute(
                            "ALTER TABLE customer ADD COLUMN parent_customer_id uuid",
                            "ALTER TABLE dashboard ADD COLUMN customer_id uuid",
                            "ALTER TABLE integration ADD COLUMN downlink_converter_id uuid",
                            "ALTER TABLE entity_group ADD COLUMN owner_id uuid",
                            "ALTER TABLE entity_group ADD COLUMN owner_type varchar(255)",
                            "UPDATE converter SET type = 'UPLINK' WHERE type = 'CUSTOM'",
                            "ALTER TABLE integration ADD COLUMN secret varchar(255)",
                            "ALTER TABLE integration ADD COLUMN is_remote boolean",
                            "ALTER TABLE integration ADD COLUMN enabled boolean DEFAULT true ",
                            "ALTER TABLE entity_group ADD CONSTRAINT group_name_per_owner_unq_key UNIQUE (owner_id, owner_type, type, name)",
                            "ALTER TABLE integration ADD COLUMN allow_create_devices_or_assets boolean DEFAULT true ",
                            "ALTER TABLE oauth2_client ADD COLUMN basic_parent_customer_name_pattern varchar(255)",
                            "ALTER TABLE oauth2_client ADD COLUMN basic_user_groups_name_pattern varchar(1024)",
                            "ALTER TABLE oauth2_client_registration_template ADD COLUMN basic_parent_customer_name_pattern varchar(255)",
                            "ALTER TABLE oauth2_client_registration_template ADD COLUMN basic_user_groups_name_pattern varchar(1024)",
                            "ALTER TABLE edge_event ADD COLUMN entity_group_id uuid;",
                            "ALTER TABLE alarm ADD COLUMN propagate_to_owner_hierarchy boolean DEFAULT false;",
                            "ALTER TABLE edge ADD COLUMN edge_license_key varchar(30) DEFAULT 'PUT_YOUR_EDGE_LICENSE_HERE';",
                            "ALTER TABLE edge ADD COLUMN cloud_endpoint varchar(255) DEFAULT 'PUT_YOUR_CLOUD_ENDPOINT_HERE';",
                            "ALTER TABLE scheduler_event ADD COLUMN originator_id uuid;",
                            "ALTER TABLE scheduler_event ADD COLUMN originator_type varchar(255);",
                            "UPDATE scheduler_event set originator_id = ((configuration::json)->'originatorId'->>'id')::uuid, " +
                                    "originator_type = (configuration::json)->'originatorId'->>'entityType' where originator_id IS NULL;",
                            "CREATE INDEX IF NOT EXISTS idx_customer_tenant_id_parent_customer_id ON customer(tenant_id, parent_customer_id);",
                            "CREATE INDEX IF NOT EXISTS idx_scheduler_event_originator_id ON scheduler_event(tenant_id, originator_id);"
                    );

                    integrationRepository.findAll().forEach(integration -> {
                        if (integration.getType().equals(IntegrationType.LORIOT)) {
                            ObjectNode credentials = (ObjectNode) integration.getConfiguration().get("credentials");
                            if (credentials.get("type").asText().equals("basic") && !credentials.has("username")) {
                                credentials.set("username", credentials.get("email"));
                                credentials.remove("email");
                                integrationRepository.save(integration);
                            }

                        } else if (integration.getType().equals(IntegrationType.AZURE_EVENT_HUB)) {
                            ObjectNode clientConfiguration = (ObjectNode) integration.getConfiguration().get("clientConfiguration");
                            if (!clientConfiguration.has("connectionString")) {
                                String connectionString = String.format("Endpoint=sb://%s.servicebus.windows.net/;SharedAccessKeyName=%s;SharedAccessKey=%s;EntityPath=%s",
                                        clientConfiguration.get("namespaceName").textValue(),
                                        clientConfiguration.get("sasKeyName").textValue(),
                                        clientConfiguration.get("sasKey").textValue(),
                                        clientConfiguration.get("eventHubName").textValue());
                                clientConfiguration.put("connectionString", connectionString);
                                clientConfiguration.remove("namespaceName");
                                clientConfiguration.remove("eventHubName");
                                clientConfiguration.remove("sasKeyName");
                                clientConfiguration.remove("sasKey");
                            }
                            integrationRepository.save(integration);
                        }
                    });
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed to update schema", e);
                }
            }
            default -> throw new RuntimeException("Unsupported fromVersion '" + fromVersion + "'");
        }
    }

    private void updateSchema(String oldVersionStr, int oldVersion, String newVersionStr, int newVersion) {
        try {
            transactionTemplate.executeWithoutResult(ts -> {
                log.info("Updating schema ...");
                if (isOldSchema(oldVersion)) {
                    loadSql(getSchemaUpdateFile(oldVersionStr));
                    jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + newVersion);
                    log.info("Schema updated to version {}", newVersionStr);
                } else {
                    log.info("Skip schema re-update to version {}. Use env flag 'SKIP_SCHEMA_VERSION_CHECK' to force the re-update.", newVersionStr);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to update schema", e);
        }
    }

    private Path getSchemaUpdateFile(String version) {
        return Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
    }

    private void loadSql(Path sqlFile) {
        String sql;
        try {
            sql = Files.readString(sqlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
            stmt.execute(sql);
            printWarnings(stmt.getWarnings());
            return null;
        });
    }

    private void execute(@Language("sql") String... statements) {
        for (String statement : statements) {
            execute(statement, true);
        }
    }

    private void execute(@Language("sql") String statement, boolean ignoreErrors) {
        try {
            jdbcTemplate.execute(statement);
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }
        }
    }

    private void printWarnings(SQLWarning warnings) {
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    private boolean isOldSchema(long fromVersion) {
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to true!");
            return true;
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings (schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version))");
        Long schemaVersion = jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class).stream().findFirst().orElse(null);
        boolean isOldSchema = true;
        if (schemaVersion != null) {
            isOldSchema = schemaVersion <= fromVersion;
        } else {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
        }
        return isOldSchema;
    }

}
