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
package org.thingsboard.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class NoSecObserveClient {

    private static final long INFINIT_EXCHANGE_LIFETIME = 0L;

    private CoapClient coapClient;
    private CoapObserveRelation observeRelation;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    private CountDownLatch latch;

    public NoSecObserveClient(String host, int port, String accessToken) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, accessToken));
        this.coapClient = new CoapClient(uri);
        coapClient.setTimeout(INFINIT_EXCHANGE_LIFETIME);
        this.latch = new CountDownLatch(5);
    }

    public void start() {
        executor.submit(() -> {
            try {
                Request request = Request.newGet();
                request.setObserve();
                observeRelation = coapClient.observe(request, new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse response) {
                        String responseText = response.getResponseText();
                        CoAP.ResponseCode code = response.getCode();
                        Integer observe = response.getOptions().getObserve();
                        log.info("CoAP Response received! " +
                                        "responseText: {}, " +
                                        "code: {}, " +
                                        "observe seq number: {}",
                                responseText,
                                code,
                                observe);
                        latch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.error("Ack error!");
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred while sending COAP requests: ");
            }
        });
        try {
            latch.await();
            observeRelation.proactiveCancel();
        } catch (InterruptedException e) {
            log.error("Error occurred: ", e);
        }
    }

    private String getFutureUrl(String host, Integer port, String accessToken) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes";
    }

    public static void main(String[] args) throws URISyntaxException {
        log.info("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecObserveClient " +
                "host port accessToken");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];

        final NoSecObserveClient client = new NoSecObserveClient(host, port, accessToken);
        client.start();
    }
}
