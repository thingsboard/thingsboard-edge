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
package org.thingsboard.server.queue.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;

@Component
@InMemoryEdqsComponent
@RequiredArgsConstructor
public class InMemoryEdqsQueueFactory implements EdqsQueueFactory {

    private final InMemoryStorage storage;
    private final EdqsConfig edqsConfig;
    private final StatsFactory statsFactory;

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgConsumer(EdqsQueue queue) {
        if (queue == EdqsQueue.STATE) {
            throw new UnsupportedOperationException();
        }
        return new InMemoryTbQueueConsumer<>(storage, queue.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgConsumer(EdqsQueue queue, String group) {
        return createEdqsMsgConsumer(queue);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsMsgProducer(EdqsQueue queue) {
        if (queue == EdqsQueue.STATE) {
            throw new UnsupportedOperationException();
        }
        return new InMemoryTbQueueProducer<>(storage, queue.getTopic());
    }

    @Override
    public TbQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsResponseTemplate() {
        TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> requestConsumer = new InMemoryTbQueueConsumer<>(storage, edqsConfig.getRequestsTopic());
        TbQueueProducer<TbProtoQueueMsg<FromEdqsMsg>> responseProducer = new InMemoryTbQueueProducer<>(storage, edqsConfig.getResponsesTopic());
        return DefaultTbQueueResponseTemplate.<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>>builder()
                .requestTemplate(requestConsumer)
                .responseTemplate(responseProducer)
                .maxPendingRequests(edqsConfig.getMaxPendingRequests())
                .requestTimeout(edqsConfig.getMaxRequestTimeout())
                .pollInterval(edqsConfig.getPollInterval())
                .stats(statsFactory.createMessagesStats(StatsType.EDQS.getName()))
                .executor(ThingsBoardExecutors.newWorkStealingPool(5, "edqs"))
                .build();
    }

}
