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
package org.thingsboard.server.controller.plugin;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.service.ws.WebSocketSessionRef;

import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class TbWebSocketHandlerTest {

    TbWebSocketHandler wsHandler;
    NativeWebSocketSession session;
    Session nativeSession;
    RemoteEndpoint.Async asyncRemote;
    WebSocketSessionRef sessionRef;
    int maxMsgQueuePerSession;
    TbWebSocketHandler.SessionMetaData sendHandler;
    ExecutorService executor;

    @BeforeEach
    void setUp() throws IOException {
        maxMsgQueuePerSession = 100;
        executor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        wsHandler = spy(new TbWebSocketHandler());
        willDoNothing().given(wsHandler).close(any(), any());
        session = mock(NativeWebSocketSession.class);
        nativeSession = mock(Session.class);
        willReturn(nativeSession).given(session).getNativeSession(Session.class);
        asyncRemote = mock(RemoteEndpoint.Async.class);
        willReturn(asyncRemote).given(nativeSession).getAsyncRemote();
        sessionRef = mock(WebSocketSessionRef.class, Mockito.RETURNS_DEEP_STUBS); //prevent NPE on logs
        sendHandler = spy(wsHandler.new SessionMetaData(session, sessionRef, maxMsgQueuePerSession));
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void sendHandler_sendMsg_parallel_no_race() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession * 2);
        AtomicInteger sendersCount = new AtomicInteger();
        willAnswer(invocation -> {
            assertThat(sendersCount.incrementAndGet()).as("no race").isEqualTo(1);
            String text = invocation.getArgument(0);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                sendersCount.decrementAndGet();
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //first batch
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        Awaitility.await("first batch processed").atMost(30, TimeUnit.SECONDS).until(() -> finishLatch.getCount() == maxMsgQueuePerSession);
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //second batch - to test pause between big msg batches
        IntStream.range(100, 100 + maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession * 2)).onResult(any());
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
    }

    @Test
    void sendHandler_sendMsg_message_order() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession);
        Collection<String> outputs = new ConcurrentLinkedQueue<>();
        willAnswer(invocation -> {
            String text = invocation.getArgument(0);
            outputs.add(text);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        List<String> inputs = IntStream.range(0, maxMsgQueuePerSession).mapToObj(i -> "msg " + i).collect(Collectors.toList());
        inputs.forEach(s -> sendHandler.sendMsg(s));

        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();
        assertThat(outputs).as("inputs exactly the same as outputs").containsExactlyElementsOf(inputs);

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession)).onResult(any());
    }

    @Test
    void sendHandler_sendMsg_queue_size_exceed() {
        willDoNothing().given(asyncRemote).sendText(anyString(), any()); // send text will never call back, so queue will grow each sendMsg
        sendHandler.sendMsg("first message to stay in-flight all the time during this test");
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        verify(sendHandler, never()).closeSession(any());
        sendHandler.sendMsg("excessive message");
        verify(sendHandler, times(1)).closeSession(eq(new CloseStatus(1008, "Max pending updates limit reached!")));
        verify(asyncRemote, times(1)).sendText(anyString(), any());
    }

}
