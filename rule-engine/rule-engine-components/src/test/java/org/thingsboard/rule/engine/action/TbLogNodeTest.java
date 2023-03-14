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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@Slf4j
public class TbLogNodeTest {

    @Test
    void givenMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        String data = "{\"key\": \"value\"}";
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("mdKey1", "mdValue1", "mdKey2", "23"));
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, data);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "{\"key\": \"value\"}\n" +
                "Incoming metadata:\n" +
                "{\"mdKey1\":\"mdValue1\",\"mdKey2\":\"23\"}");
    }

    @Test
    void givenEmptyDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, "");

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    @Test
    void givenNullDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, null);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "null\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    @ParameterizedTest
    @EnumSource(ScriptLanguage.class)
    void givenDefaultConfig_whenIsStandardForEachScriptLanguage_thenTrue(ScriptLanguage scriptLanguage) throws TbNodeException {

        TbLogNodeConfiguration config = new TbLogNodeConfiguration().defaultConfiguration();
        config.setScriptLang(scriptLanguage);
        TbLogNode node = spy(new TbLogNode());
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        TbContext ctx = mock(TbContext.class);
        node.init(ctx, tbNodeConfiguration);

        assertThat(node.isStandard(config)).as("Script is standard for language " + scriptLanguage).isTrue();
        verify(node, never()).createScriptEngine(any(), any());
        verify(ctx, never()).createScriptEngine(any(), anyString());

    }

    @Test
    void givenScriptEngineEnum_whenNewAdded_thenFailed() {
        assertThat(ScriptLanguage.values().length).as("only two ScriptLanguage supported").isEqualTo(2);
    }

    @Test
    void givenScriptEngineLangJs_whenCreateScriptEngine_thenSupplyJsScript(){
        TbLogNodeConfiguration configJs = new TbLogNodeConfiguration().defaultConfiguration();
        configJs.setScriptLang(ScriptLanguage.JS);
        configJs.setJsScript(configJs.getJsScript() + " // This is JS script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configJs);
        verify(ctx).createScriptEngine(ScriptLanguage.JS, configJs.getJsScript());
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void givenScriptEngineLangTbel_whenCreateScriptEngine_thenSupplyTbelScript(){
        TbLogNodeConfiguration configTbel = new TbLogNodeConfiguration().defaultConfiguration();
        configTbel.setScriptLang(ScriptLanguage.TBEL);
        configTbel.setTbelScript(configTbel.getTbelScript() + " // This is TBEL script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configTbel);
        verify(ctx).createScriptEngine(ScriptLanguage.TBEL, configTbel.getTbelScript());
        verifyNoMoreInteractions(ctx);
    }

}
