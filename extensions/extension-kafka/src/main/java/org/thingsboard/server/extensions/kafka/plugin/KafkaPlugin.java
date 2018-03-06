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
package org.thingsboard.server.extensions.kafka.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.kafka.action.KafkaPluginAction;

import java.util.Properties;

@Plugin(name = "Kafka Plugin", actions = {KafkaPluginAction.class},
        descriptor = "KafkaPluginDescriptor.json", configuration = KafkaPluginConfiguration.class)
@Slf4j
public class KafkaPlugin extends AbstractPlugin<KafkaPluginConfiguration> {

    private KafkaMsgHandler handler;
    private Producer<?, String> producer;
    private final Properties properties = new Properties();

    @Override
    public void init(KafkaPluginConfiguration configuration) {
        properties.put("bootstrap.servers", configuration.getBootstrapServers());
        properties.put("value.serializer", configuration.getValueSerializer());
        properties.put("key.serializer", configuration.getKeySerializer());
        properties.put("acks", String.valueOf(configuration.getAcks()));
        properties.put("retries", configuration.getRetries());
        properties.put("batch.size", configuration.getBatchSize());
        properties.put("linger.ms", configuration.getLinger());
        properties.put("buffer.memory", configuration.getBufferMemory());
        if (configuration.getOtherProperties() != null) {
            configuration.getOtherProperties()
                    .forEach(p -> properties.put(p.getKey(), p.getValue()));
        }
        init();
    }

    private void init() {
        try {
            this.producer = new KafkaProducer<>(properties);
            this.handler = new KafkaMsgHandler(producer);
        } catch (Exception e) {
            log.error("Failed to start kafka producer", e);
            throw new RuntimeException(e);
        }
    }

    private void destroy() {
        try {
            this.handler = null;
            this.producer.close();
        } catch (Exception e) {
            log.error("Failed to close producer during destroy()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
