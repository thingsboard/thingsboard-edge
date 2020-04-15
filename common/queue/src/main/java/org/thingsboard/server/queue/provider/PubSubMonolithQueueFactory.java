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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.pubsub.TbPubSubAdmin;
import org.thingsboard.server.queue.pubsub.TbPubSubConsumerTemplate;
import org.thingsboard.server.queue.pubsub.TbPubSubProducerTemplate;
import org.thingsboard.server.queue.pubsub.TbPubSubSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub' && '${service.type:null}'=='monolith'")
public class PubSubMonolithQueueFactory implements TbCoreQueueFactory, TbRuleEngineQueueFactory {

    private final TbPubSubSettings pubSubSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbQueueAdmin admin;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;

    public PubSubMonolithQueueFactory(TbPubSubSettings pubSubSettings,
                                      TbQueueCoreSettings coreSettings,
                                      TbQueueRuleEngineSettings ruleEngineSettings,
                                      TbQueueTransportApiSettings transportApiSettings,
                                      TbQueueTransportNotificationSettings transportNotificationSettings,
                                      PartitionService partitionService,
                                      TbServiceInfoProvider serviceInfoProvider) {
        this.pubSubSettings = pubSubSettings;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.admin = new TbPubSubAdmin(pubSubSettings);
        this.partitionService = partitionService;
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, ruleEngineSettings.getTopic());

    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, ruleEngineSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(TbRuleEngineQueueConfiguration configuration) {
        return new TbPubSubConsumerTemplate<>(admin, pubSubSettings, ruleEngineSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(admin, pubSubSettings,
                partitionService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(admin, pubSubSettings, coreSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbPubSubConsumerTemplate<>(admin, pubSubSettings,
                partitionService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbPubSubConsumerTemplate<>(admin, pubSubSettings, transportApiSettings.getRequestsTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbPubSubProducerTemplate<>(admin, pubSubSettings, transportApiSettings.getResponsesTopic());
    }

    @Override
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        return null;
    }
}
