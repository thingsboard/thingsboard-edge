/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import org.awaitility.Awaitility;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.coapserver.DefaultCoapServerService;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTransportResource;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class CoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    protected static final String POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION = "{\"attribute1\":\"value\",\"attribute2\":false,\"attribute3\":41.0,\"attribute4\":72," +
            "\"attribute5\":{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}}";

    CoapTransportResource coapTransportResource;

    @Autowired
    DefaultCoapServerService defaultCoapServerService;

    @Autowired
    DefaultTransportService defaultTransportService;

    @Before
    public void beforeTest() throws Exception {
        Resource api = defaultCoapServerService.getCoapServer().getRoot().getChild("api");
        coapTransportResource = spy( (CoapTransportResource) api.getChild("v1") );
        api.delete(api.getChild("v1") );
        api.add(coapTransportResource);
        processBeforeTest("Test Subscribe to attribute updates", null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processTestSubscribeToAttributesUpdates(false);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerWithEmptyCurrentStateNotification() throws Exception {
        processTestSubscribeToAttributesUpdates(true);
    }

    protected void processTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }
        client = getCoapClient(FeatureType.ATTRIBUTES);

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback callback = new TestCoapCallback(latch);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, callback);

        latch.await(3, TimeUnit.SECONDS);

        if (emptyCurrentStateNotification) {
            validateEmptyCurrentStateAttributesResponse(callback);
        } else {
            validateCurrentStateAttributesResponse(callback);
        }

        latch = new CountDownLatch(1);
        int expectedObserveCnt = callback.getObserve().intValue() + 1;
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(callback, expectedObserveCnt);


        latch = new CountDownLatch(1);
        int expectedObserveBeforeDeleteCnt = callback.getObserve().intValue() + 1;
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        latch.await(3, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(callback, expectedObserveBeforeDeleteCnt);
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

        awaitClientAfterCancelObserve();
    }

    protected void validateCurrentStateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION), JacksonUtil.toJsonNode(response));
    }

    protected void validateEmptyCurrentStateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals("{}", response);
    }

    protected void validateUpdateAttributesResponse(TestCoapCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(expectedObserveCnt, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesResponse(TestCoapCallback callback, int expectedObserveCnt) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(expectedObserveCnt, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected static class TestCoapCallback implements CoapHandler {

        private final CountDownLatch latch;

        private Integer observe;
        private byte[] payloadBytes;
        private CoAP.ResponseCode responseCode;

        public Integer getObserve() {
            return observe;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CoAP.ResponseCode getResponseCode() {
            return responseCode;
        }

        private TestCoapCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onLoad(CoapResponse response) {
            observe = response.getOptions().getObserve();
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            latch.countDown();
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }

    private void awaitClientAfterCancelObserve() {
        Awaitility.await("awaitClientAfterCancelObserve")
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(()->{
                    log.trace("awaiting defaultTransportService.sessions is empty");
                    return defaultTransportService.sessions.isEmpty();});
    }
}
