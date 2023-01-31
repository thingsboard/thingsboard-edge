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
import { PubSub } from '@google-cloud/pubsub';
import { IQueue } from './queue.models';
import { Message } from '@google-cloud/pubsub/build/src/subscriber';

export class PubSubTemplate implements IQueue {

    private logger = _logger(`pubSubTemplate`);
    private projectId: string = config.get('pubsub.project_id');
    private credentials = JSON.parse(config.get('pubsub.service_account'));
    private requestTopic: string = config.get('request_topic');
    private queueProperties: string = config.get('pubsub.queue_properties');

    private pubSubClient: PubSub;
    private queueProps: { [n: string]: string } = {};
    private topics: string[] = [];
    private subscriptions: string[] = [];

    name = 'Pub/Sub';

    constructor() {
    }

    async init() {
        this.pubSubClient = new PubSub({
            projectId: this.projectId,
            credentials: this.credentials
        });

        this.parseQueueProperties();

        const topicList = await this.pubSubClient.getTopics();

        if (topicList) {
            topicList[0].forEach(topic => {
                this.topics.push(PubSubTemplate.getName(topic.name));
            });
        }

        const subscriptionList = await this.pubSubClient.getSubscriptions();

        if (subscriptionList) {
            topicList[0].forEach(sub => {
                this.subscriptions.push(PubSubTemplate.getName(sub.name));
            });
        }

        if (!(this.subscriptions.includes(this.requestTopic) && this.topics.includes(this.requestTopic))) {
            await this.createTopic(this.requestTopic);
            await this.createSubscription(this.requestTopic);
        }

        const subscription = this.pubSubClient.subscription(this.requestTopic);

        const messageProcessor = new JsInvokeMessageProcessor(this);

        const messageHandler = (message: Message) => {
            messageProcessor.onJsInvokeMessage(JSON.parse(message.data.toString('utf8')));
            message.ack();
        };

        subscription.on('message', messageHandler);
    }

    async send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any> {
        if (!(this.subscriptions.includes(responseTopic) && this.topics.includes(this.requestTopic))) {
            await this.createTopic(this.requestTopic);
            await this.createSubscription(this.requestTopic);
        }

        let data = JSON.stringify(
            {
                key: msgKey,
                data: [...rawResponse],
                headers: headers
            });
        let dataBuffer = Buffer.from(data);
        return this.pubSubClient.topic(responseTopic).publishMessage({data: dataBuffer});
    }

    private parseQueueProperties() {
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            this.queueProps[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
        });
    }

    private static getName(fullName: string): string {
        const delimiterPosition = fullName.lastIndexOf('/');
        return fullName.substring(delimiterPosition + 1);
    }

    private async createTopic(topic: string) {
        if (!this.topics.includes(topic)) {
            try {
                await this.pubSubClient.createTopic(topic);
                this.logger.info('Created new Pub/Sub topic: %s', topic);
            } catch (e) {
                this.logger.info('Pub/Sub topic already exists');
            }
            this.topics.push(topic);
        }
    }

    private async createSubscription(topic: string) {
        if (!this.subscriptions.includes(topic)) {
            try {
                await this.pubSubClient.createSubscription(topic, topic, {
                    topic: topic,
                    name: topic,
                    ackDeadlineSeconds: Number(this.queueProps['ackDeadlineInSec']),
                    messageRetentionDuration: {
                        seconds: this.queueProps['messageRetentionInSec']
                    }
                });
                this.logger.info('Created new Pub/Sub subscription: %s', topic);
            } catch (e) {
                this.logger.info('Pub/Sub subscription already exists.');
            }

            this.subscriptions.push(topic);
        }
    }

    async destroy(): Promise<void> {
        this.logger.info('Stopping Pub/Sub resources...');
        if (this.pubSubClient) {
            this.logger.info('Stopping Pub/Sub client...');
            try {
                const _pubSubClient = this.pubSubClient;
                // @ts-ignore
                delete this.pubSubClient;
                await _pubSubClient.close();
                this.logger.info('Pub/Sub client stopped.');
            } catch (e) {
                this.logger.info('Pub/Sub client stop error.');
            }
        }
        this.logger.info('Pub/Sub resources stopped.');
    }
}

