/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

@Service
@Profile("install")
@Slf4j
@SqlTsDao
@PsqlDao
public class PsqlTsDatabaseUpgradeService implements DatabaseTsUpgradeService {

    private static final String CALL_REGEX = "call ";
    private static final String LOAD_FUNCTIONS_SQL = "schema_update_psql_ts.sql";
    private static final String CHECK_VERSION = CALL_REGEX + "check_version()";
    private static final String CREATE_PARTITION_TABLE = CALL_REGEX + "create_partition_table()";
    private static final String CREATE_PARTITIONS = CALL_REGEX + "create_partitions()";
    private static final String CREATE_TS_KV_DICTIONARY_TABLE = CALL_REGEX + "create_ts_kv_dictionary_table()";
    private static final String INSERT_INTO_DICTIONARY = CALL_REGEX + "insert_into_dictionary()";
    private static final String INSERT_INTO_TS_KV = CALL_REGEX + "insert_into_ts_kv()";
    private static final String DROP_OLD_TABLE = "DROP TABLE ts_kv_old;";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private InstallScripts installScripts;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "2.4.3":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating timeseries schema ...");
                    log.info("Load upgrade functions ...");
                    loadSql(conn);
                    log.info("Upgrade functions successfully loaded!");
                    boolean versionValid = checkVersion(conn);
                    if (!versionValid) {
                        log.info("PostgreSQL version should be at least more than 10!");
                        log.info("Please upgrade your PostgreSQL and restart the script!");
                    } else {
                        log.info("PostgreSQL version is valid!");
                        log.info("Updating schema ...");
                        executeFunction(conn, CREATE_PARTITION_TABLE);
                        executeFunction(conn, CREATE_PARTITIONS);
                        executeFunction(conn, CREATE_TS_KV_DICTIONARY_TABLE);
                        executeFunction(conn, INSERT_INTO_DICTIONARY);
                        executeFunction(conn, INSERT_INTO_TS_KV);
                        dropOldTable(conn, DROP_OLD_TABLE);
                        log.info("schema timeseries updated!");
                    }
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void loadSql(Connection conn) {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.4.3", LOAD_FUNCTIONS_SQL);
        try {
            loadFunctions(schemaUpdateFile, conn);
        } catch (Exception e) {
            log.info("Failed to load PostgreSQL upgrade functions due to: {}", e.getMessage());
        }
    }

    private void loadFunctions(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
    }

    private boolean checkVersion(Connection conn) {
        log.info("Check the current PostgreSQL version...");
        boolean versionValid = false;
        try {
            CallableStatement callableStatement = conn.prepareCall("{? = " + CHECK_VERSION + " }");
            callableStatement.registerOutParameter(1, Types.BOOLEAN);
            callableStatement.execute();
            versionValid = callableStatement.getBoolean(1);
            callableStatement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    private void executeFunction(Connection conn, String query) {
        log.info("{} ... ", query);
        try {
            CallableStatement callableStatement = conn.prepareCall("{" + query + "}");
            callableStatement.execute();
            callableStatement.close();
            log.info("Successfully executed: {}", query.replace(CALL_REGEX, ""));
        } catch (Exception e) {
            log.info("Failed to execute {} due to: {}", query, e.getMessage());
        }
    }

    private void dropOldTable(Connection conn, String query) {
        try {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to drop table {} due to: {}", query.replace("DROP TABLE ", ""), e.getMessage());
        }
    }
}