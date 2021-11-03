/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.HsqlDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Slf4j
@HsqlDao
@Repository
public class HsqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    @Override
    public void cleanupEvents(long otherEventsTtl, long debugEventsTtl) {
        long otherExpirationTime = System.currentTimeMillis() - otherEventsTtl * 1000;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("DELETE FROM event WHERE ts < ? AND event_type != 'DEBUG_RULE_NODE' AND event_type != 'DEBUG_RULE_CHAIN' " +
                     "AND event_type != 'DEBUG_CONVERTER' AND event_type != 'DEBUG_INTEGRATION'")) {
            stmt.setLong(1, otherExpirationTime);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQLException occurred during events TTL task execution ", e);
        }
        long debugExpirationTime = System.currentTimeMillis() - debugEventsTtl * 1000;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("DELETE FROM event WHERE ts < ? AND (event_type = 'DEBUG_RULE_NODE' OR event_type = 'DEBUG_RULE_CHAIN' " +
                     "OR event_type = 'DEBUG_CONVERTER' OR event_type = 'DEBUG_INTEGRATION')")) {
            stmt.setLong(1, debugExpirationTime);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
        } catch (SQLException e) {
            log.error("SQLException occurred during events TTL task execution ", e);
        }
    }
}
