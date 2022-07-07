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
const {logLevel, Kafka, CompressionTypes, Partitioners} = require('kafkajs');

const config = require('config'),
    JsInvokeMessageProcessor = require('../api/jsInvokeMessageProcessor'),
    logger = require('../config/logger')._logger('kafkaTemplate'),
    KafkaJsWinstonLogCreator = require('../config/logger').KafkaJsWinstonLogCreator;
const replicationFactor = Number(config.get('kafka.replication_factor'));
const topicProperties = config.get('kafka.topic_properties');
const kafkaClientId = config.get('kafka.client_id');
const acks = Number(config.get('kafka.acks'));
const maxBatchSize = Number(config.get('kafka.batch_size'));
const linger = Number(config.get('kafka.linger_ms'));
const requestTimeout = Number(config.get('kafka.requestTimeout'));
const compressionType = (config.get('kafka.compression') === "gzip") ? CompressionTypes.GZIP : CompressionTypes.None;
const partitionsConsumedConcurrently = Number(config.get('kafka.partitions_consumed_concurrently'));

let kafkaClient;
let kafkaAdmin;
let consumer;
let producer;

const configEntries = [];

let batchMessages = [];
let sendLoopInstance;

function KafkaProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        logger.debug('Pending queue response, scriptId: [%s]', scriptId);
        const message = {
            topic: responseTopic,
            messages: [{
                key: scriptId,
                value: rawResponse,
                headers: headers.data
            }]
        };

        await pushMessageToSendLater(message);
    }
}

async function pushMessageToSendLater(message) {
    batchMessages.push(message);
    if (batchMessages.length >= maxBatchSize) {
        await sendMessagesAsBatch(true);
    }
}

function sendLoopWithLinger() {
    if (sendLoopInstance) {
        clearTimeout(sendLoopInstance);
    // } else {
    //     logger.debug("Starting new send loop with linger [%s]", linger)
    }
    sendLoopInstance = setTimeout(sendMessagesAsBatch, linger);
}

