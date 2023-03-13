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
package org.thingsboard.server.service.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class TbCoreTransportApiService {
    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final TransportApiService transportApiService;
    private final StatsFactory statsFactory;

    @Value("${queue.transport_api.max_pending_requests:10000}")
    private int maxPendingRequests;
    @Value("${queue.transport_api.max_requests_timeout:10000}")
    private long requestTimeout;
    @Value("${queue.transport_api.request_poll_interval:25}")
    private int responsePollDuration;
    @Value("${queue.transport_api.max_callback_threads:10}")
    private int maxCallbackThreads;

    private ExecutorService transportCallbackExecutor;
    private TbQueueResponseTemplate<TbProtoQueueMsg<TransportApiRequestMsg>,
            TbProtoQueueMsg<TransportApiResponseMsg>> transportApiTemplate;

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = ThingsBoardExecutors.newWorkStealingPool(maxCallbackThreads, getClass());
        TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> producer = tbCoreQueueFactory.createTransportApiResponseProducer();
        TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> consumer = tbCoreQueueFactory.createTransportApiRequestConsumer();

        String key = StatsType.TRANSPORT.getName();
        MessagesStats queueStats = statsFactory.createMessagesStats(key);

        DefaultTbQueueResponseTemplate.DefaultTbQueueResponseTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> builder = DefaultTbQueueResponseTemplate.builder();
        builder.requestTemplate(consumer);
        builder.responseTemplate(producer);
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(transportCallbackExecutor);
        builder.stats(queueStats);
        transportApiTemplate = builder.build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        transportApiTemplate.init(transportApiService);
    }

    @PreDestroy
    public void destroy() {
        if (transportApiTemplate != null) {
            transportApiTemplate.stop();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
    }

}
