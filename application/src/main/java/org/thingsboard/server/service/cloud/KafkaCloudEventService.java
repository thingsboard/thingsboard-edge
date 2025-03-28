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
package org.thingsboard.server.service.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCloudEventProvider;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.gen.transport.TransportProtos.ToCloudEventMsg;

@Slf4j
@Service
@Primary
@AllArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaCloudEventService implements CloudEventService {

    private final TbCloudEventProvider tbCloudEventProvider;
    private final DataValidator<CloudEvent> cloudEventValidator;

    @Override
    public void saveCloudEvent(TenantId tenantId, CloudEventType cloudEventType,
                               EdgeEventActionType cloudEventAction, EntityId entityId,
                               JsonNode entityBody, EntityGroupId entityGroupId) throws ExecutionException, InterruptedException {

        saveCloudEventAsync(tenantId, cloudEventType, cloudEventAction, entityId, entityBody, entityGroupId).get();
    }

    @Override
    public ListenableFuture<Void> saveCloudEventAsync(TenantId tenantId, CloudEventType cloudEventType,
                                                      EdgeEventActionType cloudEventAction, EntityId entityId,
                                                      JsonNode entityBody, EntityGroupId entityGroupId) {
        CloudEvent cloudEvent = new CloudEvent(
                tenantId,
                cloudEventAction,
                entityId != null ? entityId.getId() : null,
                cloudEventType,
                entityBody,
                entityGroupId != null ? entityGroupId.getId() : null
        );
        return saveAsync(cloudEvent);
    }

    @Override
    public ListenableFuture<Void> saveAsync(CloudEvent cloudEvent) {
        return saveCloudEventToTopic(cloudEvent, tbCloudEventProvider.getCloudEventMsgProducer());
    }

    @Override
    public ListenableFuture<Void> saveTsKvAsync(CloudEvent cloudEvent) {
        return saveCloudEventToTopic(cloudEvent, tbCloudEventProvider.getCloudEventTSMsgProducer());
    }

    private ListenableFuture<Void> saveCloudEventToTopic(CloudEvent cloudEvent, TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer) {
        cloudEventValidator.validate(cloudEvent, CloudEvent::getTenantId);
        log.trace("Save cloud event {}", cloudEvent);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        saveCloudEventToTopic(cloudEvent, producer, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to send cloud event", t);
                futureToSet.setException(t);
            }
        });
        return futureToSet;
    }

    private void saveCloudEventToTopic(CloudEvent cloudEvent, TbQueueProducer<TbProtoQueueMsg<ToCloudEventMsg>> producer, TbQueueCallback callback) {
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();

        ToCloudEventMsg toCloudEventMsg = ToCloudEventMsg.newBuilder()
                .setCloudEventMsg(ProtoUtils.toProto(cloudEvent))
                .build();

        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), toCloudEventMsg), callback);
    }

    @Override
    public PageData<CloudEvent> findCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public PageData<CloudEvent> findTsKvCloudEvents(TenantId tenantId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        throw new RuntimeException("Not implemented!");
    }

}
