/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.notification;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.notification.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.UUID;

@Service
@ConditionalOnMissingBean(NotificationRuleProcessor.class)
@RequiredArgsConstructor
public class RemoteNotificationRuleProcessor implements NotificationRuleProcessor {

    private final TbQueueProducerProvider producerProvider;
    private final PartitionService partitionService;
    private final DataDecodingEncodingService encodingService;

    @Override
    public void process(NotificationRuleTrigger trigger) {
        TransportProtos.NotificationRuleProcessorMsg.Builder msg = TransportProtos.NotificationRuleProcessorMsg.newBuilder()
                .setTrigger(ByteString.copyFrom(encodingService.encode(trigger)));

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, trigger.getTenantId(), trigger.getOriginatorEntityId());
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportProtos.ToCoreMsg.newBuilder()
                        .setNotificationRuleProcessorMsg(msg)
                        .build()), null);
    }

}
