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

import com.microsoft.azure.eventhubs.*;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

@Slf4j
public class AzureEventHubIntegration extends AbstractIntegration<AzureEventHubIntegrationMsg> implements Runnable {

    private IntegrationContext context;
    private EventHubClient ehClient;
    private PartitionReceiver receiver;
    private transient boolean started = false;
    private ExecutorService executorService;
    private Future receiverFuture;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        this.context = params.getContext();
        AzureEventHubClientConfiguration clientConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                AzureEventHubClientConfiguration.class);
        ehClient = initClient(clientConfiguration);
        String partitionId = "0";
        receiver = ehClient.createReceiverSync(
                EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                partitionId,
                PartitionReceiver.END_OF_STREAM,
                false);
        receiver.setReceiveTimeout(Duration.ofSeconds(20));
        started = true;
        executorService = Executors.newSingleThreadExecutor();
        receiverFuture = executorService.submit(this);
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public void destroy() {
        started = false;
        if (receiverFuture != null) {
            receiverFuture.cancel(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (receiver != null) {
            receiver.close();
        }
        if (ehClient != null) {
            ehClient.close();
        }
    }

    @Override
    public void run() {
        while (started) {
            try {
                Iterable<EventData> events = receiver.receiveSync(10);
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

}
