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
package org.thingsboard.server.edqs.processor;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;

@Slf4j
@Builder
@RequiredArgsConstructor
public class EdqsProducer {

    private final TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> producer;
    private final EdqsPartitionService partitionService;

    public void send(TenantId tenantId, ObjectType type, String key, ToEdqsMsg msg) {
        TopicPartitionInfo tpi = TopicPartitionInfo.builder()
                .topic(producer.getDefaultTopic())
                .partition(partitionService.resolvePartition(tenantId, key))
                .build();
        TbQueueCallback callback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("[{}][{}][{}] Published msg to {}: {}", tenantId, type, key, tpi, msg);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof RecordTooLargeException) {
                    if (!log.isDebugEnabled()) {
                        log.warn("[{}][{}][{}] Failed to publish msg to {}", tenantId, type, key, tpi, t); // not logging the whole message
                        return;
                    }
                }
                log.warn("[{}][{}][{}] Failed to publish msg to {}: {}", tenantId, type, key, tpi, msg, t);
            }
        };
        if (producer instanceof TbKafkaProducerTemplate<TbProtoQueueMsg<ToEdqsMsg>> kafkaProducer) {
            kafkaProducer.send(tpi, key, new TbProtoQueueMsg<>(null, msg), callback); // specifying custom key for compaction
        } else {
            producer.send(tpi, new TbProtoQueueMsg<>(null, msg), callback);
        }
    }

    public void stop() {
        producer.stop();
    }

}
