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
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.thingsboard.server.common.stats.StatsCounter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class AbstractJsInvokeServiceTest {

    AbstractJsInvokeService service;
    final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = mock(AbstractJsInvokeService.class, Mockito.RETURNS_DEEP_STUBS);

        ReflectionTestUtils.setField(service, "requestsCounter", mock(StatsCounter.class));
        ReflectionTestUtils.setField(service, "evalCallback", mock(FutureCallback.class));

        // Make sure core checks always pass
        doReturn(true).when(service).isExecEnabled(any());
        doReturn(false).when(service).scriptBodySizeExceeded(anyString());
        doReturn(Futures.immediateFuture(id)).when(service).doEvalScript(any(), any(), anyString(), any(), any(String[].class));

        // Use real implementations
        doCallRealMethod().when(service).eval(any(), any(), any(), any(String[].class));
        doCallRealMethod().when(service).error(anyString());
        doCallRealMethod().when(service).validate(any(), anyString());
    }

    @Test
    void shouldReturnValidationErrorFromJsValidator() throws ExecutionException, InterruptedException {
        String scriptWithAsync = "async function test() {}";

        var future = service.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptWithAsync, "a", "b");
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertTrue(ex.getCause().getMessage().contains("Script must not contain 'async' keyword."));
        assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
        verify(service).isExecEnabled(any());
        verify(service).scriptBodySizeExceeded(any());
    }

    @Test
    void shouldPassValidationAndCallSuperEval() throws ExecutionException, InterruptedException, TimeoutException {
        String validScript = "function test() { return 42; }";
        var result = service.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, validScript, "x", "y");

        assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo(id);
        verify(service, times(1)).isExecEnabled(any());
        verify(service, times(1)).scriptBodySizeExceeded(any());
    }

}
