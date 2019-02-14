/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.kafka.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "transport", value = "type", havingValue = "remote")
public class RemoteTransportApiService {

    @Value("${transport.remote.transport_api.requests_topic}")
    private String transportApiRequestsTopic;
    @Value("${transport.remote.transport_api.max_pending_requests}")
    private int maxPendingRequests;
    @Value("${transport.remote.transport_api.request_timeout}")
    private long requestTimeout;
    @Value("${transport.remote.transport_api.request_poll_interval}")
    private int responsePollDuration;
    @Value("${transport.remote.transport_api.request_auto_commit_interval}")
    private int autoCommitInterval;

    @Autowired
    private TbKafkaSettings kafkaSettings;

    @Autowired
    private TbNodeIdProvider nodeIdProvider;

    @Autowired
    private TransportApiService transportApiService;

    private ExecutorService transportCallbackExecutor;

    private TbKafkaResponseTemplate<TransportApiRequestMsg, TransportApiResponseMsg> transportApiTemplate;

    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = Executors.newWorkStealingPool(100);

        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TransportApiResponseMsg> responseBuilder = TBKafkaProducerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.clientId("producer-transport-api-response-" + nodeIdProvider.getNodeId());
        responseBuilder.encoder(new TransportApiResponseEncoder());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TransportApiRequestMsg> requestBuilder = TBKafkaConsumerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.topic(transportApiRequestsTopic);
        requestBuilder.clientId(nodeIdProvider.getNodeId());
        requestBuilder.groupId("tb-node");
        requestBuilder.autoCommit(true);
        requestBuilder.autoCommitIntervalMs(autoCommitInterval);
        requestBuilder.decoder(new TransportApiRequestDecoder());

        TbKafkaResponseTemplate.TbKafkaResponseTemplateBuilder
                <TransportApiRequestMsg, TransportApiResponseMsg> builder = TbKafkaResponseTemplate.builder();
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(transportCallbackExecutor);
        builder.handler(transportApiService);
        transportApiTemplate = builder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        transportApiTemplate.init();
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
