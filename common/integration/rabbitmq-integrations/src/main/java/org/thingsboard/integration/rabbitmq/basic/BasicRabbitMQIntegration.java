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
package org.thingsboard.integration.rabbitmq.basic;

import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.rabbitmq.AbstractRabbitMQIntegration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicRabbitMQIntegration extends AbstractRabbitMQIntegration<BasicRabbitMQIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            return;
        }

        initConsumer(rabbitMQConsumerConfiguration);

        loopExecutor.submit(() -> {
            while (!stopped) {
                rabbitMQLock.lock();
                try {
                    List<GetResponse> requests = doPoll();
                    requests.forEach(request -> process(new BasicRabbitMQIntegrationMsg(new String(request.getBody(), StandardCharsets.UTF_8))));
                    doCommit();
                } catch (Throwable e) {
                    log.warn("[{}] Failed to obtain messages from queue.", this.configuration.getId(), e);
                    try {
                        Thread.sleep(rabbitMQConsumerConfiguration.getPollPeriod());
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                } finally {
                    rabbitMQLock.unlock();
                }
            }
        });
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicRabbitMQIntegrationMsg msg) throws Exception {
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
