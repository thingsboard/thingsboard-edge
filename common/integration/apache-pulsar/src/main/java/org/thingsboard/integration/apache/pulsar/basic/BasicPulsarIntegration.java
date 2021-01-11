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
package org.thingsboard.integration.apache.pulsar.basic;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClientException;
import org.thingsboard.integration.apache.pulsar.AbstractPulsarIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicPulsarIntegration extends AbstractPulsarIntegration<BasicPulsarIntegrationMsg> {

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        if (!this.configuration.isEnabled()) {
            return;
        }

        stopped = false;

        loopExecutor.submit(() -> {
            while (!stopped) {
                try {
                    Messages<byte[]> messages = pulsarConsumer.batchReceive();
                    messages.forEach(msg -> process(new BasicPulsarIntegrationMsg(msg.getData())));
                    pulsarConsumer.acknowledge(messages);
                } catch (PulsarClientException e) {
                    if (!stopped) {
                        log.warn("[{}] Failed to receive messages from Apache Pulsar integration.", this.configuration.getId(), e);
                    }
                }
            }
        });
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicPulsarIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getMsg(), new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
        }
    }
}
