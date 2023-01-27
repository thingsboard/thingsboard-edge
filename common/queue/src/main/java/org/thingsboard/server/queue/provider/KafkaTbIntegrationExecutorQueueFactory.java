/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationApiSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-integration-executor'")
public class KafkaTbIntegrationExecutorQueueFactory implements TbIntegrationExecutorQueueFactory {

    private final NotificationsTopicService notificationsTopicService;
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueIntegrationApiSettings integrationApiSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbKafkaConsumerStatsService consumerStatsService;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorRequestAdmin;
    private final TbQueueAdmin jsExecutorResponseAdmin;
    private final TbQueueAdmin integrationApiRequestAdmin;
    private final TbQueueAdmin integrationApiResponseAdmin;
    private final TbQueueAdmin notificationAdmin;

    private final AtomicLong integrationConsumerCount = new AtomicLong();

    public KafkaTbIntegrationExecutorQueueFactory(NotificationsTopicService notificationsTopicService,
                                                  TbKafkaSettings kafkaSettings,
                                                  TbServiceInfoProvider serviceInfoProvider,
                                                  TbQueueCoreSettings coreSettings,
                                                  TbQueueIntegrationApiSettings integrationApiSettings,
                                                  TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                                  TbKafkaConsumerStatsService consumerStatsService,
                                                  TbQueueIntegrationNotificationSettings integrationNotificationSettings,
                                                  TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.notificationsTopicService = notificationsTopicService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.integrationApiSettings = integrationApiSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.consumerStatsService = consumerStatsService;

        this.coreAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.jsExecutorRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorRequestConfigs());
        this.jsExecutorResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getJsExecutorResponseConfigs());
        this.integrationApiRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getIntegrationApiRequestConfigs());
        this.integrationApiResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getIntegrationApiResponseConfigs());
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> createTbCoreIntegrationMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreIntegrationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-ie-to-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getIntegrationsTopic());
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-ie-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createToIntegrationExecutorNotificationsMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(notificationsTopicService.getNotificationsTopic(ServiceType.TB_INTEGRATION_EXECUTOR, serviceInfoProvider.getServiceId()).getFullTopicName());
        consumerBuilder.clientId("tb-ie-notifications-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId("ie-notifications-node-" + serviceInfoProvider.getServiceId());
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(notificationAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType integrationType) {
        String queueName = integrationType.name();
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(coreSettings.getIntegrationDownlinkTopic(integrationType));
        consumerBuilder.clientId("ie-" + queueName + "-consumer-" + serviceInfoProvider.getServiceId() + "-" + integrationConsumerCount.incrementAndGet());
        consumerBuilder.groupId("ie-" + queueName + "-consumer");
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorDownlinkMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(ruleEngineAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-js-invoke-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(jsInvokeSettings.getRequestTopic());
        requestBuilder.admin(jsExecutorRequestAdmin);

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("js-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("ie-node-" + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                }
        );
        responseBuilder.admin(jsExecutorResponseAdmin);
        responseBuilder.statsService(consumerStatsService);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorResponseAdmin);
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> createIntegrationApiRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<IntegrationApiRequestMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("integration-api-request-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(integrationApiSettings.getRequestsTopic());
        requestBuilder.admin(integrationApiRequestAdmin);

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<IntegrationApiResponseMsg>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(integrationApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId());
        responseBuilder.clientId("integration-api-response-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId("integration-node-" + serviceInfoProvider.getServiceId());
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), IntegrationApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));
        responseBuilder.admin(integrationApiResponseAdmin);
        responseBuilder.statsService(consumerStatsService);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(integrationApiResponseAdmin);
        templateBuilder.requestTemplate(requestBuilder.build());
        templateBuilder.responseTemplate(responseBuilder.build());
        templateBuilder.maxPendingRequests(integrationApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(integrationApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(integrationApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorRequestAdmin != null) {
            jsExecutorRequestAdmin.destroy();
        }
        if (jsExecutorResponseAdmin != null) {
            jsExecutorResponseAdmin.destroy();
        }
        if (integrationApiRequestAdmin != null) {
            integrationApiRequestAdmin.destroy();
        }
        if (integrationApiResponseAdmin != null) {
            integrationApiResponseAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-ie-us-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(coreSettings.getUsageStatsTopic());
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }
}
