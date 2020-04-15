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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusConsumerTemplate;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusProducerTemplate;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class ServiceBusTransportQueueFactory implements TbTransportQueueFactory {
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbServiceBusSettings serviceBusSettings;
    private final TbQueueAdmin admin;
    private final TbServiceInfoProvider serviceInfoProvider;

    public ServiceBusTransportQueueFactory(TbQueueTransportApiSettings transportApiSettings,
                                            TbQueueTransportNotificationSettings transportNotificationSettings,
                                            TbServiceBusSettings serviceBusSettings,
                                            TbServiceInfoProvider serviceInfoProvider,
                                            TbQueueAdmin admin) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.serviceBusSettings = serviceBusSettings;
        this.admin = admin;
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> producerTemplate =
                new TbServiceBusProducerTemplate<>(admin, serviceBusSettings, transportApiSettings.getRequestsTopic());

        TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> consumerTemplate =
                new TbServiceBusConsumerTemplate<>(admin, serviceBusSettings,
                        transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId(),
                        msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(admin);
        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbServiceBusProducerTemplate<>(admin, serviceBusSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbServiceBusProducerTemplate<>(admin, serviceBusSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> createTransportNotificationsConsumer() {
        return new TbServiceBusConsumerTemplate<>(admin, serviceBusSettings,
                transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }
}
