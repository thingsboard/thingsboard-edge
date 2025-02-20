/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cf.ctx.state;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.common.consumer.QueueStateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.service.cf.AbstractCalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.bytesToString;
import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.bytesToUuid;
import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.stringToBytes;
import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.uuidToBytes;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCalculatedFieldStateService extends AbstractCalculatedFieldStateService {

    private final TbRuleEngineQueueFactory queueFactory;
    private final PartitionService partitionService;

    @Value("${queue.calculated_fields.poll_interval:25}")
    private long pollInterval;

    private PartitionedQueueConsumerManager<TbProtoQueueMsg<CalculatedFieldStateProto>> stateConsumer;
    private TbKafkaProducerTemplate<TbProtoQueueMsg<CalculatedFieldStateProto>> stateProducer;
    private QueueStateService<TbProtoQueueMsg<ToCalculatedFieldMsg>, TbProtoQueueMsg<CalculatedFieldStateProto>> queueStateService;

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void init(PartitionedQueueConsumerManager<TbProtoQueueMsg<ToCalculatedFieldMsg>> eventConsumer) {
        super.init(eventConsumer);
        this.stateConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<CalculatedFieldStateProto>>create()
                .queueKey(QueueKey.CF_STATES)
                .topic(partitionService.getTopic(QueueKey.CF_STATES))
                .pollInterval(pollInterval)
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<CalculatedFieldStateProto> msg : msgs) {
                        try {
                            if (msg.getValue() != null) {
                                processRestoredState(msg.getValue());
                            } else {
                                processRestoredState(getStateId(msg.getHeaders()), null);
                            }
                        } catch (Throwable t) {
                            log.error("Failed to process state message: {}", msg, t);
                        }

                        int processedMsgCount = counter.incrementAndGet();
                        if (processedMsgCount % 10000 == 0) {
                            log.info("Processed {} calculated field state msgs", processedMsgCount);
                        }
                    }
                })
                .consumerCreator((config, partitionId) -> queueFactory.createCalculatedFieldStateConsumer())
                .consumerExecutor(eventConsumer.getConsumerExecutor())
                .scheduler(eventConsumer.getScheduler())
                .taskExecutor(eventConsumer.getTaskExecutor())
                .build();
        this.stateProducer = (TbKafkaProducerTemplate<TbProtoQueueMsg<CalculatedFieldStateProto>>) queueFactory.createCalculatedFieldStateProducer();
        this.queueStateService = new QueueStateService<>();
        this.queueStateService.init(stateConsumer, super.eventConsumer);
    }

    @Override
    protected void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(QueueKey.CF_STATES, stateId.entityId());
        TbProtoQueueMsg<CalculatedFieldStateProto> msg = new TbProtoQueueMsg<>(stateId.entityId().getId(), stateMsgProto);
        if (stateMsgProto == null) {
            putStateId(msg.getHeaders(), stateId);
        }
        stateProducer.send(tpi, stateId.toKey(), msg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    @Override
    protected void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        doPersist(stateId, null, callback);
    }

    @Override
    public void restore(Set<TopicPartitionInfo> partitions) {
        queueStateService.update(partitions);
    }

    private void putStateId(TbQueueMsgHeaders headers, CalculatedFieldEntityCtxId stateId) {
        headers.put("tenantId", uuidToBytes(stateId.tenantId().getId()));
        headers.put("cfId", uuidToBytes(stateId.cfId().getId()));
        headers.put("entityId", uuidToBytes(stateId.entityId().getId()));
        headers.put("entityType", stringToBytes(stateId.entityId().getEntityType().name()));
    }

    private CalculatedFieldEntityCtxId getStateId(TbQueueMsgHeaders headers) {
        TenantId tenantId = TenantId.fromUUID(bytesToUuid(headers.get("tenantId")));
        CalculatedFieldId cfId = new CalculatedFieldId(bytesToUuid(headers.get("cfId")));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(bytesToString(headers.get("entityType")), bytesToUuid(headers.get("entityId")));
        return new CalculatedFieldEntityCtxId(tenantId, cfId, entityId);
    }

    @Override
    public void stop() {
        stateConsumer.stop();
        stateConsumer.awaitStop();
        stateProducer.stop();
    }

}
