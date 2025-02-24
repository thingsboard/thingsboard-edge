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
package org.thingsboard.server.queue.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TbRuleEngineProducerService {

    private final PartitionService partitionService;

    public void sendToRuleEngine(TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> producer,
                                 TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        List<TopicPartitionInfo> tpis = partitionService.resolveAll(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, tbMsg.getOriginator());
        if (tpis.size() > 1) {
            UUID correlationId = UUID.randomUUID();
            for (int i = 0; i < tpis.size(); i++) {
                TopicPartitionInfo tpi = tpis.get(i);
                Integer partition = tpi.getPartition().orElse(null);
                UUID id = i > 0 ? UUID.randomUUID() : tbMsg.getId();

                tbMsg = tbMsg.transform()
                        .id(id)
                        .correlationId(correlationId)
                        .partition(partition)
                        .build();
                sendToRuleEngine(producer, tpi, tenantId, tbMsg, i == tpis.size() - 1 ? callback : null);
            }
        } else {
            sendToRuleEngine(producer, tpis.get(0), tenantId, tbMsg, callback);
        }
    }

    private void sendToRuleEngine(TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> producer, TopicPartitionInfo tpi,
                                  TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, tbMsg.getOriginator(), tpi.getFullTopicName(), tbMsg);
        }
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder()
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

}
