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
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
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
import org.thingsboard.server.queue.pubsub.TbPubSubProducerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqConsumerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqProducerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationApiSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq' && '${service.type:null}'=='tb-integration-executor'")
public class RabbitMqIntegrationExecutorQueueFactory implements TbIntegrationExecutorQueueFactory {

    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbRabbitMqSettings rabbitMqSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueIntegrationApiSettings integrationApiSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin integrationApiAdmin;

    public RabbitMqIntegrationExecutorQueueFactory(NotificationsTopicService notificationsTopicService, TbQueueCoreSettings coreSettings,
                                                   TbServiceInfoProvider serviceInfoProvider,
                                                   TbRabbitMqSettings rabbitMqSettings,
                                                   TbRabbitMqQueueArguments queueArguments,
                                                   TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                                   TbQueueIntegrationApiSettings integrationApiSettings) {
        this.notificationsTopicService = notificationsTopicService;
        this.coreSettings = coreSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.rabbitMqSettings = rabbitMqSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.integrationApiSettings = integrationApiSettings;

        this.coreAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getCoreArgs());
        this.ruleEngineAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getRuleEngineArgs());
        this.jsExecutorAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getJsExecutorArgs());
        this.notificationAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getNotificationsArgs());
        this.integrationApiAdmin = new TbRabbitMqAdmin(rabbitMqSettings, queueArguments.getIntegrationArgs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> createTbCoreIntegrationMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, coreSettings.getIntegrationsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, coreSettings.getTopic());
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<RemoteJsRequest>> producer = new TbRabbitMqProducerTemplate<>(jsExecutorAdmin, rabbitMqSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<RemoteJsResponse>> consumer = new TbRabbitMqConsumerTemplate<>(jsExecutorAdmin, rabbitMqSettings,
                jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> {
                    RemoteJsResponse.Builder builder = RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                });

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> createIntegrationApiRequestTemplate() {
        TbQueueProducer<TbProtoQueueMsg<IntegrationApiRequestMsg>> producer = new TbRabbitMqProducerTemplate<>(integrationApiAdmin, rabbitMqSettings, integrationApiSettings.getRequestsTopic());
        TbQueueConsumer<TbProtoQueueMsg<IntegrationApiResponseMsg>> consumer = new TbRabbitMqConsumerTemplate<>(integrationApiAdmin, rabbitMqSettings,
                integrationApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), IntegrationApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(integrationApiAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(integrationApiSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(integrationApiSettings.getMaxRequestsTimeout());
        builder.pollInterval(integrationApiSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createToIntegrationExecutorNotificationsMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(notificationAdmin, rabbitMqSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_INTEGRATION_EXECUTOR, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorNotificationMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType integrationType) {
        return new TbRabbitMqConsumerTemplate<>(ruleEngineAdmin, rabbitMqSettings,
                HashPartitionService.getIntegrationDownlinkTopic(integrationType),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorDownlinkMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (integrationApiAdmin != null) {
            integrationApiAdmin.destroy();
        }
    }
}