async function sendMessagesAsBatch(isImmediately) {
    if (sendLoopInstance) {
        // logger.debug("sendMessagesAsBatch: Clear sendLoop scheduler. Starting new send loop with linger [%s]", linger);
        clearTimeout(sendLoopInstance);
    }
    sendLoopInstance = null;
    if (batchMessages.length > 0) {
        logger.debug('sendMessagesAsBatch, length: [%s], %s', batchMessages.length, isImmediately ? 'immediately' : '');
        const messagesToSend = batchMessages;
        batchMessages = [];
        try {
            await producer.sendBatch({
                topicMessages: messagesToSend,
                acks: acks,
                compression: compressionType
            })
            logger.debug('Response batch sent to kafka, length: [%s]', messagesToSend.length);
        } catch(err) {
            logger.error('Failed batch send to kafka, length: [%s], pending to reprocess msgs', messagesToSend.length);
            logger.error(err.stack);
            batchMessages = messagesToSend.concat(batchMessages);
        }
    }
    sendLoopWithLinger();
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
        const requestTopic = config.get('request_topic');
        const useConfluent = config.get('kafka.use_confluent_cloud');

        logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        logger.info('Kafka Requests Topic: %s', requestTopic);

        let kafkaConfig = {
            brokers: kafkaBootstrapServers.split(','),
            logLevel: logLevel.INFO,
            logCreator: KafkaJsWinstonLogCreator
        };

        if (kafkaClientId) {
            kafkaConfig['clientId'] = kafkaClientId;
        } else {
            logger.warn('KAFKA_CLIENT_ID is undefined. Consider to define the env variable KAFKA_CLIENT_ID');
        }

        kafkaConfig['requestTimeout'] = requestTimeout;

        if (useConfluent) {
            kafkaConfig['sasl'] = {
                mechanism: config.get('kafka.confluent.sasl.mechanism'),
                username: config.get('kafka.confluent.username'),
                password: config.get('kafka.confluent.password')
            };
            kafkaConfig['ssl'] = true;
        }

        kafkaClient = new Kafka(kafkaConfig);

        parseTopicProperties();

        kafkaAdmin = kafkaClient.admin();
        await kafkaAdmin.connect();

        let partitions = 1;

        for (let i = 0; i < configEntries.length; i++) {
            let param = configEntries[i];
            if (param.name === 'partitions') {
                partitions = param.value;
                configEntries.splice(i, 1);
                break;
            }
        }

        let topics = await kafkaAdmin.listTopics();

        if (!topics.includes(requestTopic)) {
            let createRequestTopicResult = await createTopic(requestTopic, partitions);
            if (createRequestTopicResult) {
                logger.info('Created new topic: %s', requestTopic);
            }
        }

        consumer = kafkaClient.consumer({groupId: 'js-executor-group'});
        producer = kafkaClient.producer({ createPartitioner: Partitioners.DefaultPartitioner });

/*
        //producer event instrumentation to debug
        const { CONNECT } = producer.events;
        const removeListenerC = producer.on(CONNECT, e => logger.info(`producer CONNECT`));
        const { DISCONNECT } = producer.events;
        const removeListenerD = producer.on(DISCONNECT, e => logger.info(`producer DISCONNECT`));
        const { REQUEST } = producer.events;
        const removeListenerR = producer.on(REQUEST, e => logger.info(`producer REQUEST ${e.payload.broker}`));
        const { REQUEST_TIMEOUT } = producer.events;
        const removeListenerRT = producer.on(REQUEST_TIMEOUT, e => logger.info(`producer REQUEST_TIMEOUT ${e.payload.broker}`));
        const { REQUEST_QUEUE_SIZE } = producer.events;
        const removeListenerRQS = producer.on(REQUEST_QUEUE_SIZE, e => logger.info(`producer REQUEST_QUEUE_SIZE ${e.payload.broker} size ${e.queueSize}`));
*/

/*
        //consumer event instrumentation to debug
        const removeListeners = {}
        const { FETCH_START } = consumer.events;
        removeListeners[FETCH_START] = consumer.on(FETCH_START, e => logger.info(`consumer FETCH_START`));
        const { FETCH } = consumer.events;
        removeListeners[FETCH] = consumer.on(FETCH, e => logger.info(`consumer FETCH numberOfBatches ${e.payload.numberOfBatches} duration ${e.payload.duration}`));
        const { START_BATCH_PROCESS } = consumer.events;
        removeListeners[START_BATCH_PROCESS] = consumer.on(START_BATCH_PROCESS, e => logger.info(`consumer START_BATCH_PROCESS topic ${e.payload.topic} batchSize ${e.payload.batchSize}`));
        const { END_BATCH_PROCESS } = consumer.events;
        removeListeners[END_BATCH_PROCESS] = consumer.on(END_BATCH_PROCESS, e => logger.info(`consumer END_BATCH_PROCESS topic ${e.payload.topic} batchSize ${e.payload.batchSize}`));
        const { COMMIT_OFFSETS } = consumer.events;
        removeListeners[COMMIT_OFFSETS] = consumer.on(COMMIT_OFFSETS, e => logger.info(`consumer COMMIT_OFFSETS topics ${e.payload.topics}`));
*/

        const { CRASH } = consumer.events;

        consumer.on(CRASH, e => {
            logger.error(`Got consumer CRASH event, should restart: ${e.payload.restart}`);
            if (!e.payload.restart) {
                logger.error('Going to exit due to not retryable error!');
                exit(-1);
            }
        });

        const messageProcessor = new JsInvokeMessageProcessor(new KafkaProducer());
        await consumer.connect();
        await producer.connect();
        sendLoopWithLinger();
        await consumer.subscribe({topic: requestTopic});

        logger.info('Started ThingsBoard JavaScript Executor Microservice.');
        await consumer.run({
            partitionsConsumedConcurrently: partitionsConsumedConcurrently,
            eachMessage: async ({topic, partition, message}) => {
                let headers = message.headers;
                let key = message.key;
                let msg = {};
                msg.key = key.toString('utf8');
                msg.data = message.value;
                msg.headers = {data: headers};
                messageProcessor.onJsInvokeMessage(msg);
            },
        });

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

function createTopic(topic, partitions) {
    return kafkaAdmin.createTopics({
        topics: [{
            topic: topic,
            numPartitions: partitions,
            replicationFactor: replicationFactor,
            configEntries: configEntries
        }]
    });
}

function parseTopicProperties() {
    const props = topicProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        configEntries.push({name: p.substring(0, delimiterPosition), value: p.substring(delimiterPosition + 1)});
    });
}

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);

    if (kafkaAdmin) {
        logger.info('Stopping Kafka Admin...');
        await kafkaAdmin.disconnect();
        logger.info('Kafka Admin stopped.');
    }

    if (consumer) {
        logger.info('Stopping Kafka Consumer...');
        let _consumer = consumer;
        consumer = null;
        try {
            await _consumer.disconnect();
            logger.info('Kafka Consumer stopped.');
            await disconnectProducer();
            process.exit(status);
        } catch (e) {
            logger.info('Kafka Consumer stop error.');
            await disconnectProducer();
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}

async function disconnectProducer() {
    if (producer) {
        logger.info('Stopping Kafka Producer...');
        var _producer = producer;
        producer = null;
        try {
            logger.info('Stopping loop...');
            clearTimeout(sendLoopInstance);
            await sendMessagesAsBatch();
            await _producer.disconnect();
            logger.info('Kafka Producer stopped.');
        } catch (e) {
            logger.info('Kafka Producer stop error.');
        }
    }
}
