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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
public class RuleNodeJsScriptEngine implements org.thingsboard.rule.engine.api.ScriptEngine {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final JsSandboxService sandboxService;

    private final UUID scriptId;
    private final EntityId entityId;

<<<<<<< HEAD
    public RuleNodeJsScriptEngine(JsSandboxService sandboxService, String script, String... argNames) {
        this(sandboxService, JsScriptType.RULE_NODE_SCRIPT, script, argNames);
    }

    public RuleNodeJsScriptEngine(JsSandboxService sandboxService, JsScriptType scriptType, String script, String... argNames) {
=======
    public RuleNodeJsScriptEngine(JsSandboxService sandboxService, EntityId entityId, String script, String... argNames) {
>>>>>>> upstream/master
        this.sandboxService = sandboxService;
        this.entityId = entityId;
        try {
            this.scriptId = this.sandboxService.eval(scriptType, script, argNames).get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage(), e);
        }
    }

    private static String[] prepareArgs(TbMsg msg) {
        try {
            String[] args = new String[3];
            if (msg.getData() != null) {
                args[0] = msg.getData();
            } else {
                args[0] = "";
            }
            args[1] = mapper.writeValueAsString(msg.getMetaData().getData());
            args[2] = msg.getType();
            return args;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

    private static TbMsg unbindMsg(JsonNode msgData, TbMsg msg) {
        try {
            String data = null;
            Map<String, String> metadata = null;
            String messageType = null;
            if (msgData.has(RuleNodeScriptFactory.MSG)) {
                JsonNode msgPayload = msgData.get(RuleNodeScriptFactory.MSG);
                data = mapper.writeValueAsString(msgPayload);
            }
            if (msgData.has(RuleNodeScriptFactory.METADATA)) {
                JsonNode msgMetadata = msgData.get(RuleNodeScriptFactory.METADATA);
                metadata = mapper.convertValue(msgMetadata, new TypeReference<Map<String, String>>() {
                });
            }
            if (msgData.has(RuleNodeScriptFactory.MSG_TYPE)) {
                messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).asText();
            }
            String newData = data != null ? data : msg.getData();
            TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
            String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
            return new TbMsg(msg.getId(), newMessageType, msg.getOriginator(), newMetadata, newData, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition());
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException("Failed to unbind message data from javascript result", th);
        }
    }

    @Override
    public TbMsg executeUpdate(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return unbindMsg(result, msg);
    }

    @Override
    public TbMsg executeGenerate(TbMsg prevMsg) throws ScriptException {
        JsonNode result = executeScript(prevMsg);
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return unbindMsg(result, prevMsg);
    }

    @Override
    public JsonNode executeJson(TbMsg msg) throws ScriptException {
        return executeScript(msg);
    }

    @Override
    public String executeToString(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isTextual()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return result.asText();
    }

    @Override
    public boolean executeFilter(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (!result.isBoolean()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return result.asBoolean();
    }

    @Override
    public boolean executeAttributesFilter(Map<String, String> attributes) throws ScriptException {
        JsonNode result = executeAttributesScript(attributes);
        if (!result.isBoolean()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
        return result.asBoolean();
    }

    @Override
    public Set<String> executeSwitch(TbMsg msg) throws ScriptException {
        JsonNode result = executeScript(msg);
        if (result.isTextual()) {
            return Collections.singleton(result.asText());
        } else if (result.isArray()) {
            Set<String> nextStates = Sets.newHashSet();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    log.warn("Wrong result type: {}", val.getNodeType());
                    throw new ScriptException("Wrong result type: " + val.getNodeType());
                } else {
                    nextStates.add(val.asText());
                }
            }
            return nextStates;
        } else {
            log.warn("Wrong result type: {}", result.getNodeType());
            throw new ScriptException("Wrong result type: " + result.getNodeType());
        }
    }

    private JsonNode executeScript(TbMsg msg) throws ScriptException {
        try {
            String[] inArgs = prepareArgs(msg);
            String eval = sandboxService.invokeFunction(this.scriptId, this.entityId, inArgs[0], inArgs[1], inArgs[2]).get().toString();
            return mapper.readTree(eval);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException)e.getCause();
            } else {
                throw new ScriptException(e);
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private JsonNode executeAttributesScript(Map<String,String> attributes) throws ScriptException {
        try {
            String attributesStr = mapper.writeValueAsString(attributes);
            String eval = sandboxService.invokeFunction(this.scriptId, attributesStr).get().toString();
            return mapper.readTree(eval);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ScriptException) {
                throw (ScriptException)e.getCause();
            } else {
                throw new ScriptException("Failed to execute js script: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new ScriptException("Failed to execute js script: " + e.getMessage());
        }
    }

    public void destroy() {
        sandboxService.release(this.scriptId, this.entityId);
    }
}
