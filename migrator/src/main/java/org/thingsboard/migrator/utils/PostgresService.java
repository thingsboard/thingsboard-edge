/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.migrator.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresService {

    private final JdbcTemplate jdbcTemplate;

    public Blob getBlob(long oid) {
        byte[] data = jdbcTemplate.execute((ConnectionCallback<byte[]>) connection -> {
            connection.setAutoCommit(false);
            try {
                LargeObjectManager loManager = getLoManager(connection);
                try (LargeObject lo = loManager.open(oid, LargeObjectManager.READ)) {
                    return IOUtils.toByteArray(lo.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                connection.setAutoCommit(true);
            }
        });
        return new Blob(data);
    }

    public Long saveBlob(Blob blob) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            LargeObjectManager largeObjectManager = getLoManager(connection);
            long oid = largeObjectManager.createLO();
            try (LargeObject lo = largeObjectManager.open(oid, LargeObjectManager.WRITE)) {
                lo.write(blob.data());
            }
            log.info("Created blob {} (data size: {})", oid, blob.data().length);
            return oid;
        });
    }

    private LargeObjectManager getLoManager(Connection connection) throws SQLException {
        return connection.unwrap(PgConnection.class).getLargeObjectAPI();
    }

    public record Blob(byte[] data) {}

}
