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
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationApiSettings;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='monolith'")
public class AwsSqsMonolithQueueFactory implements TbCoreQueueFactory, TbRuleEngineQueueFactory, TbVersionControlQueueFactory {

    private final NotificationsTopicService notificationsTopicService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueVersionControlSettings vcSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueIntegrationApiSettings integrationApiSettings;
    private final TbQueueIntegrationExecutorSettings integrationExecutorSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorAdmin;
    private final TbQueueAdmin transportApiAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin otaAdmin;
    private final TbQueueAdmin vcAdmin;
    private final TbQueueAdmin integrationApiAdmin;

    public AwsSqsMonolithQueueFactory(NotificationsTopicService notificationsTopicService, TbQueueCoreSettings coreSettings,
                                      TbQueueRuleEngineSettings ruleEngineSettings,
                                      TbServiceInfoProvider serviceInfoProvider,
                                      TbQueueTransportApiSettings transportApiSettings,
                                      TbQueueTransportNotificationSettings transportNotificationSettings,
                                      TbAwsSqsSettings sqsSettings,
                                      TbQueueVersionControlSettings vcSettings,
                                      TbAwsSqsQueueAttributes sqsQueueAttributes,
                                      TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                      TbQueueIntegrationApiSettings integrationApiSettings,
                                      TbQueueIntegrationExecutorSettings integrationNotificationSettings,
                                      TbQueueIntegrationExecutorSettings integrationExecutorSettings) {
        this.notificationsTopicService = notificationsTopicService;
        this.coreSettings = coreSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        this.vcSettings = vcSettings;
        this.jsInvokeSettings = jsInvokeSettings;
        this.integrationApiSettings = integrationApiSettings;
        this.integrationExecutorSettings = integrationExecutorSettings;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.ruleEngineAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getRuleEngineAttributes());
        this.jsExecutorAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getJsExecutorAttributes());
        this.transportApiAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getTransportApiAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.otaAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getOtaAttributes());
        this.vcAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getVcAttributes());
        this.integrationApiAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getIntegrationAttributes());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(ruleEngineAdmin, sqsSettings, ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(vcAdmin, sqsSettings, vcSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(Queue configuration) {
        return new TbAwsSqsConsumerTemplate<>(ruleEngineAdmin, sqsSettings, configuration.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, coreSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbAwsSqsConsumerTemplate<>(transportApiAdmin, sqsSettings, transportApiSettings.getRequestsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(transportApiAdmin, sqsSettings, transportApiSettings.getResponsesTopic());
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<RemoteJsRequest>> producer = new TbAwsSqsProducerTemplate<>(jsExecutorAdmin, sqsSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<RemoteJsResponse>> consumer = new TbAwsSqsConsumerTemplate<>(jsExecutorAdmin, sqsSettings,
                jsInvokeSettings.getResponseTopic() + "_" + serviceInfoProvider.getServiceId(),
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
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, coreSettings.getUsageStatsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(otaAdmin, sqsSettings, coreSettings.getOtaPackageTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(otaAdmin, sqsSettings, coreSettings.getOtaPackageTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<IntegrationApiRequestMsg>> createIntegrationApiRequestConsumer() {
        return new TbAwsSqsConsumerTemplate<>(integrationApiAdmin, sqsSettings, integrationApiSettings.getRequestsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), IntegrationApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<IntegrationApiResponseMsg>> createIntegrationApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(integrationApiAdmin, sqsSettings, integrationApiSettings.getResponsesTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreIntegrationMsg>> createToCoreIntegrationMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, integrationExecutorSettings.getUplinkTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreIntegrationMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createToIntegrationExecutorNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                notificationsTopicService.getNotificationsTopic(ServiceType.TB_INTEGRATION_EXECUTOR, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorNotificationMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> createIntegrationExecutorNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, integrationExecutorSettings.getNotificationsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType integrationType) {
        return new TbAwsSqsConsumerTemplate<>(ruleEngineAdmin, sqsSettings,
                integrationExecutorSettings.getIntegrationDownlinkTopic(integrationType),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToIntegrationExecutorDownlinkMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> createIntegrationExecutorDownlinkMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, integrationExecutorSettings.getDownlinkTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(vcAdmin, sqsSettings, vcSettings.getTopic());
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
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (otaAdmin != null) {
            otaAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
        if (integrationApiAdmin != null) {
            integrationApiAdmin.destroy();
        }
    }
}
