/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.service.integration.mqtt.basic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.DownlinkData;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.mqtt.AbstractMqttIntegration;
import org.thingsboard.server.service.integration.mqtt.BasicMqttIntegrationMsg;
import org.thingsboard.server.service.integration.mqtt.MqttTopicFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by ashvayka on 25.12.17.
 */
@Slf4j
public class BasicMqttIntegration extends AbstractMqttIntegration<BasicMqttIntegrationMsg> {

    protected String downlinkTopicPattern = "${topic}";

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);

        List<MqttTopicFilter> topics = mapper.readValue(mapper.writeValueAsString(configuration.getConfiguration().get("topicFilters")),
                new TypeReference<List<MqttTopicFilter>>() {
                });

        for (MqttTopicFilter topicFilter : topics) {
            mqttClient.on(topicFilter.getFilter(), (topic, data) ->
                    process(params.getContext(), new BasicMqttIntegrationMsg(topic, data)), MqttQoS.valueOf(topicFilter.getQos()));
        }

        if (configuration.getConfiguration().has("downlinkTopicPattern")) {
            this.downlinkTopicPattern = configuration.getConfiguration().get("downlinkTopicPattern").asText();
        }
    }

    @Override
    protected void doProcess(IntegrationContext context, BasicMqttIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.put("topic", msg.getTopic());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data", data);
            }
        }
    }

    @Override
    protected boolean doProcessDownLinkMsg(IntegrationContext context, DownLinkMsg msg) throws Exception {
        Map<String, List<DownlinkData>> topicToDataMap = convertDownLinkMsg(context, msg);
        for (Map.Entry<String, List<DownlinkData>> topicEntry : topicToDataMap.entrySet()) {
            for (DownlinkData data : topicEntry.getValue()) {
                String topic = topicEntry.getKey();
                logMqttDownlink(context, topic, data);
                mqttClient.publish(topic, Unpooled.wrappedBuffer(data.getData()), MqttQoS.AT_LEAST_ONCE);
            }
        }
        return !topicToDataMap.isEmpty();
    }

    protected Map<String, List<DownlinkData>> convertDownLinkMsg(IntegrationContext context, DownLinkMsg msg) throws Exception {
        Map<String, List<DownlinkData>> topicToDataMap = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getConverterContext(), Collections.singletonList(msg), new DownLinkMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String downlinkTopic = compileDownlinkTopic(data.getMetadata());
                topicToDataMap.computeIfAbsent(downlinkTopic, k -> new ArrayList<>()).add(data);
            }
        }
        return topicToDataMap;
    }

    private String compileDownlinkTopic(Map<String,String> md) {
        if (md != null) {
            String result = downlinkTopicPattern;
            for (Map.Entry<String,String> mdEntry : md.entrySet()) {
                String key = "${"+mdEntry.getKey()+"}";
                result = result.replace(key, mdEntry.getValue());
            }
            return result;
        }
        return downlinkTopicPattern;
    }

    private void logMqttDownlink(IntegrationContext context, String topic, DownlinkData data) {
        if (configuration.isDebugMode()) {
            try {
                ObjectNode json = mapper.createObjectNode();
                json.put("topic", topic);
                json.set("payload", getDownlinkPayloadJson(data));
                persistDebug(context, "Downlink", "JSON", mapper.writeValueAsString(json), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private JsonNode getDownlinkPayloadJson(DownlinkData data) throws IOException {
        String contentType = data.getContentType();
        if ("JSON".equals(contentType)) {
            return mapper.readTree(data.getData());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(data.getData(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64Utils.encodeToString(data.getData()));
        }
    }

}
