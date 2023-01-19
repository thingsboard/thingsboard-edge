/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
        switch (queueUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                queueCreationLock.lock();
                try {
                    Queue queue = queueService.findQueueById(tenantId, queueId);
                    boolean create = false;
                    if (queue == null) {
                        queue = new Queue();
                        queue.setId(queueId);
                        queue.setCreatedTime(Uuids.unixTimestamp(queueId.getId()));
                        TenantId queueTenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
                        queue.setTenantId(queueTenantId);
                        create = true;
                    }
                    queue.setName(queueUpdateMsg.getName());
                    queue.setTopic(queueUpdateMsg.getTopic());
                    queue.setPollInterval(queueUpdateMsg.getPollInterval());
                    queue.setPartitions(queueUpdateMsg.getPartitions());
                    queue.setConsumerPerPartition(queueUpdateMsg.getConsumerPerPartition());
                    queue.setPackProcessingTimeout(queueUpdateMsg.getPackProcessingTimeout());
                    SubmitStrategy submitStrategy = new SubmitStrategy();
                    submitStrategy.setType(SubmitStrategyType.valueOf(queueUpdateMsg.getSubmitStrategy().getType()));
                    submitStrategy.setBatchSize(queueUpdateMsg.getSubmitStrategy().getBatchSize());
                    queue.setSubmitStrategy(submitStrategy);
                    ProcessingStrategy processingStrategy = new ProcessingStrategy();
                    processingStrategy.setType(ProcessingStrategyType.valueOf(queueUpdateMsg.getProcessingStrategy().getType()));
                    processingStrategy.setRetries(queueUpdateMsg.getProcessingStrategy().getRetries());
                    processingStrategy.setPauseBetweenRetries(queueUpdateMsg.getProcessingStrategy().getPauseBetweenRetries());
                    processingStrategy.setMaxPauseBetweenRetries(queueUpdateMsg.getProcessingStrategy().getMaxPauseBetweenRetries());
                    processingStrategy.setFailurePercentage(queueUpdateMsg.getProcessingStrategy().getFailurePercentage());
                    queue.setProcessingStrategy(processingStrategy);
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
        return Futures.immediateFuture(null);
    }
}
