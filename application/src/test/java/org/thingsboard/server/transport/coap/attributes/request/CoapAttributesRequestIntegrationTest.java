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
package org.thingsboard.server.transport.coap.attributes.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class CoapAttributesRequestIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    protected static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Request attribute values from the server", null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processTestRequestAttributesValuesFromTheServer();
    }

    protected void processTestRequestAttributesValuesFromTheServer() throws Exception {
        postAttributes();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> savedAttributeKeys = null;
        while (start <= end) {
            savedAttributeKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (savedAttributeKeys.size() == 5) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(savedAttributeKeys);

        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String featureTokenUrl = getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keys + "&sharedKeys=" + keys;
        client = getCoapClient(featureTokenUrl);

        CoapResponse getAttributesResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
        validateResponse(getAttributesResponse);
    }

    protected void postAttributes() throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client = getCoapClient(FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(POST_ATTRIBUTES_PAYLOAD.getBytes(), MediaTypeRegistry.APPLICATION_JSON);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
    }

    protected void validateResponse(CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        assertEquals(CoAP.ResponseCode.CONTENT, getAttributesResponse.getCode());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(getAttributesResponse.getPayload(), StandardCharsets.UTF_8)));
    }
}
