/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.kafka.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.InterruptException;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.kafka.AbstractKafkaIntegration;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicKafkaIntegration extends AbstractKafkaIntegration<BasicKafkaIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            return;
        }

        initConsumer(kafkaConsumerConfiguration);

        loopExecutor.submit(() -> {
            while (!stopped) {
                kafkaLock.lock();
                try {
                    ConsumerRecords<String, String> requests = kafkaConsumer.poll(Duration.ofMillis(pollInterval));
                    requests.forEach(request -> process(new BasicKafkaIntegrationMsg(request.value())));
                } catch (InterruptException ie) {
                    if (!stopped) {
                        log.warn("[{}] Fetching data from kafka was interrupted.", this.configuration.getId(), ie);
                    }
                } catch (Throwable e) {
                    log.warn("[{}] Failed to obtain messages from queue.", this.configuration.getId(), e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                } finally {
                    kafkaLock.unlock();
                }
            }
        });
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicKafkaIntegrationMsg msg) throws Exception {
        byte[] bytes = msg.getMsg().getBytes();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, bytes, new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }
}
