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
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-rule-engine'")
public class KafkaTbRuleEngineQueueFactory implements TbRuleEngineQueueFactory {

    private final PartitionService partitionService;
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;

    public KafkaTbRuleEngineQueueFactory(PartitionService partitionService, TbKafkaSettings kafkaSettings,
                                         TbServiceInfoProvider serviceInfoProvider,
                                         TbQueueCoreSettings coreSettings,
                                         TbQueueRuleEngineSettings ruleEngineSettings,
                                         TbQueueRemoteJsInvokeSettings jsInvokeSettings) {
        this.partitionService = partitionService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.jsInvokeSettings = jsInvokeSettings;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-rule-engine-transport-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-rule-engine-to-rule-engine-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-rule-engine-to-rule-engine-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(ruleEngineSettings.getTopic());
        return requestBuilder.build();
    }


    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-rule-engine-to-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-rule-engine-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(TbRuleEngineQueueConfiguration configuration) {
        String queueName = configuration.getName();
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> consumerBuilder = TBKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(ruleEngineSettings.getTopic());
        consumerBuilder.clientId("re-" + queueName + "-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("re-" + queueName + "-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> consumerBuilder = TBKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(partitionService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-rule-engine-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("tb-rule-engine-notifications-node-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return consumerBuilder.build();
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
