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
package org.thingsboard.rule.engine.rest;


import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbHttpClientTest {

    EventLoopGroup eventLoop;
    TbHttpClient client;

    private final String ENDPOINT_URL = "http://localhost/api?data=[{\\\"test\\\":\\\"test\\\"}]";
    private final String GET_METHOD = "GET";

    @Before
    public void setUp() throws Exception {
        client = mock(TbHttpClient.class);
        willCallRealMethod().given(client).getSharedOrCreateEventLoopGroup(any());
    }

    @After
    public void tearDown() throws Exception {
        if (eventLoop != null) {
            eventLoop.shutdownGracefully();
        }
    }

    @Test
    public void givenSharedEventLoop_whenGetEventLoop_ThenReturnShared() {
        eventLoop = mock(EventLoopGroup.class);
        assertThat(client.getSharedOrCreateEventLoopGroup(eventLoop), is(eventLoop));
    }

    @Test
    public void givenNull_whenGetEventLoop_ThenReturnShared() {
        eventLoop = client.getSharedOrCreateEventLoopGroup(null);
        assertThat(eventLoop, instanceOf(NioEventLoopGroup.class));
    }

    @Test
    public void testProcessMessageWithJsonInUrlVariable() throws Exception {
        var config = new TbRestApiCallNodeConfiguration()
                .defaultConfiguration();
        config.setRequestMethod(GET_METHOD);
        config.setRestEndpointUrlPattern(ENDPOINT_URL);
        config.setUseSimpleClientHttpFactory(true);

        var asyncRestTemplate = mock(AsyncRestTemplate.class);
        var uriCaptor = ArgumentCaptor.forClass(URI.class);

        var responseEntity = new ResponseEntity<>(
                "{}",
                new HttpHeaders(),
                HttpStatus.OK
        );

        when(asyncRestTemplate.exchange(
                uriCaptor.capture(),
                any(),
                any(),
                eq(String.class)
        )).thenReturn(new AsyncResult<>(responseEntity));

        var httpClient = new TbHttpClient(config, eventLoop);
        httpClient.setHttpClient(asyncRestTemplate);

        var msg = TbMsg.newMsg(
                "Main", "GET", new DeviceId(EntityId.NULL_UUID),
                TbMsgMetaData.EMPTY, "{}"
        );
        var successMsg = TbMsg.newMsg(
                "SUCCESS", msg.getOriginator(),
                msg.getMetaData(), msg.getData()
        );

        var ctx = mock(TbContext.class);
        when(ctx.transformMsg(
                        eq(msg), eq(msg.getType()),
                        eq(msg.getOriginator()),
                        eq(msg.getMetaData()),
                        eq(msg.getData())
                )).thenReturn(successMsg);

        httpClient.processMessage(ctx, msg);

        verify(ctx, times(1)).transformMsg(
                eq(msg), eq(msg.getType()),
                eq(msg.getOriginator()),
                eq(msg.getMetaData()),
                eq(msg.getData())
        );
        verify(ctx, times(1))
                .tellSuccess(eq(successMsg));

        URI uri = UriComponentsBuilder.fromUriString(ENDPOINT_URL).build().encode().toUri();
        Assert.assertEquals("URI encoding was not performed!!", uri, uriCaptor.getValue());
    }
}