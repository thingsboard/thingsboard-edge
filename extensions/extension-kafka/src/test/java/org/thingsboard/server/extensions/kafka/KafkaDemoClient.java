/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.extensions.kafka;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Starts build-in ZK and Kafka and prints messages from CONSUMER_TOPIC
 */
public class KafkaDemoClient {

    private static final int ZK_PORT = 2222;
    private static final String HOSTNAME = "localhost";
    private static final String ZOOKEEPER_CONNECT = HOSTNAME + ":" + ZK_PORT;
    private static final int KAFKA_PORT = 9092;
    private static final int BROKER_ID = 1;
    private static final String CONSUMER_TOPIC = "test_topic";

    public static void main(String[] args) {
        try {
            startZkLocal();
            startKafkaLocal();
            startConsumer();
        } catch (Exception e) {
            System.out.println("Error running local Kafka broker");
            e.printStackTrace(System.out);
        }
    }

    private static void startConsumer() throws InterruptedException {
        ConsumerIterator<String, String> it = buildConsumer(CONSUMER_TOPIC);
        do {
          if (it.hasNext()) {
              MessageAndMetadata<String, String> messageAndMetadata = it.next();
              System.out.println(String.format("Kafka message [%s]", messageAndMetadata.message()));
          }
          Thread.sleep(100);
        } while (true);
    }

    private static ConsumerIterator<String, String> buildConsumer(String topic) {
        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1);
        ConsumerConfig consumerConfig = new ConsumerConfig(consumerProperties());
        ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        Map<String, List<KafkaStream<String, String>>> consumers = consumerConnector.createMessageStreams(topicCountMap, new StringDecoder(null), new StringDecoder(null));
        KafkaStream<String, String> stream = consumers.get(topic).get(0);
        return stream.iterator();
    }

    private static Properties consumerProperties() {
        Properties props = new Properties();
        props.put("zookeeper.connect", ZOOKEEPER_CONNECT);
        props.put("group.id", "group1");
        props.put("auto.offset.reset", "smallest");
        return props;
    }

    private static void startZkLocal() throws Exception {
        final File zkTmpDir = File.createTempFile("zookeeper", "test");
        if (zkTmpDir.delete() && zkTmpDir.mkdir()) {
            Properties zkProperties = new Properties();
            zkProperties.setProperty("dataDir", zkTmpDir.getAbsolutePath());
            zkProperties.setProperty("clientPort", String.valueOf(ZK_PORT));

            ServerConfig configuration = new ServerConfig();
            QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
            quorumConfiguration.parseProperties(zkProperties);
            configuration.readFrom(quorumConfiguration);

            new Thread() {
                public void run() {
                    try {
                        new ZooKeeperServerMain().runFromConfig(configuration);
                    } catch (IOException e) {
                        System.out.println("Start of Local ZooKeeper Failed");
                        e.printStackTrace(System.err);
                    }
                }
            }.start();
        } else {
            System.out.println("Failed to delete or create data dir for Zookeeper");
        }
    }

    private static void startKafkaLocal() throws Exception {
        final File kafkaTmpLogsDir = File.createTempFile("zookeeper", "test");
        if (kafkaTmpLogsDir.delete() && kafkaTmpLogsDir.mkdir()) {
            Properties kafkaProperties = new Properties();
            kafkaProperties.setProperty("host.name", HOSTNAME);
            kafkaProperties.setProperty("port", String.valueOf(KAFKA_PORT));
            kafkaProperties.setProperty("broker.id", String.valueOf(BROKER_ID));
            kafkaProperties.setProperty("zookeeper.connect", ZOOKEEPER_CONNECT);
            kafkaProperties.setProperty("log.dirs", kafkaTmpLogsDir.getAbsolutePath());
            KafkaConfig kafkaConfig = new KafkaConfig(kafkaProperties);
            KafkaServerStartable kafka = new KafkaServerStartable(kafkaConfig);
            kafka.startup();
        }
    }
}