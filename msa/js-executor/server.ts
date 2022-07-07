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

const serviceType = config.get('queue_type');
const httpPort = Number(config.get('http_port'));
let queues: IQueue;
let httpServer: HttpServer;

(async () => {
    switch (serviceType) {
        case 'kafka':
            logger.info('Starting kafka template.');
            queues = await KafkaTemplate.build();
            logger.info('kafka template started.');
            break;
        case 'pubsub':
            logger.info('Starting Pub/Sub template.')
            queues = await PubSubTemplate.build();
            logger.info('Pub/Sub template started.')
            break;
        case 'aws-sqs':
            logger.info('Starting Aws Sqs template.')
            queues = await AwsSqsTemplate.build();
            logger.info('Aws Sqs template started.')
            break;
        case 'rabbitmq':
            logger.info('Starting RabbitMq template.')
            queues = await RabbitMqTemplate.build();
            logger.info('RabbitMq template started.')
            break;
        case 'service-bus':
            logger.info('Starting Azure Service Bus template.')
            queues = await ServiceBusTemplate.build();
            logger.info('Azure Service Bus template started.')
            break;
        default:
            logger.error('Unknown service type: ', serviceType);
            process.exit(-1);
    }

    httpServer = new HttpServer(httpPort);
})();

process.on('SIGTERM', () => {
    process.exit();
});

process.on('exit', async () => {
    if (httpServer) {
        httpServer.stop();
    }
    if (queues) {
        queues.exit(0);
    }
});

