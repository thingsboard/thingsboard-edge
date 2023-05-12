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
package org.thingsboard.server.dao.service.install.sql;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DaoSqlTest
public class EntitiesSchemaSqlTest extends AbstractServiceTest {

    @Value("${classpath:sql/schema-entities.sql}")
    private Path installEntitiesPath;
    @Value("${classpath:sql/schema-views-and-functions.sql}")
    private Path installViewsPath;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testRepeatedInstall() throws IOException {
        String entitiesScript = Files.readString(installEntitiesPath);
        String viewsScript = Files.readString(installViewsPath);
        try {
            for (int i = 1; i <= 2; i++) {
                jdbcTemplate.execute(entitiesScript);
                jdbcTemplate.execute(viewsScript);
            }
        } catch (Exception e) {
            Assertions.fail("Failed to execute reinstall", e);
        }
    }

    @Test
    public void testRepeatedInstall_badScript() {
        String illegalInstallScript = "CREATE TABLE IF NOT EXISTS qwerty ();\n" +
                "ALTER TABLE qwerty ADD COLUMN first VARCHAR(10);";

        assertDoesNotThrow(() -> {
            jdbcTemplate.execute(illegalInstallScript);
        });

        try {
            assertThatThrownBy(() -> {
                jdbcTemplate.execute(illegalInstallScript);
            }).getCause().hasMessageContaining("column").hasMessageContaining("already exists");
        } finally {
            jdbcTemplate.execute("DROP TABLE qwerty;");
        }
    }

}
