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
package org.thingsboard.rule.engine.filter;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbJsFilterNodeTest {

    private TbJsFilterNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ScriptEngine scriptEngine;

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    @Test
    public void falseEvaluationDoNotSendMsg() throws TbNodeException, ScriptException {
        initWithScript();
        TbMsg msg = TbMsg.newMsg("USER", null, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFuture(false));

        node.onMsg(ctx, msg);
        verify(ctx).getDbCallbackExecutor();
        verify(ctx).tellNext(msg, "False");
    }

    @Test
    public void exceptionInJsThrowsException() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg msg = TbMsg.newMsg("USER", null, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFailedFuture(new ScriptException("error")));


        node.onMsg(ctx, msg);
        verifyError(msg, "error", ScriptException.class);
    }

    @Test
    public void metadataConditionCanBeTrue() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg msg = TbMsg.newMsg("USER", null, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).getDbCallbackExecutor();
        verify(ctx).tellNext(msg, "True");
    }

    private void initWithScript() throws TbNodeException {
        TbJsFilterNodeConfiguration config = new TbJsFilterNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbJsFilterNode();
        node.init(ctx, nodeConfiguration);
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}
