/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.integration.IntegrationRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "3.3.2":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    try {
                        conn.createStatement().execute("insert into entity_alarm(tenant_id, entity_id, created_time, alarm_type, customer_id, alarm_id)" +
                                " select tenant_id, originator_id, created_time, type, customer_id, id from alarm ON CONFLICT DO NOTHING;");
                        conn.createStatement().execute("insert into entity_alarm(tenant_id, entity_id, created_time, alarm_type, customer_id, alarm_id)" +
                                " select a.tenant_id, r.from_id, created_time, type, customer_id, id" +
                                " from alarm a inner join relation r on r.relation_type_group = 'ALARM' and r.relation_type = 'ANY' and a.id = r.to_id ON CONFLICT DO NOTHING;");
                        conn.createStatement().execute("delete from relation r where r.relation_type_group = 'ALARM';");
                    } catch (Exception e) {
                        log.error("Failed to update alarm relations!!!", e);
                    }

                    log.info("Updating lwm2m device profiles ...");
                    try {
                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.2", "schema_update_lwm2m_bootstrap.sql");
                        loadSql(schemaUpdateFile, conn);
                        log.info("Updating server`s public key from HexDec to Base64 in profile for LWM2M...");
                        conn.createStatement().execute("call update_profile_bootstrap();");
                        log.info("Server`s public key from HexDec to Base64 in profile for LWM2M updated.");
                        log.info("Updating client`s public key and secret key from HexDec to Base64 for LWM2M...");
                        conn.createStatement().execute("call update_device_credentials_to_base64_and_bootstrap();");
                        log.info("Client`s public key and secret key from HexDec to Base64 for LWM2M updated.");
                    } catch (Exception e) {
                        log.error("Failed to update lwm2m profiles!!!", e);
                    }
                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3003003;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.3.3":
                log.info("Updating schema ...");
                Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.3pe", SCHEMA_UPDATE_SQL);
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    loadSql(schemaUpdateFile, conn);
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN downlink_converter_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE customer ADD COLUMN parent_customer_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE dashboard ADD COLUMN customer_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE entity_group ADD COLUMN owner_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE entity_group ADD COLUMN owner_type varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    conn.createStatement().execute("update converter set type = 'UPLINK' where type = 'CUSTOM'"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN secret varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN is_remote boolean"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN enabled boolean DEFAULT true "); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE entity_group ADD CONSTRAINT group_name_per_owner_unq_key UNIQUE (owner_id, owner_type, type, name)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN allow_create_devices_or_assets boolean DEFAULT true "); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE oauth2_registration ADD COLUMN basic_parent_customer_name_pattern varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE oauth2_registration ADD COLUMN basic_user_groups_name_pattern varchar(1024)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE oauth2_client_registration_template ADD COLUMN basic_parent_customer_name_pattern varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE oauth2_client_registration_template ADD COLUMN basic_user_groups_name_pattern varchar(1024)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}
                    try {
                        conn.createStatement().execute("ALTER TABLE edge_event ADD COLUMN entity_group_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {}

                    integrationRepository.findAll().forEach(integration -> {
                        if (integration.getType().equals(IntegrationType.AZURE_EVENT_HUB)) {
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
                } catch(Exception e) {
                    log.error("Failed to update schema!!!");
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        Statement st = conn.createStatement();
        st.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(3));
        st.execute(sql);//NOSONAR, ignoring because method used to execute thingsboard database upgrade script
        printWarnings(st);
        Thread.sleep(5000);
    }

    protected void printWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }
}
