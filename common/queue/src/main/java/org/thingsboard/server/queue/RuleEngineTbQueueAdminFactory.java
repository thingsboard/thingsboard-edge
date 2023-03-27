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
package org.thingsboard.server.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusQueueConfigs;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusSettings;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.pubsub.TbPubSubAdmin;
import org.thingsboard.server.queue.pubsub.TbPubSubSettings;
import org.thingsboard.server.queue.pubsub.TbPubSubSubscriptionSettings;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

@Configuration
public class RuleEngineTbQueueAdminFactory {

    @Autowired(required = false)
    private TbKafkaTopicConfigs kafkaTopicConfigs;
    @Autowired(required = false)
    private TbKafkaSettings kafkaSettings;

    @Autowired(required = false)
    private TbAwsSqsQueueAttributes awsSqsQueueAttributes;
    @Autowired(required = false)
    private TbAwsSqsSettings awsSqsSettings;

    @Autowired(required = false)
    private TbPubSubSubscriptionSettings pubSubSubscriptionSettings;
    @Autowired(required = false)
    private TbPubSubSettings pubSubSettings;

    @Autowired(required = false)
    private TbRabbitMqQueueArguments rabbitMqQueueArguments;
    @Autowired(required = false)
    private TbRabbitMqSettings rabbitMqSettings;

    @Autowired(required = false)
    private TbServiceBusQueueConfigs serviceBusQueueConfigs;
    @Autowired(required = false)
    private TbServiceBusSettings serviceBusSettings;

    @ConditionalOnExpression("'${queue.type:null}'=='kafka'")
    @Bean
    public TbQueueAdmin createKafkaAdmin() {
        return new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
    @Bean
    public TbQueueAdmin createAwsSqsAdmin() {
        return new TbAwsSqsAdmin(awsSqsSettings, awsSqsQueueAttributes.getRuleEngineAttributes());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
    @Bean
    public TbQueueAdmin createPubSubAdmin() {
        return new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getRuleEngineSettings());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
    @Bean
    public TbQueueAdmin createRabbitMqAdmin() {
        return new TbRabbitMqAdmin(rabbitMqSettings, rabbitMqQueueArguments.getRuleEngineArgs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
    @Bean
    public TbQueueAdmin createServiceBusAdmin() {
        return new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
    @Bean
    public TbQueueAdmin createInMemoryAdmin() {
        return new TbQueueAdmin() {

            @Override
            public void createTopicIfNotExists(String topic) {
            }

            @Override
            public void deleteTopic(String topic) {
            }

            @Override
            public void destroy() {
            }
        };
    }
}