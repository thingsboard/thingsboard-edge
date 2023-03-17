///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import config from 'config';
import { _logger } from '../config/logger';
import { JsInvokeMessageProcessor } from '../api/jsInvokeMessageProcessor'
import { IQueue } from './queue.models';
import {
    CreateQueueOptions,
    ProcessErrorArgs,
    ServiceBusAdministrationClient,
    ServiceBusClient,
    ServiceBusReceivedMessage,
    ServiceBusReceiver,
    ServiceBusSender
} from '@azure/service-bus';

export class ServiceBusTemplate implements IQueue {

    private logger = _logger(`serviceBusTemplate`);
    private requestTopic: string = config.get('request_topic');
    private namespaceName = config.get('service_bus.namespace_name');
    private sasKeyName = config.get('service_bus.sas_key_name');
    private sasKey = config.get('service_bus.sas_key');
    private queueProperties: string = config.get('service_bus.queue_properties');

    private sbClient: ServiceBusClient;
    private serviceBusService: ServiceBusAdministrationClient;
    private queueOptions: CreateQueueOptions = {};
    private queues: string[] = [];
    private receiver: ServiceBusReceiver;
    private senderMap = new Map<string, ServiceBusSender>();

    name = 'Azure Service Bus';

    constructor() {
    }

    async init() {
        const connectionString = `Endpoint=sb://${this.namespaceName}.servicebus.windows.net/;SharedAccessKeyName=${this.sasKeyName};SharedAccessKey=${this.sasKey}`;
        this.sbClient = new ServiceBusClient(connectionString)
        this.serviceBusService = new ServiceBusAdministrationClient(connectionString);

        this.parseQueueProperties();

        const listQueues = await this.serviceBusService.listQueues();
        for await (const queue of listQueues) {
            this.queues.push(queue.name);
        }

        if (!this.queues.includes(this.requestTopic)) {
            await this.createQueueIfNotExist(this.requestTopic);
            this.queues.push(this.requestTopic);
        }

        this.receiver = this.sbClient.createReceiver(this.requestTopic, {receiveMode: 'peekLock'});

        const messageProcessor = new JsInvokeMessageProcessor(this);

        const messageHandler = async (message: ServiceBusReceivedMessage) => {
            if (message) {
                messageProcessor.onJsInvokeMessage(message.body);
                await this.receiver.completeMessage(message);
            }
        };
        const errorHandler = async (error: ProcessErrorArgs) => {
            this.logger.error('Failed to receive message from queue.', error);
        };
        this.receiver.subscribe({processMessage: messageHandler, processError: errorHandler})
    }

    async send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any> {
        if (!this.queues.includes(this.requestTopic)) {
            await this.createQueueIfNotExist(this.requestTopic);
            this.queues.push(this.requestTopic);
        }

        let customSender = this.senderMap.get(responseTopic);

        if (!customSender) {
            customSender = this.sbClient.createSender(responseTopic);
            this.senderMap.set(responseTopic, customSender);
        }

        let data = {
            key: msgKey,
            data: [...rawResponse],
            headers: headers
        };

        return customSender.sendMessages({body: data});
    }

    private parseQueueProperties() {
        let properties: { [n: string]: string } = {};
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            properties[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
        });
        this.queueOptions = {
            requiresDuplicateDetection: false,
            maxSizeInMegabytes: Number(properties['maxSizeInMb']),
            defaultMessageTimeToLive: `PT${properties['messageTimeToLiveInSec']}S`,
            lockDuration: `PT${properties['lockDurationInSec']}S`
        };
    }

    private async createQueueIfNotExist(topic: string) {
        try {
            await this.serviceBusService.createQueue(topic, this.queueOptions)
        } catch (err: any) {
            if (err && err.code !== "MessageEntityAlreadyExistsError") {
                throw new Error(err);
            }
        }
    }

    async destroy() {
        this.logger.info('Stopping Azure Service Bus resources...')
        if (this.receiver) {
            this.logger.info('Stopping Service Bus Receiver...');
            try {
                const _receiver = this.receiver;
                // @ts-ignore
                delete this.receiver;
                await _receiver.close();
                this.logger.info('Service Bus Receiver stopped.');
            } catch (e) {
                this.logger.info('Service Bus Receiver stop error.');
            }
        }

        this.logger.info('Stopping Service Bus Senders...');
        const senders: Promise<void>[] = [];
        this.senderMap.forEach((sender) => {
            senders.push(sender.close());
        });
        this.senderMap.clear();
        try {
            await Promise.all(senders);
            this.logger.info('Service Bus Senders stopped.');
        } catch (e) {
            this.logger.info('Service Bus Senders stop error.');
        }

        if (this.sbClient) {
            this.logger.info('Stopping Service Bus Client...');
            try {
                const _sbClient = this.sbClient;
                // @ts-ignore
                delete this.sbClient;
                await _sbClient.close();
                this.logger.info('Service Bus Client stopped.');
            } catch (e) {
                this.logger.info('Service Bus Client stop error.');
            }
        }
        this.logger.info('Azure Service Bus resources stopped.')
    }
}
