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
package org.thingsboard.server.transport.coap.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processTestSubscribeToAttributesUpdates();
    }

    protected void processTestSubscribeToAttributesUpdates() throws Exception {

        CoapClient client = getCoapClient(FeatureType.ATTRIBUTES);

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(latch);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(testCoapCallback);

        latch = new CountDownLatch(1);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        latch.await(3, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(testCoapCallback);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void validateUpdateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(1, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected static class TestCoapCallback implements CoapHandler {

        private final CountDownLatch latch;

        private Integer observe;
        private byte[] payloadBytes;

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public Integer getObserve() {
            return observe;
        }

        private TestCoapCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onLoad(CoapResponse response) {
            assertNotNull(response.getPayload());
            assertEquals(response.getCode(), CoAP.ResponseCode.CONTENT);
            observe = response.getOptions().getObserve();
            payloadBytes = response.getPayload();
            latch.countDown();
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }
}
