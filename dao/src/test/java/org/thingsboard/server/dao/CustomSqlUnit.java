/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;


/**
 * Created by Valerii Sosliuk on 6/24/2017.
 */
@Slf4j
public class CustomSqlUnit extends ExternalResource {

    private final List<String> sqlFiles;
    private final String dropAllTablesSqlFile;
    private final String dbUrl;
    private final String dbUserName;
    private final String dbPassword;

    public CustomSqlUnit(List<String> sqlFiles, String dropAllTablesSqlFile, String configurationFileName) {
        this.sqlFiles = sqlFiles;
        this.dropAllTablesSqlFile = dropAllTablesSqlFile;
        final Properties properties = new Properties();
        try (final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(configurationFileName)) {
            properties.load(stream);
            this.dbUrl = properties.getProperty("spring.datasource.url");
            this.dbUserName = properties.getProperty("spring.datasource.username");
            this.dbPassword = properties.getProperty("spring.datasource.password");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void before() {
        cleanUpDb();

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
            for (String sqlFile : sqlFiles) {
                URL sqlFileUrl = Resources.getResource(sqlFile);
                String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to start embedded hsqldb. Reason: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void after() {
        cleanUpDb();
    }

    private void cleanUpDb() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
            URL dropAllTableSqlFileUrl = Resources.getResource(dropAllTablesSqlFile);
            String dropAllTablesSql = Resources.toString(dropAllTableSqlFileUrl, Charsets.UTF_8);
            conn.createStatement().execute(dropAllTablesSql);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to clean up embedded hsqldb. Reason: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
