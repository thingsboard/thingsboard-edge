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
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;

public class CookieUtilsTest {

    @Test
    public void serializeDeserializeOAuth2AuthorizationRequestTest() {
        HttpCookieOAuth2AuthorizationRequestRepository cookieRequestRepo = new HttpCookieOAuth2AuthorizationRequestRepository();
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        additionalParameters.put("param1", "value1");
        additionalParameters.put("param2", "value2");
        var request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("testUri").clientId("testId")
                .scope("read", "write")
                .additionalParameters(additionalParameters).build();


        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtils.serialize(request));
        Mockito.when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        OAuth2AuthorizationRequest deserializedRequest = cookieRequestRepo.loadAuthorizationRequest(servletRequest);

        assertNotNull(deserializedRequest);
        assertEquals(request.getGrantType(), deserializedRequest.getGrantType());
        assertEquals(request.getAuthorizationUri(), deserializedRequest.getAuthorizationUri());
        assertEquals(request.getClientId(), deserializedRequest.getClientId());
    }

}