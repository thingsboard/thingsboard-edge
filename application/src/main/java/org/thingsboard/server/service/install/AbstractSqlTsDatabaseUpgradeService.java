/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@Slf4j
public abstract class AbstractSqlTsDatabaseUpgradeService {

    protected static final String CALL_REGEX = "call ";
    protected static final String DROP_TABLE = "DROP TABLE ";
    protected static final String DROP_PROCEDURE_IF_EXISTS = "DROP PROCEDURE IF EXISTS ";
    protected static final String TS_KV_SQL = "ts_kv.sql";
    protected static final String PATH_TO_USERS_PUBLIC_FOLDER = "C:\\Users\\Public";
    protected static final String THINGSBOARD_WINDOWS_UPGRADE_DIR = "THINGSBOARD_WINDOWS_UPGRADE_DIR";

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    protected InstallScripts installScripts;

    protected abstract void loadSql(Connection conn, String fileName, String version);

    protected void loadFunctions(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
    }

    protected boolean checkVersion(Connection conn) {
        boolean versionValid = false;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT current_setting('server_version_num')");
            resultSet.next();
            if(resultSet.getLong(1) > 110000) {
                versionValid = true;
            }
            statement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    protected boolean isOldSchema(Connection conn, long fromVersion) {
        boolean isOldSchema = true;
        try {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings ( schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version));");
            Thread.sleep(1000);
            ResultSet resultSet = statement.executeQuery("SELECT schema_version FROM tb_schema_settings;");
            if (resultSet.next()) {
                isOldSchema = resultSet.getLong(1) <= fromVersion;
            } else {
                resultSet.close();
                statement.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
            }
            statement.close();
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to check current PostgreSQL schema due to: {}", e.getMessage());
        }
        return isOldSchema;
    }

    protected void executeQuery(Connection conn, String query) {
        try {
            Statement statement = conn.createStatement();
            statement.execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            SQLWarning warnings = statement.getWarnings();
            if (warnings != null) {
                log.info("{}", warnings.getMessage());
                SQLWarning nextWarning = warnings.getNextWarning();
                while (nextWarning != null) {
                    log.info("{}", nextWarning.getMessage());
                    nextWarning = nextWarning.getNextWarning();
                }
            }
            Thread.sleep(2000);
            log.info("Successfully executed query: {}", query);
        } catch (InterruptedException | SQLException e) {
            log.error("Failed to execute query: {} due to: {}", query, e.getMessage());
            throw new RuntimeException("Failed to execute query:" + query + " due to: ", e);
        }
    }

}