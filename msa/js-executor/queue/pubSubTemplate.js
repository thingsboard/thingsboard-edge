/*
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
'use strict';

const config = require('config'),
    JsInvokeMessageProcessor = require('../api/jsInvokeMessageProcessor'),
    logger = require('../config/logger')._logger('pubSubTemplate');
const {PubSub} = require('@google-cloud/pubsub');

const projectId = config.get('pubsub.project_id');
const credentials = JSON.parse(config.get('pubsub.service_account'));
const requestTopic = config.get('request_topic');
const queueProperties = config.get('pubsub.queue_properties');

let pubSubClient;

const topics = [];
const subscriptions = [];
const queueProps = [];

function PubSubProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {

        if (!(subscriptions.includes(responseTopic) && topics.includes(requestTopic))) {
            await createTopic(requestTopic);
        }

        let data = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });
        let dataBuffer = Buffer.from(data);
        return pubSubClient.topic(responseTopic).publish(dataBuffer);
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
        pubSubClient = new PubSub({projectId: projectId, credentials: credentials});

        parseQueueProperties();

        const topicList = await pubSubClient.getTopics();

        if (topicList) {
            topicList[0].forEach(topic => {
                topics.push(getName(topic.name));
            });
        }

        const subscriptionList = await pubSubClient.getSubscriptions();

        if (subscriptionList) {
            topicList[0].forEach(sub => {
                subscriptions.push(getName(sub.name));
            });
        }

        if (!(subscriptions.includes(requestTopic) && topics.includes(requestTopic))) {
            await createTopic(requestTopic);
        }

        const subscription = pubSubClient.subscription(requestTopic);

        const messageProcessor = new JsInvokeMessageProcessor(new PubSubProducer());

        const messageHandler = message => {

            messageProcessor.onJsInvokeMessage(JSON.parse(message.data.toString('utf8')));
            message.ack();
        };

        subscription.on('message', messageHandler);

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

async function createTopic(topic) {
    if (!topics.includes(topic)) {
        try {
            await pubSubClient.createTopic(topic);
            logger.info('Created new Pub/Sub topic: %s', topic);
        } catch (e) {
            logger.info('Pub/Sub topic already exists');
        }
        topics.push(topic);
    }
    await createSubscription(topic)
}

async function createSubscription(topic) {
    if (!subscriptions.includes(topic)) {
        try {
            await pubSubClient.createSubscription(topic, topic, {
                topic: topic,
                subscription: topic,
                ackDeadlineSeconds: queueProps['ackDeadlineInSec'],
                messageRetentionDuration: {seconds: queueProps['messageRetentionInSec']}
            });
            logger.info('Created new Pub/Sub subscription: %s', topic);
        } catch (e) {
            logger.info('Pub/Sub subscription already exists.');
        }

        subscriptions.push(topic);
    }
}

function parseQueueProperties() {
    const props = queueProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        queueProps[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
    });
}

function getName(fullName) {
    const delimiterPosition = fullName.lastIndexOf('/');
    return fullName.substring(delimiterPosition + 1);
}

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (pubSubClient) {
        logger.info('Stopping Pub/Sub client.')
        try {
            await pubSubClient.close();
            logger.info('Pub/Sub client stopped.')
            process.exit(status);
        } catch (e) {
            logger.info('Pub/Sub client stop error.');
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}

