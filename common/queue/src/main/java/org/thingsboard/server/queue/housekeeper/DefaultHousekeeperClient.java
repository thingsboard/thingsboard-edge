/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.housekeeper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

@Service
@Slf4j
public class DefaultHousekeeperClient implements HousekeeperClient {

    private final HousekeeperConfig config;
    private final TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> producer;
    private final TopicPartitionInfo submitTpi;
    private final TbQueueCallback submitCallback;

    public DefaultHousekeeperClient(HousekeeperConfig config,
                                    TbQueueProducerProvider producerProvider) {
        this.config = config;
        this.producer = producerProvider.getHousekeeperMsgProducer();
        this.submitTpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        this.submitCallback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("Submitted Housekeeper task");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to submit Housekeeper task", t);
            }
        };
    }

    @Override
    public void submitTask(HousekeeperTask task) {
        HousekeeperTaskType taskType = task.getTaskType();
        if (config.getDisabledTaskTypes().contains(taskType)) {
            log.trace("Task type {} is disabled, ignoring {}", taskType, task);
            return;
        }

        log.debug("[{}][{}][{}] Submitting task: {}", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task);
        /*
         * using msg key as entity id so that msgs related to certain entity are pushed to same partition,
         * e.g. on tenant deletion (entity id is tenant id), we need to clean up tenant entities in certain order
         * */
        try {
            producer.send(submitTpi, new TbProtoQueueMsg<>(task.getEntityId().getId(), ToHousekeeperServiceMsg.newBuilder()
                    .setTask(TransportProtos.HousekeeperTaskProto.newBuilder()
                            .setValue(JacksonUtil.toString(task))
                            .setTs(task.getTs())
                            .setAttempt(0)
                            .build())
                    .build()), submitCallback);
        } catch (Throwable t) {
            log.error("Failed to submit Housekeeper task {}", task, t);
        }
    }

}
