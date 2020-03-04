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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;

@Slf4j
public abstract class AbstractSqlTsDatabaseUpgradeService {

    protected static final String CALL_REGEX = "call ";
    protected static final String CHECK_VERSION = "check_version()";
    protected static final String DROP_TABLE = "DROP TABLE ";
    protected static final String DROP_FUNCTION_IF_EXISTS = "DROP FUNCTION IF EXISTS ";

    private static final String CALL_CHECK_VERSION = CALL_REGEX + CHECK_VERSION;


    private static final String FUNCTION = "function: {}";
    private static final String DROP_STATEMENT = "drop statement: {}";
    private static final String QUERY = "query: {}";
    private static final String SUCCESSFULLY_EXECUTED = "Successfully executed ";
    private static final String FAILED_TO_EXECUTE = "Failed to execute ";
    private static final String FAILED_DUE_TO = " due to: {}";

    protected static final String SUCCESSFULLY_EXECUTED_FUNCTION = SUCCESSFULLY_EXECUTED + FUNCTION;
    protected static final String FAILED_TO_EXECUTE_FUNCTION_DUE_TO = FAILED_TO_EXECUTE + FUNCTION + FAILED_DUE_TO;

    protected static final String SUCCESSFULLY_EXECUTED_DROP_STATEMENT = SUCCESSFULLY_EXECUTED + DROP_STATEMENT;
    protected static final String FAILED_TO_EXECUTE_DROP_STATEMENT = FAILED_TO_EXECUTE + DROP_STATEMENT + FAILED_DUE_TO;

    protected static final String SUCCESSFULLY_EXECUTED_QUERY = SUCCESSFULLY_EXECUTED + QUERY;
    protected static final String FAILED_TO_EXECUTE_QUERY = FAILED_TO_EXECUTE + QUERY + FAILED_DUE_TO;

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    protected InstallScripts installScripts;

    protected abstract void loadSql(Connection conn);

    protected void loadFunctions(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
    }

    protected boolean checkVersion(Connection conn) {
        log.info("Check the current PostgreSQL version...");
        boolean versionValid = false;
        try {
            CallableStatement callableStatement = conn.prepareCall("{? = " + CALL_CHECK_VERSION + " }");
            callableStatement.registerOutParameter(1, Types.BOOLEAN);
            callableStatement.execute();
            versionValid = callableStatement.getBoolean(1);
            callableStatement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    protected void executeFunction(Connection conn, String query) {
        log.info("{} ... ", query);
        try {
            CallableStatement callableStatement = conn.prepareCall("{" + query + "}");
            callableStatement.execute();
            SQLWarning warnings = callableStatement.getWarnings();
            if (warnings != null) {
                log.info("{}", warnings.getMessage());
                SQLWarning nextWarning = warnings.getNextWarning();
                while (nextWarning != null) {
                    log.info("{}", nextWarning.getMessage());
                    nextWarning = nextWarning.getNextWarning();
                }
            }
            callableStatement.close();
            log.info(SUCCESSFULLY_EXECUTED_FUNCTION, query.replace(CALL_REGEX, ""));
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info(FAILED_TO_EXECUTE_FUNCTION_DUE_TO, query, e.getMessage());
        }
    }

    protected void executeDropStatement(Connection conn, String query) {
        try {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_DROP_STATEMENT, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_DROP_STATEMENT, query, e.getMessage());
        }
    }

    protected void executeQuery(Connection conn, String query) {
        try {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_QUERY, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_QUERY, query, e.getMessage());
        }
    }

}