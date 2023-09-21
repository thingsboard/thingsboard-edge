/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class AzureServiceBusIntegration extends AbstractIntegration<AzureServiceBusIntegrationMsg> {

    private AzureServiceBusClientConfiguration clientConfiguration;
    private ServiceBusSenderClient senderClient;
    private ServiceBusProcessorClient receiverClient;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        clientConfiguration = getClientConfiguration(configuration, AzureServiceBusClientConfiguration.class);

        initReceiver(clientConfiguration);

        if (downlinkConverter != null) {
            senderClient = initSenderClient(clientConfiguration);
        }
    }

    @Override
    public void destroy() {
        if (senderClient != null) {
            senderClient.close();
        }
        if (receiverClient != null) {
            receiverClient.close();
        }
    }

    @Override
    public void process(AzureServiceBusIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getDefaultUplinkContentType(), JacksonUtil.toString(msg.toJson()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg downlink) {
        TbMsg msg = downlink.getTbMsg();
        logDownlink(context, "Downlink: " + msg.getType(), msg);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msg);
        }
    }

    @Override
    public void checkConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        var configuration = getClientConfiguration(
                integration.getConfiguration().get("clientConfiguration"),
                AzureServiceBusClientConfiguration.class
        );

        try (var consumerClient = buildConsumerClient(configuration)) {
            log.debug("Service bus consumer connection checked");
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            if (doProcessDownLinkMsg(context, msg)) {
                integrationStatistics.incMessagesProcessed();
            }
        } catch (Exception e) {
            log.warn("Failed to process downLink message", e);
            exception = e;
            status = "ERROR";
        }
        reportDownlinkError(context, msg, status, exception);
    }

    private void doProcess(IntegrationContext context, AzureServiceBusIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        msg.getSystemProperties().forEach(
                (key, value) -> {
                    if (value != null) {
                        mdMap.put("sysProp:" + key, value.toString());
                    }
                }
        );
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getDefaultUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.trace("[{}] Processing uplink data", data);
            }
        }
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.debug("Processing message. Session: [{}], Sequence #: [{}]. Contents: [{}]", message.getMessageId(),
                message.getSequenceNumber(), message.getBody());
        process(new AzureServiceBusIntegrationMsg(context));
    }

    private boolean doProcessDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        if (senderClient == null) {
            return false;
        }
        Map<String, List<ServiceBusMessage>> deviceIdToMessage = convertDownLinkMsg(context, msg);
        ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();
        for (Map.Entry<String, List<ServiceBusMessage>> messageEntry : deviceIdToMessage.entrySet()) {
            for (ServiceBusMessage message : messageEntry.getValue()) {
                logServiceBusDownlink(context, message, messageEntry.getKey(), message.getContentType());
                if (messageBatch.tryAddMessage(message)) {
                    continue;
                }

                senderClient.sendMessages(messageBatch);
                log.debug("Sent a batch of messages to the queue [{}]", clientConfiguration.getDownlinkTopicName());

                messageBatch = senderClient.createMessageBatch();
                if (!messageBatch.tryAddMessage(message)) {
                    log.error("Message is too large for an empty batch. Skipping. Max size: [{}].", messageBatch.getMaxSizeInBytes());
                }
                log.debug("[{}][{}] Sent downlink [{}] successfully.", configuration.getId(), message.getTo(), message.getMessageId());
            }
        }
        if (messageBatch.getCount() > 0) {
            senderClient.sendMessages(messageBatch);
            log.debug("Sent a batch of messages to the queue [{}]", clientConfiguration.getDownlinkTopicName());
        }
        return !deviceIdToMessage.isEmpty();
    }

    private Map<String, List<ServiceBusMessage>> convertDownLinkMsg(IntegrationContext context, TbMsg msg) throws Exception {
        Map<String, List<ServiceBusMessage>> deviceIdToMessage = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getDownlinkConverterContext(), Collections.singletonList(msg), new IntegrationMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String deviceId = data.getMetadata().get("deviceId");
                if (StringUtils.isEmpty(deviceId)) {
                    continue;
                }
                ServiceBusMessage message = new ServiceBusMessage(data.getData());
                message.setMessageId(UUID.randomUUID().toString());
                message.setTo(deviceId);
                message.getApplicationProperties().putAll(data.getMetadata());
                message.setContentType(data.getContentType());
                deviceIdToMessage.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(message);
            }
        }
        return deviceIdToMessage;
    }

    private void initReceiver(AzureServiceBusClientConfiguration configuration) {
        receiverClient = buildConsumerClient(configuration);
        receiverClient.start();
    }

    private ServiceBusProcessorClient buildConsumerClient(AzureServiceBusClientConfiguration configuration) {
        return new ServiceBusClientBuilder()
                .connectionString(configuration.getConnectionString())
                .processor()
                .topicName(configuration.getTopicName())
                .subscriptionName(configuration.getSubName())
                .processMessage(this::processMessage)
                .processError(error -> log.error("It was trouble when receiving: " + error.getException().getMessage()))
                .buildProcessorClient();
    }

    private ServiceBusSenderClient initSenderClient(AzureServiceBusClientConfiguration clientConfiguration) throws Exception {
        return new ServiceBusClientBuilder()
                .connectionString(clientConfiguration.getDownlinkConnectionString())
                .sender()
                .topicName(clientConfiguration.getDownlinkTopicName())
                .buildClient();
    }

    private void logServiceBusDownlink(IntegrationContext context, ServiceBusMessage message, String deviceId, String contentType) {
        if (configuration.isDebugMode()) {
            try {
                ObjectNode json = JacksonUtil.newObjectNode();
                json.put("deviceId", deviceId);
                json.set("payload", getDownlinkPayloadJson(message, contentType));
                json.set("properties", JacksonUtil.valueToTree(message.getApplicationProperties()));
                persistDebug(context, "Downlink", "JSON", JacksonUtil.toString(json), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private JsonNode getDownlinkPayloadJson(ServiceBusMessage message, String contentType) throws IOException {
        if ("JSON".equals(contentType)) {
            return JacksonUtil.fromBytes(message.getBody().toBytes());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(message.getBody().toBytes(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64Utils.encodeToString(message.getBody().toBytes()));
        }
    }
}
