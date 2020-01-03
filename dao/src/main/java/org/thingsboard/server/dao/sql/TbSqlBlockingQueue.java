/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class TbSqlBlockingQueue<E> implements TbSqlQueue<E> {

    private final BlockingQueue<TbSqlQueueElement<E>> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger addedCount = new AtomicInteger();
    private final AtomicInteger savedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final TbSqlBlockingQueueParams params;

    private ExecutorService executor;
    private ScheduledLogExecutorComponent logExecutor;

    public TbSqlBlockingQueue(TbSqlBlockingQueueParams params) {
        this.params = params;
    }

    @Override
    public void init(ScheduledLogExecutorComponent logExecutor, Consumer<List<E>> saveFunction) {
        this.logExecutor = logExecutor;
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("sql-queue-" + params.getLogName().toLowerCase()));
        executor.submit(() -> {
            String logName = params.getLogName();
            int batchSize = params.getBatchSize();
            long maxDelay = params.getMaxDelay();
            List<TbSqlQueueElement<E>> entities = new ArrayList<>(batchSize);
            while (!Thread.interrupted()) {
                try {
                    long currentTs = System.currentTimeMillis();
                    TbSqlQueueElement<E> attr = queue.poll(maxDelay, TimeUnit.MILLISECONDS);
                    if (attr == null) {
                        continue;
                    } else {
                        entities.add(attr);
                    }
                    queue.drainTo(entities, batchSize - 1);
                    boolean fullPack = entities.size() == batchSize;
                    log.debug("[{}] Going to save {} entities", logName, entities.size());
                    if (log.isTraceEnabled()) {
                        for (TbSqlQueueElement<E> e : entities) {
                            log.debug("[{}] Element: {}", logName, e.getEntity());
                        }
                    }
                    saveFunction.accept(entities.stream().map(TbSqlQueueElement::getEntity).collect(Collectors.toList()));
                    entities.forEach(v -> v.getFuture().set(null));
                    savedCount.addAndGet(entities.size());
                    if (!fullPack) {
                        long remainingDelay = maxDelay - (System.currentTimeMillis() - currentTs);
                        if (remainingDelay > 0) {
                            Thread.sleep(remainingDelay);
                        }
                    }
                } catch (Exception e) {
                    failedCount.addAndGet(entities.size());
                    entities.forEach(entityFutureWrapper -> entityFutureWrapper.getFuture().setException(e));
                    if (e instanceof InterruptedException) {
                        log.info("[{}] Queue polling was interrupted", logName);
                        break;
                    } else {
                        log.error("[{}] Failed to save {} entities", logName, entities.size(), e);
                    }
                } finally {
                    entities.clear();
                }
            }
        });

        logExecutor.scheduleAtFixedRate(() -> {
            if (queue.size() > 0 || addedCount.get() > 0 || savedCount.get() > 0 || failedCount.get() > 0) {
                log.info("[{}] queueSize [{}] totalAdded [{}] totalSaved [{}] totalFailed [{}]",
                        params.getLogName(), queue.size(), addedCount.getAndSet(0), savedCount.getAndSet(0), failedCount.getAndSet(0));
            }
        }, params.getStatsPrintIntervalMs(), params.getStatsPrintIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> add(E element) {
        SettableFuture<Void> future = SettableFuture.create();
        queue.add(new TbSqlQueueElement<>(future, element));
        addedCount.incrementAndGet();
        return future;
    }
}
