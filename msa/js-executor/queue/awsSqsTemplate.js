/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
    logger = require('../config/logger')._logger('awsSqsTemplate');
const uuid = require('uuid-random');

const requestTopic = config.get('request_topic');

const accessKeyId = config.get('aws_sqs.access_key_id');
const secretAccessKey = config.get('aws_sqs.secret_access_key');
const region = config.get('aws_sqs.region');
const AWS = require('aws-sdk');
const queueProperties = config.get('aws_sqs.queue_properties');
const pollInterval = config.get('js.response_poll_interval');

let queueAttributes = {FifoQueue: 'true'};
let sqsClient;
let requestQueueURL;
const queueUrls = new Map();
let stopped = false;

function AwsSqsProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        let msgBody = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });

        let responseQueueUrl = queueUrls.get(topicToSqsQueueName(responseTopic));

        if (!responseQueueUrl) {
            responseQueueUrl = await createQueue(responseTopic);
            queueUrls.set(responseTopic, responseQueueUrl);
        }

        let msgId = uuid();

        let params = {
            MessageBody: msgBody,
            QueueUrl: responseQueueUrl,
            MessageGroupId: msgId,
            MessageDeduplicationId: msgId
        };

        return new Promise((resolve, reject) => {
            sqsClient.sendMessage(params, function (err, data) {
                if (err) {
                    reject(err);
                } else {
                    resolve(data);
                }
            });
        });
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
        AWS.config.update({accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region});

        sqsClient = new AWS.SQS({apiVersion: '2012-11-05'});

        const queues = await getQueues();

        if (queues) {
            queues.forEach(queueUrl => {
                const delimiterPosition = queueUrl.lastIndexOf('/');
                const queueName = queueUrl.substring(delimiterPosition + 1);
                queueUrls.set(queueName, queueUrl);
            });
        }

        parseQueueProperties();

        requestQueueURL = queueUrls.get(topicToSqsQueueName(requestTopic));
        if (!requestQueueURL) {
            requestQueueURL = await createQueue(requestTopic);
        }

        const messageProcessor = new JsInvokeMessageProcessor(new AwsSqsProducer());

        const params = {
            MaxNumberOfMessages: 10,
            QueueUrl: requestQueueURL,
            WaitTimeSeconds: pollInterval / 1000
        };
        while (!stopped) {
            let pollStartTs = new Date().getTime();
            const messages = await new Promise((resolve, reject) => {
                sqsClient.receiveMessage(params, function (err, data) {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(data.Messages);
                    }
                });
            });

            if (messages && messages.length > 0) {
                const entries = [];

                messages.forEach(message => {
                    entries.push({
                        Id: message.MessageId,
                        ReceiptHandle: message.ReceiptHandle
                    });
                    messageProcessor.onJsInvokeMessage(JSON.parse(message.Body));
                });

                const deleteBatch = {
                    QueueUrl: requestQueueURL,
                    Entries: entries
                };
                sqsClient.deleteMessageBatch(deleteBatch, function (err, data) {
                    if (err) {
                        logger.error("Failed to delete messages from queue.", err.message);
                    } else {
                        //do nothing
                    }
                });
            } else {
                let pollDuration = new Date().getTime() - pollStartTs;
                if (pollDuration < pollInterval) {
                    await sleep(pollInterval - pollDuration);
                }
            }
        }
    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

function createQueue(topic) {
    let queueName = topicToSqsQueueName(topic);
    let queueParams = {QueueName: queueName, Attributes: queueAttributes};

    return new Promise((resolve, reject) => {
        sqsClient.createQueue(queueParams, function (err, data) {
            if (err) {
                reject(err);
            } else {
                resolve(data.QueueUrl);
            }
        });
    });
}

function getQueues() {
    return new Promise((resolve, reject) => {
        sqsClient.listQueues(function (err, data) {
            if (err) {
                reject(err);
            } else {
                resolve(data.QueueUrls);
            }
        });
    });
}

function topicToSqsQueueName(topic) {
    return topic.replace(/\./g, '_') + '.fifo';
}

function parseQueueProperties() {
    const props = queueProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        queueAttributes[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
    });
}

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

process.on('exit', () => {
    stopped = true;
    logger.info('Aws Sqs client stopped.');
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (sqsClient) {
        logger.info('Stopping Aws Sqs client.')
        try {
            await sqsClient.close();
            logger.info('Aws Sqs client stopped.')
            process.exit(status);
        } catch (e) {
            logger.info('Aws Sqs client stop error.');
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}
