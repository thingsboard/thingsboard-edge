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
package org.thingsboard.server.service.integration.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.microsoft.azure.eventhubs.*;
import com.microsoft.azure.sdk.iot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.service.converter.DownLinkMetaData;
import org.thingsboard.server.service.converter.DownlinkData;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.downlink.DownLinkMsg;
import org.thingsboard.server.service.integration.msg.RPCCallIntegrationMsg;
import org.thingsboard.server.service.integration.msg.SharedAttributesUpdateIntegrationMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class AzureEventHubIntegration extends AbstractIntegration<AzureEventHubIntegrationMsg> {

    private IntegrationContext context;
    private EventHubClient ehClient;
    private ServiceClient serviceClient;
    private List<PartitionReceiver> receivers;
    private transient boolean started = false;
    private ExecutorService executorService;
    private List<Future> receiverFutures;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        this.context = params.getContext();
        AzureEventHubClientConfiguration clientConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                AzureEventHubClientConfiguration.class);
        ehClient = initClient(clientConfiguration);
        EventHubRuntimeInformation runtimeInfo = ehClient.getRuntimeInformation().get();
        receivers = new ArrayList<>();
        for (String partitionId : runtimeInfo.getPartitionIds()) {
            PartitionReceiver receiver = ehClient.createReceiverSync(
                    EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                    partitionId,
                    PartitionReceiver.END_OF_STREAM,
                    false);
            receiver.setReceiveTimeout(Duration.ofSeconds(20));
            receivers.add(receiver);
        }
        if (downlinkConverter != null) {
            serviceClient = initServiceClient(clientConfiguration);
        }
        started = true;
        executorService = Executors.newFixedThreadPool(receivers.size());
        receiverFutures = new ArrayList<>();
        receiverFutures.addAll(receivers.stream().map(receiver -> executorService.submit(new ReceiverRunnable(receiver))).collect(Collectors.toList()));
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public void destroy() {
        started = false;
        if (receiverFutures != null) {
            for (Future receiverFuture : receiverFutures) {
                receiverFuture.cancel(true);
            }
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (receivers != null) {
            receivers.forEach(ClientEntity::close);
        }
        if (ehClient != null) {
            ehClient.close();
        }
        if (serviceClient != null) {
            serviceClient.closeAsync();
        }
    }

    class ReceiverRunnable implements Runnable {

        private final PartitionReceiver receiver;

        ReceiverRunnable(PartitionReceiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public void run() {
            while (started) {
                try {
                    Iterable<EventData> events = this.receiver.receiveSync(10);
                    if (events != null) {
                        for (EventData event : events) {
                            process(context, new AzureEventHubIntegrationMsg(event));
                        }
                    }
                } catch (EventHubException e) {
                    log.error("Failed to receive events from Event Hub", e);
                }
            }
        }
    }

    @Override
    public void process(IntegrationContext context, AzureEventHubIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.warn("Failed to apply data converter function", e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.toJson()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    @Override
    public void onSharedAttributeUpdate(IntegrationContext context, SharedAttributesUpdateIntegrationMsg msg) {
        logDownlink(context, "SharedAttributeUpdate", msg);
        if (downlinkConverter != null) {
            DownLinkMsg downLinkMsg = DownLinkMsg.from(msg);
            processDownLinkMsg(context, downLinkMsg);
        }
    }

    @Override
    public void onRPCCall(IntegrationContext context, RPCCallIntegrationMsg msg) {
        logDownlink(context, "RPCCall", msg);
        if (downlinkConverter != null) {
            DownLinkMsg downLinkMsg = DownLinkMsg.from(msg);
            processDownLinkMsg(context, downLinkMsg);
        }
    }

    protected void processDownLinkMsg(IntegrationContext context, DownLinkMsg msg) {
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

    private void doProcess(IntegrationContext context, AzureEventHubIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        msg.getSystemProperties().forEach(
                (key, value) -> {
                    if (value != null) {
                        mdMap.put("sysProp:" + key, value.toString());
                    }
                }
        );
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data", data);
            }
        }
    }

    private boolean doProcessDownLinkMsg(IntegrationContext context, DownLinkMsg msg) throws Exception {
        if (serviceClient == null) {
            return false;
        }
        Map<String, List<Message>> deviceIdToMessage = convertDownLinkMsg(context, msg);
        for (Map.Entry<String, List<Message>> messageEntry : deviceIdToMessage.entrySet()) {
            for (Message message : messageEntry.getValue()) {
                logEventHubDownlink(context, message, messageEntry.getKey(), message.getProperties().get("content-type"));
                serviceClient.sendAsync(messageEntry.getKey(), message);
            }
        }
        return !deviceIdToMessage.isEmpty();
    }

    private Map<String, List<Message>> convertDownLinkMsg(IntegrationContext context, DownLinkMsg msg) throws Exception {
        Map<String, List<Message>> deviceIdToMessage = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        List<DownlinkData> result = downlinkConverter.convertDownLink(context.getConverterContext(), Collections.singletonList(msg), new DownLinkMetaData(mdMap));
        for (DownlinkData data : result) {
            if (!data.isEmpty()) {
                String deviceId = data.getMetadata().get("deviceId");
                if (StringUtils.isEmpty(deviceId)) {
                    continue;
                }
                Message message = new Message(data.getData());
                message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
                message.setMessageId(UUID.randomUUID().toString());
                message.setTo(deviceId);
                message.getProperties().putAll(data.getMetadata());
                message.getProperties().put("content-type", data.getContentType());
                deviceIdToMessage.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(message);
            }
        }
        return deviceIdToMessage;
    }

    private EventHubClient initClient(AzureEventHubClientConfiguration clientConfiguration) throws Exception {
        ConnectionStringBuilder connStr = new ConnectionStringBuilder(clientConfiguration.getNamespaceName(),
                clientConfiguration.getEventHubName(),
                clientConfiguration.getSasKeyName(),
                clientConfiguration.getSasKey());

        CompletableFuture<EventHubClient> ehClientFuture = EventHubClient.createFromConnectionString(connStr.toString());
        EventHubClient ehClient;
        try {
            ehClient = ehClientFuture.get(clientConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            ehClientFuture.cancel(true);
            throw new RuntimeException(String.format("Failed to connect to the Event Hub Endpoint %s within specified timeout.",
                    clientConfiguration.getEventHubName()));
        }
        return ehClient;
    }

    private ServiceClient initServiceClient(AzureEventHubClientConfiguration clientConfiguration) throws Exception {
        if (StringUtils.isEmpty(clientConfiguration.getIotHubName())) {
            return null;
        }
        String iotHubConnectionString =
                String.format("HostName=%s.azure-devices.net;SharedAccessKeyName=%s;SharedAccessKey=%s", clientConfiguration.getIotHubName(),
                        clientConfiguration.getSasKeyName(), clientConfiguration.getSasKey());
        ServiceClient serviceClient = ServiceClient.createFromConnectionString(iotHubConnectionString, IotHubServiceClientProtocol.AMQPS);
        CompletableFuture<Void> serviceClientFuture = serviceClient.openAsync();
        try {
            serviceClientFuture.get(clientConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            serviceClientFuture.cancel(true);
            throw new RuntimeException(String.format("Failed to connect to the IoT Hub %s within specified timeout.",
                    clientConfiguration.getIotHubName()));
        }
        return serviceClient;
    }

    private <T> void logDownlink(IntegrationContext context, String updateType, T msg) {
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, updateType, "JSON", mapper.writeValueAsString(msg), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private void logEventHubDownlink(IntegrationContext context, Message message, String deviceId, String contentType) {
        if (configuration.isDebugMode()) {
            try {
                ObjectNode json = mapper.createObjectNode();
                json.put("deviceId", deviceId);
                json.set("payload", getDownlinkPayloadJson(message, contentType));
                json.set("properties", mapper.valueToTree(message.getProperties()));
                persistDebug(context, "Downlink", "JSON", mapper.writeValueAsString(json), downlinkConverter != null ? "OK" : "FAILURE", null);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private JsonNode getDownlinkPayloadJson(Message message, String contentType) throws IOException {
        if ("JSON".equals(contentType)) {
            return mapper.readTree(message.getBytes());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(message.getBytes(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64Utils.encodeToString(message.getBytes()));
        }
    }

}
