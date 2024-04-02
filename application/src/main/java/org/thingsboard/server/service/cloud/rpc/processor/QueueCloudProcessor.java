/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class QueueCloudProcessor extends BaseEdgeProcessor {

    private final Lock queueCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processQueueMsgFromCloud(TenantId tenantId, QueueUpdateMsg queueUpdateMsg) {
        QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getIdMSB(), queueUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (queueUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    queueCreationLock.lock();
                    try {
                        Queue queue = JacksonUtil.fromString(queueUpdateMsg.getEntity(), Queue.class, true);
                        if (queue == null) {
                            throw new RuntimeException("[{" + tenantId + "}] queueUpdateMsg {" + queueUpdateMsg + "} cannot be converted to queue");
                        }
                        Queue queueById = queueService.findQueueById(tenantId, queueId);
                        boolean create = queueById == null;
                        queueService.saveQueue(queue, false);
                        tbQueueService.saveQueue(queue, create);
                    } finally {
                        queueCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Queue queue = queueService.findQueueById(tenantId, queueId);
                    if (queue != null) {
                        tbQueueService.deleteQueue(tenantId, queueId);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(queueUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

}
