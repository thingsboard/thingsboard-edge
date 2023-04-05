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
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public abstract class AbstractSqlTimeseriesDao extends BaseAbstractSqlTimeseriesDao implements AggregationTimeseriesDao {

    protected static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Value("${sql.ts.batch_size:1000}")
    protected int tsBatchSize;

    @Value("${sql.ts.batch_max_delay:100}")
    protected long tsMaxDelay;

    @Value("${sql.ts.stats_print_interval_ms:1000}")
    protected long tsStatsPrintIntervalMs;

    @Value("${sql.ts.batch_threads:4}")
    protected int tsBatchThreads;

    @Value("${sql.timescale.batch_threads:4}")
    protected int timescaleBatchThreads;

    @Value("${sql.batch_sort:true}")
    protected boolean batchSortEnabled;

    @Value("${sql.ttl.ts.ts_key_value_ttl:0}")
    private long systemTtl;

    public void cleanup(long systemTtl) {
        log.info("Going to cleanup old timeseries data using ttl: {}s", systemTtl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_timeseries_by_ttl(?,?,?)")) {
            stmt.setObject(1, ModelConstants.NULL_UUID);
            stmt.setLong(2, systemTtl);
            stmt.setLong(3, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total telemetry removed stats by TTL for entities: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during timeseries TTL task execution ", e);
        }
    }

    protected ListenableFuture<List<ReadTsKvQueryResult>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<ReadTsKvQueryResult>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<>() {
            @Nullable
            @Override
            public List<ReadTsKvQueryResult> apply(@Nullable List<ReadTsKvQueryResult> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
            }
        }, service);
    }

    protected long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    protected int getDataPointDays(TsKvEntry tsKvEntry, long ttl) {
        return tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
    }
}
