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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.PsqlTsDao;

import java.sql.Connection;

@PsqlTsDao
@Service
@Slf4j
public class PsqlTimeseriesCleanUpService extends AbstractTimeseriesCleanUpService {

    @Value("${sql.postgres.ts_key_value_partitioning}")
    private String partitionType;

    @Override
    protected void doCleanUp(Connection connection) {
            long totalPartitionsRemoved = executeQuery(connection, "call drop_partitions_by_max_ttl('" + partitionType + "'," + systemTtl + ", 0);");
            log.info("Total partitions removed by TTL: [{}]", totalPartitionsRemoved);
            long totalEntitiesTelemetryRemoved = executeQuery(connection, "call cleanup_timeseries_by_ttl('" + ModelConstants.NULL_UUID_STR + "'," + systemTtl + ", 0);");
            log.info("Total telemetry removed stats by TTL for entities: [{}]", totalEntitiesTelemetryRemoved);
    }
}