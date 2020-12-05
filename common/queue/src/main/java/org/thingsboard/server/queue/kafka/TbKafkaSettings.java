/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
@ConfigurationProperties(prefix = "queue.kafka")
@Component
public class TbKafkaSettings {

    @Value("${queue.kafka.bootstrap.servers}")
    private String servers;

    @Value("${queue.kafka.acks}")
    private String acks;

    @Value("${queue.kafka.retries}")
    private int retries;

    @Value("${queue.kafka.batch.size}")
    private int batchSize;

    @Value("${queue.kafka.linger.ms}")
    private long lingerMs;

    @Value("${queue.kafka.buffer.memory}")
    private long bufferMemory;

    @Value("${queue.kafka.replication_factor}")
    @Getter
    private short replicationFactor;

    @Value("${queue.kafka.max_poll_records:8192}")
    @Getter
    private int maxPollRecords;

    @Value("${queue.kafka.max_poll_interval_ms:0}")
    @Getter
    private int maxPollIntervalMs;

    @Value("${queue.kafka.max_partition_fetch_bytes:16777216}")
    @Getter
    private int maxPartitionFetchBytes;

    @Value("${queue.kafka.fetch_max_bytes:134217728}")
    @Getter
    private int fetchMaxBytes;

    @Value("${queue.kafka.use_confluent_cloud:false}")
    private boolean useConfluent;

    @Value("${queue.kafka.confluent.ssl.algorithm}")
    private String sslAlgorithm;

    @Value("${queue.kafka.confluent.sasl.mechanism}")
    private String saslMechanism;

    @Value("${queue.kafka.confluent.sasl.config}")
    private String saslConfig;

    @Value("${queue.kafka.confluent.security.protocol}")
    private String securityProtocol;

    @Setter
    private List<TbKafkaProperty> other;

    public Properties toProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);

        if (useConfluent) {
            props.put("ssl.endpoint.identification.algorithm", sslAlgorithm);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslConfig);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        } else {
            props.put(ProducerConfig.ACKS_CONFIG, acks);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        }

        if (other != null) {
            other.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        }
        return props;
    }

}
