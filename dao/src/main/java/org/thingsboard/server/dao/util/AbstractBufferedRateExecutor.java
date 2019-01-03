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
package org.thingsboard.server.dao.util;

import com.datastax.driver.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 24.10.18.
 */
@Slf4j
public abstract class AbstractBufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture<V>, V> implements BufferedRateExecutor<T, F> {

    private final long maxWaitTime;
    private final long pollMs;
    private final BlockingQueue<AsyncTaskContext<T, V>> queue;
    private final ExecutorService dispatcherExecutor;
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final int concurrencyLimit;
    private final boolean perTenantLimitsEnabled;
    private final String perTenantLimitsConfiguration;
    private final ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    protected final ConcurrentMap<TenantId, AtomicInteger> rateLimitedTenants = new ConcurrentHashMap<>();

    protected final AtomicInteger concurrencyLevel = new AtomicInteger();
    protected final AtomicInteger totalAdded = new AtomicInteger();
    protected final AtomicInteger totalLaunched = new AtomicInteger();
    protected final AtomicInteger totalReleased = new AtomicInteger();
    protected final AtomicInteger totalFailed = new AtomicInteger();
    protected final AtomicInteger totalExpired = new AtomicInteger();
    protected final AtomicInteger totalRejected = new AtomicInteger();
    protected final AtomicInteger totalRateLimited = new AtomicInteger();

    public AbstractBufferedRateExecutor(int queueLimit, int concurrencyLimit, long maxWaitTime, int dispatcherThreads, int callbackThreads, long pollMs,
                                        boolean perTenantLimitsEnabled, String perTenantLimitsConfiguration) {
        this.maxWaitTime = maxWaitTime;
        this.pollMs = pollMs;
        this.concurrencyLimit = concurrencyLimit;
        this.queue = new LinkedBlockingDeque<>(queueLimit);
        this.dispatcherExecutor = Executors.newFixedThreadPool(dispatcherThreads);
        this.callbackExecutor = Executors.newWorkStealingPool(callbackThreads);
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        this.perTenantLimitsEnabled = perTenantLimitsEnabled;
        this.perTenantLimitsConfiguration = perTenantLimitsConfiguration;
        for (int i = 0; i < dispatcherThreads; i++) {
            dispatcherExecutor.submit(this::dispatch);
        }
    }

    @Override
    public F submit(T task) {
        SettableFuture<V> settableFuture = create();
        F result = wrap(task, settableFuture);
        boolean perTenantLimitReached = false;
        if (perTenantLimitsEnabled) {
            if (task.getTenantId() == null) {
                log.info("Invalid task received: {}", task);
            } else if (!task.getTenantId().isNullUid()) {
                TbRateLimits rateLimits = perTenantLimits.computeIfAbsent(task.getTenantId(), id -> new TbRateLimits(perTenantLimitsConfiguration));
                if (!rateLimits.tryConsume()) {
                    rateLimitedTenants.computeIfAbsent(task.getTenantId(), tId -> new AtomicInteger(0)).incrementAndGet();
                    totalRateLimited.incrementAndGet();
                    settableFuture.setException(new TenantRateLimitException());
                    perTenantLimitReached = true;
                }
            }
        }
        if (!perTenantLimitReached) {
            try {
                totalAdded.incrementAndGet();
                queue.add(new AsyncTaskContext<>(UUID.randomUUID(), task, settableFuture, System.currentTimeMillis()));
            } catch (IllegalStateException e) {
                totalRejected.incrementAndGet();
                settableFuture.setException(e);
            }
        }
        return result;
    }

    public void stop() {
        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    protected abstract SettableFuture<V> create();

    protected abstract F wrap(T task, SettableFuture<V> future);

    protected abstract ListenableFuture<V> execute(AsyncTaskContext<T, V> taskCtx);

    private void dispatch() {
        log.info("Buffered rate executor thread started");
        while (!Thread.interrupted()) {
            int curLvl = concurrencyLevel.get();
            AsyncTaskContext<T, V> taskCtx = null;
            try {
                if (curLvl <= concurrencyLimit) {
                    taskCtx = queue.take();
                    final AsyncTaskContext<T, V> finalTaskCtx = taskCtx;
                    logTask("Processing", finalTaskCtx);
                    concurrencyLevel.incrementAndGet();
                    long timeout = finalTaskCtx.getCreateTime() + maxWaitTime - System.currentTimeMillis();
                    if (timeout > 0) {
                        totalLaunched.incrementAndGet();
                        ListenableFuture<V> result = execute(finalTaskCtx);
                        result = Futures.withTimeout(result, timeout, TimeUnit.MILLISECONDS, timeoutExecutor);
                        Futures.addCallback(result, new FutureCallback<V>() {
                            @Override
                            public void onSuccess(@Nullable V result) {
                                logTask("Releasing", finalTaskCtx);
                                totalReleased.incrementAndGet();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().set(result);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (t instanceof TimeoutException) {
                                    logTask("Expired During Execution", finalTaskCtx);
                                } else {
                                    logTask("Failed", finalTaskCtx);
                                }
                                totalFailed.incrementAndGet();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().setException(t);
                                log.debug("[{}] Failed to execute task: {}", finalTaskCtx.getId(), finalTaskCtx.getTask(), t);
                            }
                        }, callbackExecutor);
                    } else {
                        logTask("Expired Before Execution", finalTaskCtx);
                        totalExpired.incrementAndGet();
                        concurrencyLevel.decrementAndGet();
                        taskCtx.getFuture().setException(new TimeoutException());
                    }
                } else {
                    Thread.sleep(pollMs);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (taskCtx != null) {
                    log.debug("[{}] Failed to execute task: {}", taskCtx.getId(), taskCtx, e);
                    totalFailed.incrementAndGet();
                    concurrencyLevel.decrementAndGet();
                } else {
                    log.debug("Failed to queue task:", e);
                }
            }
        }
        log.info("Buffered rate executor thread stopped");
    }

    private void logTask(String action, AsyncTaskContext<T, V> taskCtx) {
        if (log.isTraceEnabled()) {
            if (taskCtx.getTask() instanceof CassandraStatementTask) {
                CassandraStatementTask cassStmtTask = (CassandraStatementTask) taskCtx.getTask();
                if (cassStmtTask.getStatement() instanceof BoundStatement) {
                    BoundStatement stmt = (BoundStatement) cassStmtTask.getStatement();
                    String query = toStringWithValues(stmt, ProtocolVersion.V5);
                    log.trace("[{}] {} task: {}, BoundStatement query: {}", taskCtx.getId(), action, taskCtx, query);
                }
            } else {
                log.trace("[{}] {} task: {}", taskCtx.getId(), action, taskCtx);
            }
        } else {
            log.debug("[{}] {} task", taskCtx.getId(), action);
        }
    }

    private static String toStringWithValues(BoundStatement boundStatement, ProtocolVersion protocolVersion) {
        CodecRegistry codecRegistry = boundStatement.preparedStatement().getCodecRegistry();
        PreparedStatement preparedStatement = boundStatement.preparedStatement();
        String query = preparedStatement.getQueryString();
        ColumnDefinitions defs = preparedStatement.getVariables();
        int index = 0;
        for (ColumnDefinitions.Definition def : defs) {
            DataType type = def.getType();
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            if (boundStatement.getBytesUnsafe(index) != null) {
                Object value = codec.deserialize(boundStatement.getBytesUnsafe(index), protocolVersion);
                query = query.replaceFirst("\\?", codec.format(value));
            }
            index++;
        }
        return query;
    }

    protected int getQueueSize() {
        return queue.size();
    }
}
