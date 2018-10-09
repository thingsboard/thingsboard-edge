/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
const config = require('config'),
      kafka = require('kafka-node'),
      ConsumerGroup = kafka.ConsumerGroup,
      Producer = kafka.Producer,
      JsInvokeMessageProcessor = require('./api/jsInvokeMessageProcessor'),
      logger = require('./config/logger')('main');

var kafkaClient;

(async() => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
        const kafkaRequestTopic = config.get('kafka.request_topic');

        logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        logger.info('Kafka Requests Topic: %s', kafkaRequestTopic);

        kafkaClient = new kafka.KafkaClient({kafkaHost: kafkaBootstrapServers});

        var consumer = new ConsumerGroup(
            {
                kafkaHost: kafkaBootstrapServers,
                groupId: 'js-executor-group',
                autoCommit: true,
                encoding: 'buffer'
            },
            kafkaRequestTopic
        );

        var producer = new Producer(kafkaClient);
        producer.on('error', (err) => {
            logger.error('Unexpected kafka producer error: %s', err.message);
            logger.error(err.stack);
        });

        var messageProcessor = new JsInvokeMessageProcessor(producer);

        producer.on('ready', () => {
            consumer.on('message', (message) => {
                messageProcessor.onJsInvokeMessage(message);
            });
            logger.info('Started ThingsBoard JavaScript Executor Microservice.');
        });

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

process.on('exit', function () {
    exit(0);
});

function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (kafkaClient) {
        logger.info('Stopping Kafka Client...');
        var _kafkaClient = kafkaClient;
        kafkaClient = null;
        _kafkaClient.close(() => {
            logger.info('Kafka Client stopped.');
            process.exit(status);
        });
    } else {
        process.exit(status);
    }
}
