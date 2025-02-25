/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.aws.sns;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TbSnsNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("fccfdf2e-6a88-4a94-81dd-5cbb557019cf"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private TbSnsNode node;
    private TbSnsNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AmazonSNS snsClientMock;
    @Mock
    private PublishResult publishResultMock;
    @Mock
    private ResponseMetadata responseMetadataMock;

    @BeforeEach
    void setUp() {
        node = new TbSnsNode();
        config = new TbSnsNodeConfiguration().defaultConfiguration();
        ReflectionTestUtils.setField(node, "snsClient", snsClientMock);
        ReflectionTestUtils.setField(node, "config", config);
    }

    @Test
    void verifyDefaultConfig() {
        assertThat(config.getTopicArnPattern()).isEqualTo("arn:aws:sns:us-east-1:123456789012:MyNewTopic");
        assertThat(config.getAccessKeyId()).isNull();
        assertThat(config.getSecretAccessKey()).isNull();
        assertThat(config.getRegion()).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @MethodSource
    void givenForceAckIsTrueAndTopicNamePattern_whenOnMsg_thenEnqueueForTellNext(String topicName, TbMsgMetaData metaData, String data) {
        ReflectionTestUtils.setField(node, "forceAck", true);
        config.setAccessKeyId("accessKeyId");
        config.setSecretAccessKey("secretAccessKey");
        config.setTopicArnPattern(topicName);
        String messageId = "msgId-1d186a16-80c7-44b3-a245-a1fc835f20c7";
        String requestId = "reqId-bef0799b-dde9-4aa0-855b-86bbafaeaf31";

        given(ctxMock.getExternalCallExecutor()).willReturn(executor);
        given(snsClientMock.publish(any(PublishRequest.class))).willReturn(publishResultMock);
        given(publishResultMock.getMessageId()).willReturn(messageId);
        given(publishResultMock.getSdkResponseMetadata()).willReturn(responseMetadataMock);
        given(responseMetadataMock.getRequestId()).willReturn(requestId);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().ack(msg);
        PublishRequest publishRequest = new PublishRequest()
                .withTopicArn(TbNodeUtils.processPattern(topicName, msg))
                .withMessage(data);
        then(snsClientMock).should().publish(publishRequest);
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(actualMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg)
                .usingRecursiveComparison()
                .ignoringFields("metaData", "ctx")
                .isEqualTo(msg);
        assertThat(actualMsg.getMetaData().getData())
                .hasFieldOrPropertyWithValue("messageId", messageId)
                .hasFieldOrPropertyWithValue("requestId", requestId);
        verifyNoMoreInteractions(ctxMock, snsClientMock, publishResultMock, responseMetadataMock);
    }

    private static Stream<Arguments> givenForceAckIsTrueAndTopicNamePattern_whenOnMsg_thenEnqueueForTellNext() {
        return Stream.of(
                Arguments.of("arn:aws:sns:us-east-1:123456789012:NewTopic", TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT),
                Arguments.of("arn:aws:sns:us-east-1:123456789012:$[msgTopicName]", TbMsgMetaData.EMPTY, "{\"msgTopicName\":\"msg-topic-name\"}"),
                Arguments.of("arn:aws:sns:us-east-1:123456789012:${mdTopicName}", new TbMsgMetaData(Map.of("mdTopicName", "md-topic-name")), TbMsg.EMPTY_JSON_OBJECT)
        );
    }

    @Test
    void givenForceAckIsFalseAndErrorOccursDuringProcessingRequest_whenOnMsg_thenTellFailure() {
        ReflectionTestUtils.setField(node, "forceAck", false);
        ListeningExecutor listeningExecutor = mock(ListeningExecutor.class);
        given(ctxMock.getExternalCallExecutor()).willReturn(listeningExecutor);
        String errorMsg = "Something went wrong";
        ListenableFuture<TbMsg> failedFuture = Futures.immediateFailedFuture(new RuntimeException(errorMsg));
        given(listeningExecutor.executeAsync(any(Callable.class))).willReturn(failedFuture);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should(never()).enqueueForTellNext(any(), any(String.class));
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(actualMsgCaptor.capture(), throwableCaptor.capture());
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg)
                .usingRecursiveComparison()
                .ignoringFields("metaData", "ctx")
                .isEqualTo(msg);
        assertThat(actualMsg.getMetaData().getData())
                .hasFieldOrPropertyWithValue("error", RuntimeException.class + ": " + errorMsg);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
        verifyNoMoreInteractions(ctxMock, snsClientMock);
    }

    @Test
    void givenSnsClientIsNotNull_whenDestroy_thenShutdown() {
        node.destroy();
        then(snsClientMock).should().shutdown();
    }

    @Test
    void givenSnsClientIsNull_whenDestroy_thenVerifyNoInteractions() {
        ReflectionTestUtils.setField(node, "snsClient", null);
        node.destroy();
        then(snsClientMock).shouldHaveNoInteractions();
    }
}
