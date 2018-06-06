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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.transport.quota.tenant.TenantQuotaService;
import org.thingsboard.server.dao.queue.MsgQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class DefaultMsgQueueService implements MsgQueueService {

    @Value("${actors.rule.queue.max_size}")
    private long queueMaxSize;

    @Value("${actors.rule.queue.cleanup_period}")
    private long queueCleanUpPeriod;

    @Autowired
    private MsgQueue msgQueue;

    @Autowired
    private TenantQuotaService quotaService;

    private ScheduledExecutorService cleanupExecutor;

    private Map<TenantId, AtomicLong> pendingCountPerTenant = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (queueCleanUpPeriod > 0) {
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
            cleanupExecutor.scheduleAtFixedRate(() -> cleanup(),
                    queueCleanUpPeriod, queueCleanUpPeriod, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void stop() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> put(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        if(quotaService.isQuotaExceeded(tenantId.getId().toString())) {
            log.warn("Tenant TbMsg Quota exceeded for [{}:{}] . Reject", tenantId.getId());
            return Futures.immediateFailedFuture(new RuntimeException("Tenant TbMsg Quota exceeded"));
        }

        AtomicLong pendingMsgCount = pendingCountPerTenant.computeIfAbsent(tenantId, key -> new AtomicLong());
        if (pendingMsgCount.incrementAndGet() < queueMaxSize) {
            return msgQueue.put(tenantId, msg, nodeId, clusterPartition);
        } else {
            pendingMsgCount.decrementAndGet();
            return Futures.immediateFailedFuture(new RuntimeException("Message queue is full!"));
        }
    }

    @Override
    public ListenableFuture<Void> ack(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        ListenableFuture<Void> result = msgQueue.ack(tenantId, msg, nodeId, clusterPartition);
        AtomicLong pendingMsgCount = pendingCountPerTenant.computeIfAbsent(tenantId, key -> new AtomicLong());
        pendingMsgCount.decrementAndGet();
        return result;
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(TenantId tenantId, UUID nodeId, long clusterPartition) {
        return msgQueue.findUnprocessed(tenantId, nodeId, clusterPartition);
    }

    private void cleanup() {
        pendingCountPerTenant.forEach((tenantId, pendingMsgCount) -> {
            pendingMsgCount.set(0);
            msgQueue.cleanUp(tenantId);
        });
    }

}
