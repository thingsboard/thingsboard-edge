/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.coap.rpc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.junit.Assert;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapServerSideRpcIntegrationTest extends AbstractCoapIntegrationTest {

    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected Long asyncContextTimeoutToUseRpcPlugin;

    protected void processBeforeTest(String deviceName, CoapDeviceType coapDeviceType, TransportPayloadType payloadType) throws Exception {
        super.processBeforeTest(deviceName, coapDeviceType, payloadType);
        asyncContextTimeoutToUseRpcPlugin = 10000L;
    }

    protected void processOneWayRpcTest() throws Exception {
        CoapClient client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(client, latch, true);

        Request request = Request.newGet().setObserve();
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(0, testCoapCallback.getObserve().intValue());
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processTwoWayRpcTest() throws Exception {
        CoapClient client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(client, latch, false);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String expected = "{\"value1\":\"A\",\"value2\":\"B\"}";

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        assertEquals(expected, result);
        assertEquals(0, testCoapCallback.getObserve().intValue());
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

//        // TODO: 3/11/21 Fix test to validate next RPC
//        latch = new CountDownLatch(1);
//
//        result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
//        latch.await(3, TimeUnit.SECONDS);
//
//        assertEquals(expected, result);
//        assertEquals(1, testCoapCallback.getObserve().intValue());
    }

    protected void processOnLoadResponse(CoapResponse response, CoapClient client, Integer observe, CountDownLatch latch) {
        client.setURI(getRpcResponseFeatureTokenUrl(accessToken, observe));
        client.post(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                latch.countDown();
            }

            @Override
            public void onError() {
                log.warn("Command Response Ack Error, No connect");
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    protected String getRpcResponseFeatureTokenUrl(String token, int requestId) {
        return COAP_BASE_URL + token + "/" + FeatureType.RPC.name().toLowerCase() + "/" + requestId;
    }

    private class TestCoapCallback implements CoapHandler {

        private final CoapClient client;
        private final CountDownLatch latch;
        private final boolean isOneWayRpc;

        public Integer getObserve() {
            return observe;
        }

        private Integer observe;

        private TestCoapCallback(CoapClient client, CountDownLatch latch, boolean isOneWayRpc) {
            this.client = client;
            this.latch = latch;
            this.isOneWayRpc = isOneWayRpc;
        }

        @Override
        public void onLoad(CoapResponse response) {
            log.warn("coap response: {}, {}", response, response.getCode());
            assertNotNull(response.getPayload());
            assertEquals(response.getCode(), CoAP.ResponseCode.CONTENT);
            observe = response.getOptions().getObserve();
            if (!isOneWayRpc) {
                processOnLoadResponse(response, client, observe, latch);
            } else {
                latch.countDown();
            }
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }

}
