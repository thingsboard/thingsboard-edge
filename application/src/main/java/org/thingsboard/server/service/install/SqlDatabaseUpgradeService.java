/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.service.install.cql.CassandraDbHelper;
import org.thingsboard.server.service.install.sql.SqlDbHelper;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLSyntaxErrorException;

import static org.thingsboard.server.service.install.DatabaseHelper.*;
import static org.thingsboard.server.service.install.DatabaseHelper.CONFIGURATION;

@Service
@Profile("install")
@Slf4j
@SqlDao
public class SqlDatabaseUpgradeService implements DatabaseUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    @Value("${install.data_dir}")
    private String dataDir;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private DashboardService dashboardService;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.3.0":
                log.info("Updating schema ...");
                Path schemaUpdateFile = Paths.get(this.dataDir, "upgrade", "1.3.1", SCHEMA_UPDATE_SQL);
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    String sql = new String(Files.readAllBytes(schemaUpdateFile), Charset.forName("UTF-8"));
                    conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                }
                log.info("Schema updated.");
                break;
            case "1.3.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {

                    log.info("Dumping dashboards ...");
                    Path dashboardsDump = SqlDbHelper.dumpTableIfExists(conn, DASHBOARD,
                            new String[]{ID, TENANT_ID, CUSTOMER_ID, TITLE, SEARCH_TEXT, ASSIGNED_CUSTOMERS, CONFIGURATION},
                            new String[]{"", "", "", "", "", "", ""},
                            "tb-dashboards", true);
                    log.info("Dashboards dumped.");

                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(this.dataDir, "upgrade", "1.4.0", SCHEMA_UPDATE_SQL);
                    String sql = new String(Files.readAllBytes(schemaUpdateFile), Charset.forName("UTF-8"));
                    conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    log.info("Schema updated.");

                    log.info("Restoring dashboards ...");
                    if (dashboardsDump != null) {
                        SqlDbHelper.loadTable(conn, DASHBOARD,
                                new String[]{ID, TENANT_ID, TITLE, SEARCH_TEXT, CONFIGURATION}, dashboardsDump, true);
                        DatabaseHelper.upgradeTo40_assignDashboards(dashboardsDump, dashboardService, true);
                        Files.deleteIfExists(dashboardsDump);
                    }
                    log.info("Dashboards restored.");
                }
                break;
            case "1.4.0":
                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(this.dataDir, "upgrade", "1.4.0pe", SCHEMA_UPDATE_SQL);
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    String sql = new String(Files.readAllBytes(schemaUpdateFile), Charset.forName("UTF-8"));
                    conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    try {
                        conn.createStatement().execute("ALTER TABLE integration ADD COLUMN downlink_converter_id varchar(31)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (SQLSyntaxErrorException e) {}
                    conn.createStatement().execute("update converter set type = 'UPLINK' where type = 'CUSTOM'"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                }
                log.info("Schema updated.");
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }
}
