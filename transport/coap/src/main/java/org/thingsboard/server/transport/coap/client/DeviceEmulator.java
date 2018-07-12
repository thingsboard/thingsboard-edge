/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.transport.coap.client;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
public class DeviceEmulator {

    public static final String SN = "SN-" + new Random().nextInt(1000);
    public static final String MODEL = "Model " + new Random().nextInt(1000);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String host;
    private final int port;
    private final String token;

    private CoapClient attributesClient;
    private CoapClient telemetryClient;
    private CoapClient rpcClient;
    private String[] keys;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private AtomicInteger seq = new AtomicInteger(100);

    private DeviceEmulator(String host, int port, String token, String keys) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.attributesClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.ATTRIBUTES));
        this.telemetryClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.TELEMETRY));
        this.rpcClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.RPC));
        this.keys = keys.split(",");
    }

    public void start() {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    sendObserveRequest(rpcClient);
                    while (!Thread.interrupted()) {


                        sendRequest(attributesClient, createAttributesRequest());
                        sendRequest(telemetryClient, createTelemetryRequest());

                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.error("Error occurred while sending COAP requests", e);
                }
            }

            private void sendRequest(CoapClient client, JsonNode request) throws JsonProcessingException {
                CoapResponse telemetryResponse = client.setTimeout(60000).post(mapper.writeValueAsString(request),
                        MediaTypeRegistry.APPLICATION_JSON);
                log.info("Response: {}, {}", telemetryResponse.getCode(), telemetryResponse.getResponseText());
            }

            private void sendObserveRequest(CoapClient client) throws JsonProcessingException {
                client.observe(new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse coapResponse) {
                        log.info("Command: {}, {}", coapResponse.getCode(), coapResponse.getResponseText());
                        try {
                            JsonNode node = mapper.readTree(coapResponse.getResponseText());
                            int requestId = node.get("id").asInt();
                            String method = node.get("method").asText();
                            ObjectNode params = (ObjectNode) node.get("params");
                            ObjectNode response = mapper.createObjectNode();
                            response.put("id", requestId);
                            response.set("response", params);
                            log.info("Command Response: {}, {}", requestId, mapper.writeValueAsString(response));
                            CoapClient commandResponseClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.RPC));
                            commandResponseClient.post(new CoapHandler() {
                                @Override
                                public void onLoad(CoapResponse response) {
                                    log.info("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                                }

                                @Override
                                public void onError() {
                                    //Do nothing
                                }
                            }, mapper.writeValueAsString(response), MediaTypeRegistry.APPLICATION_JSON);

                        } catch (IOException e) {
                            log.error("Error occurred while processing COAP response", e);
                        }
                    }

                    @Override
                    public void onError() {
                        //Do nothing
                    }
                });
            }

        });
    }

    private ObjectNode createAttributesRequest() {
        ObjectNode element = mapper.createObjectNode();
        element.put("serialNumber", SN);
        element.put("model", MODEL);
        return element;
    }

    private ArrayNode createTelemetryRequest() {
        ArrayNode rootNode = mapper.createArrayNode();
        for (String key : keys) {
            ObjectNode element = mapper.createObjectNode();
            element.put(key, seq.incrementAndGet());
            rootNode.add(element);
        }
        return rootNode;
    }

    protected void stop() {
        executor.shutdownNow();
    }

    public static void main(String args[]) {
        if (args.length != 4) {
            System.out.println("Usage: java -jar " + DeviceEmulator.class.getSimpleName() + ".jar host port device_token keys");
        }
        final DeviceEmulator emulator = new DeviceEmulator(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        emulator.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                emulator.stop();
            }
        });
    }


    private String getFeatureTokenUrl(String host, int port, String token, FeatureType featureType) {
        return getBaseUrl(host, port) + token + "/" + featureType.name().toLowerCase();
    }

    private String getBaseUrl(String host, int port) {
        return "coap://" + host + ":" + port + "/api/v1/";
    }

}
