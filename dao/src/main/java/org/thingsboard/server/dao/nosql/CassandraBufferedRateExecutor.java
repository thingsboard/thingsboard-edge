/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.nosql;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.util.AbstractBufferedRateExecutor;
import org.thingsboard.server.dao.util.AsyncTaskContext;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 24.10.18.
 */
@Component
@Slf4j
@NoSqlAnyDao
public class CassandraBufferedRateExecutor extends AbstractBufferedRateExecutor<CassandraStatementTask, ResultSetFuture, ResultSet> {

    @Autowired
    private EntityService entityService;
    private Map<TenantId, String> tenantNamesCache = new HashMap<>();

    private boolean printTenantNames;

    public CassandraBufferedRateExecutor(
            @Value("${cassandra.query.buffer_size}") int queueLimit,
            @Value("${cassandra.query.concurrent_limit}") int concurrencyLimit,
            @Value("${cassandra.query.permit_max_wait_time}") long maxWaitTime,
            @Value("${cassandra.query.dispatcher_threads:2}") int dispatcherThreads,
            @Value("${cassandra.query.callback_threads:2}") int callbackThreads,
            @Value("${cassandra.query.poll_ms:50}") long pollMs,
            @Value("${cassandra.query.tenant_rate_limits.enabled}") boolean tenantRateLimitsEnabled,
            @Value("${cassandra.query.tenant_rate_limits.configuration}") String tenantRateLimitsConfiguration,
            @Value("${cassandra.query.tenant_rate_limits.print_tenant_names}") boolean printTenantNames) {
        super(queueLimit, concurrencyLimit, maxWaitTime, dispatcherThreads, callbackThreads, pollMs, tenantRateLimitsEnabled, tenantRateLimitsConfiguration);
        this.printTenantNames = printTenantNames;
    }

    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    public void printStats() {
        log.info("Permits queueSize [{}] totalAdded [{}] totalLaunched [{}] totalReleased [{}] totalFailed [{}] totalExpired [{}] totalRejected [{}] " +
                        "totalRateLimited [{}] totalRateLimitedTenants [{}] currBuffer [{}] ",
                getQueueSize(),
                totalAdded.getAndSet(0), totalLaunched.getAndSet(0), totalReleased.getAndSet(0),
                totalFailed.getAndSet(0), totalExpired.getAndSet(0), totalRejected.getAndSet(0),
                totalRateLimited.getAndSet(0), rateLimitedTenants.size(), concurrencyLevel.get());

        rateLimitedTenants.forEach(((tenantId, counter) -> {
            if (printTenantNames) {
                String name = tenantNamesCache.computeIfAbsent(tenantId, tId -> {
                    try {
                        return entityService.fetchEntityNameAsync(TenantId.SYS_TENANT_ID, tenantId).get();
                    } catch (Exception e) {
                        log.error("[{}] Failed to get tenant name", tenantId, e);
                        return "N/A";
                    }
                });
                log.info("[{}][{}] Rate limited requests: {}", tenantId, name, counter);
            } else {
                log.info("[{}] Rate limited requests: {}", tenantId, counter);
            }
        }));
        rateLimitedTenants.clear();
    }

    @PreDestroy
    public void stop() {
        super.stop();
    }

    @Override
    protected SettableFuture<ResultSet> create() {
        return SettableFuture.create();
    }

    @Override
    protected ResultSetFuture wrap(CassandraStatementTask task, SettableFuture<ResultSet> future) {
        return new TbResultSetFuture(future);
    }

    @Override
    protected ResultSetFuture execute(AsyncTaskContext<CassandraStatementTask, ResultSet> taskCtx) {
        CassandraStatementTask task = taskCtx.getTask();
        return task.getSession().executeAsync(task.getStatement());
    }

}
