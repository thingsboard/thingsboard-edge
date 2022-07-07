///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import { sleep } from '../api/utils';

export class RabbitMqTemplate implements IQueue {

    private logger = _logger(`rabbitmqTemplate`);
    private requestTopic: string = config.get('request_topic');
    private host = config.get('rabbitmq.host');
    private port = config.get('rabbitmq.port');
    private vhost = config.get('rabbitmq.virtual_host');
    private username = config.get('rabbitmq.username');
    private password = config.get('rabbitmq.password');
    private queueProperties: string = config.get('rabbitmq.queue_properties');
    private pollInterval = Number(config.get('js.response_poll_interval'));

    private queueOptions: Options.AssertQueue = {
        durable: false,
        exclusive: false,
        autoDelete: false
    };
    private connection: Connection;
    private channel: ConfirmChannel;
    private stopped = false;
    private topics: string[] = [];

    constructor() {
    }

    async init(): Promise<void> {
        try {
            this.logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

            const url = `amqp://${this.username}:${this.password}@${this.host}:${this.port}${this.vhost}`;
            this.connection = await amqp.connect(url);
            this.channel = await this.connection.createConfirmChannel();

            this.parseQueueProperties();

            await this.createQueue(this.requestTopic);

            const messageProcessor = new JsInvokeMessageProcessor(this);

            while (!this.stopped) {
                let pollStartTs = new Date().getTime();
                let message = await this.channel.get(this.requestTopic);

                if (message) {
                    messageProcessor.onJsInvokeMessage(JSON.parse(message.content.toString('utf8')));
                    this.channel.ack(message);
                } else {
                    let pollDuration = new Date().getTime() - pollStartTs;
                    if (pollDuration < this.pollInterval) {
                        await sleep(this.pollInterval - pollDuration);
                    }
                }
            }
        } catch (e: any) {
            this.logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
            this.logger.error(e.stack);
            await this.exit(-1);
        }
    }

    async send(responseTopic: string, scriptId: string, rawResponse: Buffer, headers: any): Promise<any> {

        if (!this.topics.includes(responseTopic)) {
            await this.createQueue(responseTopic);
            this.topics.push(responseTopic);
        }

        let data = JSON.stringify(
            {
                key: scriptId,
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

    static async build(): Promise<RabbitMqTemplate> {
        const queue = new RabbitMqTemplate();
        await queue.init();
        return queue;
    }

    async exit(status: number) {
        this.logger.info('Exiting with status: %d ...', status);

        if (this.channel) {
            this.logger.info('Stopping RabbitMq chanel.')
            await this.channel.close();
            // @ts-ignore
            delete this.channel;
            this.logger.info('RabbitMq chanel stopped');
        }

        if (this.connection) {
            this.logger.info('Stopping RabbitMq connection.')
            try {
                await this.connection.close();
                // @ts-ignore
                delete this.connection;
                this.logger.info('RabbitMq client connection.')
                process.exit(status);
            } catch (e) {
                this.logger.info('RabbitMq connection stop error.');
                process.exit(status);
            }
        } else {
            process.exit(status);
        }
    }

}
