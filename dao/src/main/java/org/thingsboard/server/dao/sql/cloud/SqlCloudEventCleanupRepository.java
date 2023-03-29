/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.cloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Repository
public class SqlCloudEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements CloudEventCleanupRepository {

    @Override
    public void cleanupEvents(long eventsTtl) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_cloud_events_by_ttl(?,?)")) {
            stmt.setLong(1, eventsTtl);
            stmt.setLong(2, 0);
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()){
                resultSet.next();
                log.info("Total cloud events removed by TTL: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during events TTL task execution ", e);
        }
    }

}