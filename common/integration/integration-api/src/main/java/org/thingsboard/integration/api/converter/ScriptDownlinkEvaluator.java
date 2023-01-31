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
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ScriptDownlinkEvaluator extends AbstractScriptEvaluator {

    private static final ObjectMapper mapper = new ObjectMapper();

    public ScriptDownlinkEvaluator(TenantId tenantId, ScriptInvokeService scriptInvokeService, EntityId entityId, String script) {
        super(tenantId, scriptInvokeService, entityId, ScriptType.DOWNLINK_CONVERTER_SCRIPT, script);
    }

    public JsonNode execute(TbMsg msg, IntegrationMetaData metadata) throws ScriptException {
        try {
            validateSuccessfulScriptLazyInit();
            Object[] inArgs = prepareArgs(scriptInvokeService.getLanguage(), msg, metadata);
            Object eval = scriptInvokeService.invokeScript(this.tenantId, msg.getCustomerId(), this.scriptId, inArgs[0], inArgs[1], inArgs[2], inArgs[3]).get();
            if (eval instanceof String) {
                return JacksonUtil.toJsonNode(eval.toString());
            } else {
                return JacksonUtil.valueToTree(eval);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException) e.getCause();
            } else {
                throw new ScriptException("Failed to execute js script: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ScriptException("Failed to execute js script: " + e.getMessage());
        }
    }

    private static Object[] prepareArgs(ScriptLanguage scriptLang, TbMsg msg, IntegrationMetaData metadata) {
        if (ScriptLanguage.JS.equals(scriptLang)) {
            try {
                String[] args = new String[4];
                if (msg.getData() != null) {
                    args[0] = msg.getData();
                } else {
                    args[0] = "";
                }
                args[1] = mapper.writeValueAsString(msg.getMetaData().getData());
                args[2] = msg.getType();
                args[3] = mapper.writeValueAsString(metadata.getKvMap());
                return args;
            } catch (Throwable th) {
                throw new IllegalArgumentException("Cannot bind js args", th);
            }
        } else {
            Object[] args = new Object[4];
            if (msg.getData() != null) {
                args[0] = JacksonUtil.fromString(msg.getData(), Map.class);
            } else {
                args[0] = new HashMap<>();
            }
            args[1] = new HashMap<>(msg.getMetaData().getData());
            args[2] = msg.getType();
            args[3] = new HashMap<>(metadata.getKvMap());
            return args;
        }
    }

    @Override
    protected String[] getArgNames() {
        return new String[]{"msg", "metadata", "msgType", "integrationMetadata"};
    }
}
