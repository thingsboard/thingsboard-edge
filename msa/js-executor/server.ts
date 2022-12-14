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
import { _logger } from './config/logger';
import { HttpServer } from './api/httpServer';
import { IQueue } from './queue/queue.models';
import { KafkaTemplate } from './queue/kafkaTemplate';
import { PubSubTemplate } from './queue/pubSubTemplate';
import { AwsSqsTemplate } from './queue/awsSqsTemplate';
import { RabbitMqTemplate } from './queue/rabbitmqTemplate';
import { ServiceBusTemplate } from './queue/serviceBusTemplate';

const logger = _logger('main');

logger.info('===CONFIG BEGIN===');
logger.info(JSON.stringify(config, null, 4));
logger.info('===CONFIG END===');

const serviceType: string = config.get('queue_type');
const httpPort = Number(config.get('http_port'));
let queues: IQueue | null;
let httpServer: HttpServer | null;

(async () => {
    logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
    try {
        queues = await createQueue(serviceType);
        logger.info(`Starting ${queues.name} template...`);
        await queues.init();
        logger.info(`${queues.name} template started.`);
        httpServer = new HttpServer(httpPort);
    } catch (e: any) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }

})();

async function createQueue(serviceType: string): Promise<IQueue> {
    switch (serviceType) {
        case 'kafka':
            return new KafkaTemplate();
        case 'pubsub':
            return new PubSubTemplate();
        case 'aws-sqs':
            return new AwsSqsTemplate();
        case 'rabbitmq':
            return new RabbitMqTemplate();
        case 'service-bus':
            return new ServiceBusTemplate();
        default:
            throw new Error('Unknown service type: ' + serviceType);
    }
}

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    })
})

process.on('exit', (code: number) => {
    logger.info(`ThingsBoard JavaScript Executor Microservice has been stopped. Exit code: ${code}.`);
});

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    try {
        if (httpServer) {
            const _httpServer = httpServer;
            httpServer = null;
            await _httpServer.stop();
        }
        if (queues) {
            const _queues = queues;
            queues = null;
            await _queues.destroy();
        }
    } catch (e) {
        logger.error('Error on exit');
    }
    process.exit(status);
}
