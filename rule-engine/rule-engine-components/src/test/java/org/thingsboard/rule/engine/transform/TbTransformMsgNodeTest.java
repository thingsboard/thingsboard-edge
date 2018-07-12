/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.transform;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class TbTransformMsgNodeTest {

    private TbTransformMsgNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;
    @Mock
    private ScriptEngine scriptEngine;

    @Test
    public void metadataCanBeUpdated() throws TbNodeException, ScriptException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5}";

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson, ruleChainId, ruleNodeId, 0L);
        TbMsg transformedMsg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, "{new}", ruleChainId, ruleNodeId, 0L);
        mockJsExecutor();
        when(scriptEngine.executeUpdate(msg)).thenReturn(transformedMsg);

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellNext(captor.capture(), eq(SUCCESS));
        TbMsg actualMsg = captor.getValue();
        assertEquals(transformedMsg, actualMsg);
    }

    @Test
    public void exceptionHandledCorrectly() throws TbNodeException, ScriptException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5";

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson, ruleChainId, ruleNodeId, 0L);
        mockJsExecutor();
        when(scriptEngine.executeUpdate(msg)).thenThrow(new IllegalStateException("error"));

        node.onMsg(ctx, msg);
        verifyError(msg, "error", IllegalStateException.class);
    }

    private void initWithScript() throws TbNodeException {
        TbTransformMsgNodeConfiguration config = new TbTransformMsgNodeConfiguration();
        config.setJsScript("scr");
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.createJsScriptEngine("scr")).thenReturn(scriptEngine);

        node = new TbTransformMsgNode();
        node.init(ctx, nodeConfiguration);
    }

    private void mockJsExecutor() {
        when(ctx.getJsExecutor()).thenReturn(executor);
        doAnswer((Answer<ListenableFuture<TbMsg>>) invocationOnMock -> {
            try {
                Callable task = (Callable) (invocationOnMock.getArguments())[0];
                return Futures.immediateFuture((TbMsg) task.call());
            } catch (Throwable th) {
                return Futures.immediateFailedFuture(th);
            }
        }).when(executor).executeAsync(Matchers.any(Callable.class));
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}