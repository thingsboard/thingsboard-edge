/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.kafka.TBKafkaAdmin;
import org.thingsboard.server.queue.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-core'")
public class KafkaTbCoreQueueFactory implements TbCoreQueueFactory {

    private final PartitionService partitionService;
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;

    public KafkaTbCoreQueueFactory(PartitionService partitionService, TbKafkaSettings kafkaSettings,
                                   TbServiceInfoProvider serviceInfoProvider,
                                   TbQueueCoreSettings coreSettings,
                                   TbQueueRuleEngineSettings ruleEngineSettings,
                                   TbQueueTransportApiSettings transportApiSettings,
                                   TbQueueRemoteJsInvokeSettings jsInvokeSettings) {
        this.partitionService = partitionService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.jsInvokeSettings = jsInvokeSettings;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-transport-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-rule-engine-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-rule-engine-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(ruleEngineSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> consumerBuilder = TBKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(coreSettings.getTopic());
        consumerBuilder.clientId("tb-core-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("tb-core-node-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> consumerBuilder = TBKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(partitionService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-core-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("tb-core-notifications-node-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> consumerBuilder = TBKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(transportApiSettings.getRequestsTopic());
        consumerBuilder.clientId("tb-core-transport-api-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("tb-core-transport-api-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return consumerBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-core-transport-api-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-js-invoke-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(jsInvokeSettings.getRequestTopic());

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("js-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("rule-engine-node-" + serviceInfoProvider.getServiceId());
//        responseBuilder.autoCommit(true);
//        responseBuilder.autoCommitIntervalMs(autoCommitInterval);
        responseBuilder.decoder(msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                }
        );

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(new TBKafkaAdmin(kafkaSettings));
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

}
