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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.PsqlDao;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
@TimescaleDBTsDao
@PsqlDao
@Profile("install")
@Slf4j
public class TimescaleTsDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements TsDatabaseSchemaService {

    private static final String QUERY = "query: {}";
    private static final String SUCCESSFULLY_EXECUTED = "Successfully executed ";
    private static final String FAILED_TO_EXECUTE = "Failed to execute ";
    private static final String FAILED_DUE_TO = " due to: {}";

    private static final String SUCCESSFULLY_EXECUTED_QUERY = SUCCESSFULLY_EXECUTED + QUERY;
    private static final String FAILED_TO_EXECUTE_QUERY = FAILED_TO_EXECUTE + QUERY + FAILED_DUE_TO;

    @Value("${sql.timescale.chunk_time_interval:86400000}")
    private long chunkTimeInterval;

    public TimescaleTsDatabaseSchemaService() {
        super("schema-timescale.sql", "schema-timescale-idx.sql");
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        super.createDatabaseSchema();
        executeQuery("SELECT create_hypertable('tenant_ts_kv', 'ts', chunk_time_interval => " + chunkTimeInterval + ", if_not_exists => true);");
    }

    private void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info(SUCCESSFULLY_EXECUTED_QUERY, query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.info(FAILED_TO_EXECUTE_QUERY, query, e.getMessage());
        }
    }


}