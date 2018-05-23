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
package org.thingsboard.server.dao.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.exception.BufferLimitException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@NoSqlDao
public class BufferedRateLimiter implements AsyncRateLimiter {

    private final ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private final int permitsLimit;
    private final int maxPermitWaitTime;
    private final AtomicInteger permits;
    private final BlockingQueue<LockedFuture> queue;

    private final AtomicInteger maxQueueSize = new AtomicInteger();
    private final AtomicInteger maxGrantedPermissions = new AtomicInteger();
    private final AtomicInteger totalGranted = new AtomicInteger();
    private final AtomicInteger totalReleased = new AtomicInteger();
    private final AtomicInteger totalRequested = new AtomicInteger();

    public BufferedRateLimiter(@Value("${cassandra.query.buffer_size}") int queueLimit,
                               @Value("${cassandra.query.concurrent_limit}") int permitsLimit,
                               @Value("${cassandra.query.permit_max_wait_time}") int maxPermitWaitTime) {
        this.permitsLimit = permitsLimit;
        this.maxPermitWaitTime = maxPermitWaitTime;
        this.permits = new AtomicInteger();
        this.queue = new LinkedBlockingQueue<>(queueLimit);
    }

    @Override
    public ListenableFuture<Void> acquireAsync() {
        totalRequested.incrementAndGet();
        if (queue.isEmpty()) {
            if (permits.incrementAndGet() <= permitsLimit) {
                if (permits.get() > maxGrantedPermissions.get()) {
                    maxGrantedPermissions.set(permits.get());
                }
                totalGranted.incrementAndGet();
                return Futures.immediateFuture(null);
            }
            permits.decrementAndGet();
        }

        return putInQueue();
    }

    @Override
    public void release() {
        permits.decrementAndGet();
        totalReleased.incrementAndGet();
        reprocessQueue();
    }

    private void reprocessQueue() {
        while (permits.get() < permitsLimit) {
            if (permits.incrementAndGet() <= permitsLimit) {
                if (permits.get() > maxGrantedPermissions.get()) {
                    maxGrantedPermissions.set(permits.get());
                }
                LockedFuture lockedFuture = queue.poll();
                if (lockedFuture != null) {
                    totalGranted.incrementAndGet();
                    lockedFuture.latch.countDown();
                } else {
                    permits.decrementAndGet();
                    break;
                }
            } else {
                permits.decrementAndGet();
            }
        }
    }

    private LockedFuture createLockedFuture() {
        CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<Void> future = pool.submit(() -> {
            latch.await();
            return null;
        });
        return new LockedFuture(latch, future, System.currentTimeMillis());
    }

    private ListenableFuture<Void> putInQueue() {

        int size = queue.size();
        if (size > maxQueueSize.get()) {
            maxQueueSize.set(size);
        }

        if (queue.remainingCapacity() > 0) {
            try {
                LockedFuture lockedFuture = createLockedFuture();
                if (!queue.offer(lockedFuture, 1, TimeUnit.SECONDS)) {
                    lockedFuture.cancelFuture();
                    return Futures.immediateFailedFuture(new BufferLimitException());
                }
                if(permits.get() < permitsLimit) {
                    reprocessQueue();
                }
                if(permits.get() < permitsLimit) {
                    reprocessQueue();
                }
                return lockedFuture.future;
            } catch (InterruptedException e) {
                return Futures.immediateFailedFuture(new BufferLimitException());
            }
        }
        return Futures.immediateFailedFuture(new BufferLimitException());
    }

    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    public void printStats() {
        int expiredCount = 0;
        for (LockedFuture lockedFuture : queue) {
            if (lockedFuture.isExpired()) {
                lockedFuture.cancelFuture();
                expiredCount++;
            }
        }
        log.info("Permits maxBuffer [{}] maxPermits [{}] expired [{}] currPermits [{}] currBuffer [{}] " +
                        "totalPermits [{}] totalRequests [{}] totalReleased [{}]",
                maxQueueSize.getAndSet(0), maxGrantedPermissions.getAndSet(0), expiredCount,
                permits.get(), queue.size(),
                totalGranted.getAndSet(0), totalRequested.getAndSet(0), totalReleased.getAndSet(0));
    }

    private class LockedFuture {
        final CountDownLatch latch;
        final ListenableFuture<Void> future;
        final long createTime;

        public LockedFuture(CountDownLatch latch, ListenableFuture<Void> future, long createTime) {
            this.latch = latch;
            this.future = future;
            this.createTime = createTime;
        }

        void cancelFuture() {
            future.cancel(false);
            latch.countDown();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - createTime) > maxPermitWaitTime;
        }

    }


}
