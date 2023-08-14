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
package org.thingsboard.server.transport.coap;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.coap.client.CoapClientContext;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoapTransportResourceTest {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String TELEMETRY = "telemetry";
    private static final String ATTRIBUTES = "attributes";
    private static final String RPC = "rpc";
    private static final String CLAIM = "claim";
    private static final String PROVISION = "provision";
    private static final String GET_ATTRIBUTES_URI_QUERY = "clientKeys=attribute1,attribute2&sharedKeys=shared1,shared2";

    private static final Random RANDOM = new Random();

    private static CoapTransportResource coapTransportResource;

    @BeforeAll
    static void setUp() {

        var ctxMock = mock(CoapTransportContext.class);
        var coapServerServiceMock = mock(CoapServerService.class);
        var transportServiceMock = mock(TransportService.class);
        var clientContextMock = mock(CoapClientContext.class);
        var schedulerComponentMock = mock(SchedulerComponent.class);

        when(ctxMock.getTransportService()).thenReturn(transportServiceMock);
        when(ctxMock.getClientContext()).thenReturn(clientContextMock);
        when(ctxMock.getSessionReportTimeout()).thenReturn(1L);
        when(ctxMock.getScheduler()).thenReturn(schedulerComponentMock);

        coapTransportResource = new CoapTransportResource(ctxMock, coapServerServiceMock, V1);
    }

    @ParameterizedTest
    @MethodSource("provideRequestAndFeatureType")
    void givenRequest_whenGetFeatureType_thenReturnedExpectedFeatureType(Request request, FeatureType expectedFeatureType) {
        var featureTypeOptional = coapTransportResource.getFeatureType(request);

        assertTrue(featureTypeOptional.isPresent(), "Optional<FeatureType> is empty");
        assertEquals(expectedFeatureType, featureTypeOptional.get(), "Feature type is invalid");
    }

    static Stream<Arguments> provideRequestAndFeatureType() {
        return Stream.of(
                // accessToken based tests
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, TELEMETRY), FeatureType.TELEMETRY),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toGetAttributesAccessTokenRequest(), FeatureType.ATTRIBUTES),
                Arguments.of(toAccessTokenRequest(CoAP.Code.GET, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toAccessTokenRequest(CoAP.Code.GET, RPC), FeatureType.RPC),
                Arguments.of(toRpcResponseAccessTokenRequest(), FeatureType.RPC),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, RPC), FeatureType.RPC),
                Arguments.of(toAccessTokenRequest(CoAP.Code.POST, CLAIM), FeatureType.CLAIM),
                // certificate based tests
                Arguments.of(toCertificateRequest(CoAP.Code.POST, TELEMETRY), FeatureType.TELEMETRY),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toGetAttributesCertificateRequest(), FeatureType.ATTRIBUTES),
                Arguments.of(toCertificateRequest(CoAP.Code.GET, ATTRIBUTES), FeatureType.ATTRIBUTES),
                Arguments.of(toCertificateRequest(CoAP.Code.GET, RPC), FeatureType.RPC),
                Arguments.of(toRpcResponseCertificateRequest(), FeatureType.RPC),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, RPC), FeatureType.RPC),
                Arguments.of(toCertificateRequest(CoAP.Code.POST, CLAIM), FeatureType.CLAIM),
                // provision request
                Arguments.of(toProvisionRequest(), FeatureType.PROVISION)
        );
    }

    private static Request toAccessTokenRequest(CoAP.Code method, String featureType) {
        return getAccessTokenRequest(method, featureType, null, null);
    }

    private static Request toGetAttributesAccessTokenRequest() {
        return getAccessTokenRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    private static Request toRpcResponseAccessTokenRequest() {
        return getAccessTokenRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    private static Request toCertificateRequest(CoAP.Code method, String featureType) {
        return getCertificateRequest(method, featureType, null, null);
    }

    private static Request toGetAttributesCertificateRequest() {
        return getCertificateRequest(CoAP.Code.GET, CoapTransportResourceTest.ATTRIBUTES, null, CoapTransportResourceTest.GET_ATTRIBUTES_URI_QUERY);
    }

    private static Request toRpcResponseCertificateRequest() {
        return getCertificateRequest(CoAP.Code.POST, CoapTransportResourceTest.RPC, RANDOM.nextInt(100), null);
    }

    private static Request getAccessTokenRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, false, requestId, uriQuery);
    }

    private static Request getCertificateRequest(CoAP.Code method, String featureType, Integer requestId, String uriQuery) {
        return getRequest(method, featureType, true, requestId, uriQuery);
    }

    private static Request toProvisionRequest() {
        return getRequest(CoAP.Code.POST, PROVISION, true, null, null);
    }

    private static Request getRequest(CoAP.Code method, String featureType, boolean dtls, Integer requestId, String uriQuery) {
        var request = new Request(method);
        var options = new OptionSet();
        options.addUriPath(API);
        options.addUriPath(V1);
        if (!dtls) {
            options.addUriPath(StringUtils.randomAlphanumeric(20));
        }
        options.addUriPath(featureType);
        if (requestId != null) {
            options.addUriPath(String.valueOf(requestId));
        }
        if (uriQuery != null) {
            options.setUriQuery(uriQuery);
        }
        request.setOptions(options);
        return request;
    }

}
