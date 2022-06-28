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
    logger = require('../config/logger')._logger('serviceBusTemplate');
const {ServiceBusClient, ServiceBusAdministrationClient} = require("@azure/service-bus");

const requestTopic = config.get('request_topic');
const namespaceName = config.get('service_bus.namespace_name');
const sasKeyName = config.get('service_bus.sas_key_name');
const sasKey = config.get('service_bus.sas_key');
const queueProperties = config.get('service_bus.queue_properties');

let sbClient;
let receiver;
let serviceBusService;

let queueOptions = {};
const queues = [];
const senderMap = new Map();

function ServiceBusProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        if (!queues.includes(requestTopic)) {
            await createQueueIfNotExist(requestTopic);
            queues.push(requestTopic);
        }

        let customSender = senderMap.get(responseTopic);

        if (!customSender) {
            customSender = new CustomSender(responseTopic);
            senderMap.set(responseTopic, customSender);
        }

        let data = {
            key: scriptId,
            data: [...rawResponse],
            headers: headers
        };

        return customSender.send({body: data});
    }
}

function CustomSender(topic) {
    this.sender = sbClient.createSender(topic);

    this.send = async (message) => {
        return this.sender.sendMessages(message);
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const connectionString = `Endpoint=sb://${namespaceName}.servicebus.windows.net/;SharedAccessKeyName=${sasKeyName};SharedAccessKey=${sasKey}`;
        sbClient = new ServiceBusClient(connectionString)
        serviceBusService = new ServiceBusAdministrationClient(connectionString);

        parseQueueProperties();

        await new Promise((resolve, reject) => {
            serviceBusService.listQueues((err, data) => {
                if (err) {
                    reject(err);
                } else {
                    for (const queue of data) {
                        queues.push(queue.name);
                    }
                    resolve();
                }
            });
        });

        if (!queues.includes(requestTopic)) {
            await createQueueIfNotExist(requestTopic);
            queues.push(requestTopic);
        }

        receiver = sbClient.createReceiver(requestTopic, {receiveMode: 'peekLock'});

        const messageProcessor = new JsInvokeMessageProcessor(new ServiceBusProducer());

        const messageHandler = async (message) => {
            if (message) {
                messageProcessor.onJsInvokeMessage(message.body);
                await message.complete();
            }
        };
        const errorHandler = (error) => {
            logger.error('Failed to receive message from queue.', error);
        };
        receiver.subscribe({processMessage: messageHandler, processError: errorHandler})
    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }
})();

async function createQueueIfNotExist(topic) {
    return new Promise((resolve, reject) => {
        serviceBusService.createQueue(topic, queueOptions, (err) => {
            if (err && err.code !== "MessageEntityAlreadyExistsError") {
                reject(err);
            } else {
                resolve();
            }
        });
    });
}

function parseQueueProperties() {
    let properties = {};
    const props = queueProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        properties[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
    });
    queueOptions = {
        requiresDuplicateDetection: false,
        maxSizeInMegabytes: properties['maxSizeInMb'],
        defaultMessageTimeToLive: `PT${properties['messageTimeToLiveInSec']}S`,
        lockDuration: `PT${properties['lockDurationInSec']}S`
    };
}

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    logger.info('Stopping Azure Service Bus resources...')
    if (receiver) {
        try {
            await receiver.close();
        } catch (e) {

        }
    }

    senderMap.forEach((k, v) => {
        try {
            v.sender.close();
        } catch (e) {

        }
    });

    if (sbClient) {
        try {
            sbClient.close();
        } catch (e) {

        }
    }
    logger.info('Azure Service Bus resources stopped.')
    process.exit(status);
}
