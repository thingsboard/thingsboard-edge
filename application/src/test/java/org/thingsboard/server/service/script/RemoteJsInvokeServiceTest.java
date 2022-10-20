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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.util.Pair;
import org.thingsboard.js.api.AbstractJsInvokeService;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RemoteJsInvokeServiceTest {

    private RemoteJsInvokeService remoteJsInvokeService;
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> jsRequestTemplate;


    @BeforeEach
    public void beforeEach() {
        remoteJsInvokeService = new RemoteJsInvokeService(Optional.empty(), Optional.empty());
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
    }

    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    @Test
    public void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        String expectedInvocationResult = "scriptInvocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeFunction(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(jsInvokeRequestMade.getScriptHash()).isEqualTo(getScriptHash(scriptId));
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorCode(JsInvokeProtos.JsInvokeErrorCode.NOT_FOUND_ERROR)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        String expectedInvocationResult = "invocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isNotEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestsCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeFunction(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate, times(2)).send(jsRequestsCaptor.capture());

        List<TbProtoJsQueueMsg<RemoteJsRequest>> jsInvokeRequestsMade = jsRequestsCaptor.getAllValues();

        JsInvokeProtos.JsInvokeRequest firstRequestMade = jsInvokeRequestsMade.get(0).getValue().getInvokeRequest();
        assertThat(firstRequestMade.getScriptBody()).isNullOrEmpty();

        JsInvokeProtos.JsInvokeRequest secondRequestMade = jsInvokeRequestsMade.get(1).getValue().getInvokeRequest();
        assertThat(secondRequestMade.getScriptBody()).contains(scriptBody);

        assertThat(jsInvokeRequestsMade.stream().map(TbProtoQueueMsg::getKey).distinct().count()).as("partition keys are same")
                .isOne();

        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    @Test
    public void whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody() throws Exception {
        mockJsEvalResponse();

        TenantId tenantId1 = TenantId.fromUUID(UUID.randomUUID());
        String scriptBody1 = "var msg = { temp: 42, humidity: 77 };\n" +
                "var metadata = { data: 40 };\n" +
                "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                "\n" +
                "return { msg: msg, metadata: metadata, msgType: msgType };";

        Set<String> scriptHashes = new HashSet<>();
        String tenant1Script1Hash = null;
        for (int i = 0; i < 3; i++) {
            UUID scriptUuid = remoteJsInvokeService.eval(tenantId1, JsScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
            tenant1Script1Hash = getScriptHash(scriptUuid);
            scriptHashes.add(tenant1Script1Hash);
        }
        assertThat(scriptHashes).as("Unique scripts ids").size().isOne();

        TenantId tenantId2 = TenantId.fromUUID(UUID.randomUUID());
        UUID scriptUuid = remoteJsInvokeService.eval(tenantId2, JsScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
        String tenant2Script1Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script1Id).isNotEqualTo(tenant1Script1Hash);

        String scriptBody2 = scriptBody1 + ";;";
        scriptUuid = remoteJsInvokeService.eval(tenantId2, JsScriptType.RULE_NODE_SCRIPT, scriptBody2).get();
        String tenant2Script2Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script2Id).isNotEqualTo(tenant2Script1Id);
    }

    @Test
    public void whenReleasingScript_thenCheckForHashUsages() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId1 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        UUID scriptId2 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, JsScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        String scriptHash = getScriptHash(scriptId1);
        assertThat(scriptHash).isEqualTo(getScriptHash(scriptId2));
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setReleaseResponse(JsInvokeProtos.JsReleaseResponse.newBuilder()
                        .setSuccess(true)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        remoteJsInvokeService.release(scriptId1).get();
        verifyNoInteractions(jsRequestTemplate);
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).containsKey(scriptHash);

        remoteJsInvokeService.release(scriptId2).get();
        verify(jsRequestTemplate).send(any());
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).isEmpty();
    }

    private String getScriptHash(UUID scriptUuid) throws Exception {
        Field field = AbstractJsInvokeService.class.getDeclaredField("scriptIdToNameAndHashMap");
        field.setAccessible(true);
        return ((Map<UUID, Pair<String, String>>) field.get(remoteJsInvokeService)).get(scriptUuid).getSecond();
    }

    private void mockJsEvalResponse() {
        doAnswer(methodCall -> Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setCompileResponse(JsInvokeProtos.JsCompileResponse.newBuilder()
                        .setSuccess(true)
                        .setScriptHash(methodCall.<TbProtoQueueMsg<RemoteJsRequest>>getArgument(0).getValue().getCompileRequest().getScriptHash())
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> jsQueueMsg.getValue().hasCompileRequest()));
    }

}
