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
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TbMsgPackCallbackTest {

    TenantId tenantId;
    UUID msgId;
    TbMsgPackProcessingContext ctx;
    TbMsgPackCallback callback;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        msgId = UUID.randomUUID();
        ctx = mock(TbMsgPackProcessingContext.class);
        callback = spy(new TbMsgPackCallback(msgId, tenantId, ctx));
    }

    private static Stream<Arguments> testOnFailure_NotRateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("rule engine no cause")),
                Arguments.of(new RuleEngineException("rule engine caused 1 lvl", new RuntimeException())),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl", new RuntimeException(new Exception()))),
                Arguments.of(new RuleEngineException("rule engine caused 2 lvl Throwable", new RuntimeException(new Throwable()))),
                Arguments.of(new RuleNodeException("rule node no cause", "RuleChain", new RuleNode()))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOnFailure_NotRateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback, never()).onRateLimit(any());
        verify(callback, never()).onSuccess();
        verify(ctx, never()).onSuccess(any());
    }

    private static Stream<Arguments> testOnFailure_RateLimitException() {
        return Stream.of(
                Arguments.of(new RuleEngineException("caused lvl 1", new TbRateLimitsException(EntityType.ASSET))),
                Arguments.of(new RuleEngineException("caused lvl 2", new RuntimeException(new TbRateLimitsException(EntityType.ASSET)))),
                Arguments.of(
                        new RuleEngineException("caused lvl 3",
                                new RuntimeException(
                                        new Exception(
                                                new TbRateLimitsException(EntityType.ASSET)))))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOnFailure_RateLimitException(RuleEngineException ree) {
        callback.onFailure(ree);

        verify(callback).onRateLimit(any());
        verify(callback).onFailure(any());
        verify(callback, never()).onSuccess();
        verify(ctx).onSuccess(msgId);
        verify(ctx).onSuccess(any());
        verify(ctx, never()).onFailure(any(), any(), any());
    }

}
