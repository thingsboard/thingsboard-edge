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
import amqp, { ConfirmChannel, Connection } from 'amqplib';
import { Options, Replies } from 'amqplib/properties';

export class RabbitMqTemplate implements IQueue {

    private logger = _logger(`rabbitmqTemplate`);
    private requestTopic: string = config.get('request_topic');
    private host = config.get('rabbitmq.host');
    private port = config.get('rabbitmq.port');
    private vhost = config.get('rabbitmq.virtual_host');
    private username = config.get('rabbitmq.username');
    private password = config.get('rabbitmq.password');
    private queueProperties: string = config.get('rabbitmq.queue_properties');

    private queueOptions: Options.AssertQueue = {
        durable: false,
        exclusive: false,
        autoDelete: false
    };
    private connection: Connection;
    private channel: ConfirmChannel;
    private topics: string[] = [];

    name = 'RabbitMQ';

    constructor() {
    }

    async init(): Promise<void> {
        const url = `amqp://${this.username}:${this.password}@${this.host}:${this.port}${this.vhost}`;
        this.connection = await amqp.connect(url);
        this.channel = await this.connection.createConfirmChannel();

        this.parseQueueProperties();

        await this.createQueue(this.requestTopic);

        const messageProcessor = new JsInvokeMessageProcessor(this);

        await this.channel.consume(this.requestTopic, (message) => {
            if (message) {
                messageProcessor.onJsInvokeMessage(JSON.parse(message.content.toString('utf8')));
                this.channel.ack(message);
            }
        })
    }

    async send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any> {

        if (!this.topics.includes(responseTopic)) {
            await this.createQueue(responseTopic);
            this.topics.push(responseTopic);
        }

        let data = JSON.stringify(
            {
                key: msgKey,
                data: [...rawResponse],
                headers: headers
            });
        let dataBuffer = Buffer.from(data);
        this.channel.sendToQueue(responseTopic, dataBuffer);
        return this.channel.waitForConfirms()
    }

    private parseQueueProperties() {
        let args: { [n: string]: number } = {};
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            args[p.substring(0, delimiterPosition)] = Number(p.substring(delimiterPosition + 1));
        });
        this.queueOptions['arguments'] = args;
    }

    private async createQueue(topic: string): Promise<Replies.AssertQueue> {
        return this.channel.assertQueue(topic, this.queueOptions);
    }

    async destroy() {
        this.logger.info('Stopping RabbitMQ resources...');

        if (this.channel) {
            this.logger.info('Stopping RabbitMQ chanel...');
            const _channel = this.channel;
            // @ts-ignore
            delete this.channel;
            await _channel.close();
            this.logger.info('RabbitMQ chanel stopped');
        }

        if (this.connection) {
            this.logger.info('Stopping RabbitMQ connection...')
            try {
                const _connection = this.connection;
                // @ts-ignore
                delete this.connection;
                await _connection.close();
                this.logger.info('RabbitMQ client connection.');
            } catch (e) {
                this.logger.info('RabbitMQ connection stop error.');
            }
        }
        this.logger.info('RabbitMQ resources stopped.')
    }

}
