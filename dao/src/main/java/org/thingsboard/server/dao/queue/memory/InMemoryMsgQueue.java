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
package org.thingsboard.server.dao.queue.memory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.MsgQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 27.04.18.
 */
@Component
@ConditionalOnProperty(prefix = "actors.rule.queue", value = "type", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class InMemoryMsgQueue implements MsgQueue {

    private ListeningExecutorService queueExecutor;
    private Map<TenantId, Map<InMemoryMsgKey, Map<UUID, TbMsg>>> data = new HashMap<>();

    @PostConstruct
    public void init() {
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> put(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        return queueExecutor.submit(() -> {
            data.computeIfAbsent(tenantId, key -> new HashMap<>()).
                    computeIfAbsent(new InMemoryMsgKey(nodeId, clusterPartition), key -> new HashMap<>()).put(msg.getId(), msg);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> ack(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        return queueExecutor.submit(() -> {
            Map<InMemoryMsgKey, Map<UUID, TbMsg>> tenantMap = data.get(tenantId);
            if (tenantMap != null) {
                InMemoryMsgKey key = new InMemoryMsgKey(nodeId, clusterPartition);
                Map<UUID, TbMsg> map = tenantMap.get(key);
                if (map != null) {
                    map.remove(msg.getId());
                    if (map.isEmpty()) {
                        tenantMap.remove(key);
                    }
                }
                if (tenantMap.isEmpty()) {
                    data.remove(tenantId);
                }
            }
            return null;
        });
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(TenantId tenantId, UUID nodeId, long clusterPartition) {
        ListenableFuture<List<TbMsg>> list = queueExecutor.submit(() -> {
            Map<InMemoryMsgKey, Map<UUID, TbMsg>> tenantMap = data.get(tenantId);
            if (tenantMap != null) {
                InMemoryMsgKey key = new InMemoryMsgKey(nodeId, clusterPartition);
                Map<UUID, TbMsg> map = tenantMap.get(key);
                if (map != null) {
                    return new ArrayList<>(map.values());
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        });
        try {
            return list.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<Void> cleanUp(TenantId tenantId) {
        return queueExecutor.submit(() -> {
            data.remove(tenantId);
            return null;
        });
    }
}
