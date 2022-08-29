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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.js.api.RuleNodeScriptFactory;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Slf4j
public class RuleNodeJsScriptEngine implements org.thingsboard.rule.engine.api.ScriptEngine {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final JsInvokeService sandboxService;

    private final UUID scriptId;
    private final TenantId tenantId;
    private final EntityId entityId;

    public RuleNodeJsScriptEngine(TenantId tenantId, JsInvokeService sandboxService, EntityId entityId, String script, String... argNames) {
        this(tenantId, sandboxService, entityId, JsScriptType.RULE_NODE_SCRIPT, script, argNames);
    }

    public RuleNodeJsScriptEngine(TenantId tenantId, JsInvokeService sandboxService, EntityId entityId, JsScriptType scriptType, String script, String... argNames) {
        this.tenantId = tenantId;
        this.sandboxService = sandboxService;
        this.entityId = entityId;
        try {
            this.scriptId = this.sandboxService.eval(tenantId, scriptType, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
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
            return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to unbind message data from javascript result", th);
        }
    }

    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<JsonNode> result = executeScriptAsync(msg);
        return Futures.transformAsync(result,
                json -> executeUpdateTransform(msg, json),
                MoreExecutors.directExecutor());
    }

    ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, JsonNode json) {
        if (json.isObject()) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg(json, msg)));
        } else if (json.isArray()) {
            List<TbMsg> res = new ArrayList<>(json.size());
            json.forEach(jsonObject -> res.add(unbindMsg(jsonObject, msg)));
            return Futures.immediateFuture(res);
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg) {
        return Futures.transformAsync(executeScriptAsync(prevMsg),
                result -> executeGenerateTransform(prevMsg, result),
                MoreExecutors.directExecutor());
    }

    ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, JsonNode result) {
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
        }
        return Futures.immediateFuture(unbindMsg(result, prevMsg));
    }

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return executeScriptAsync(msg);
    }

    @Override
    public ListenableFuture<String> executeToStringAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeToStringTransform,
                MoreExecutors.directExecutor());
    }

    ListenableFuture<String> executeToStringTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(result.asText());
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    @Override
    public ListenableFuture<Boolean> executeAttributesFilterAsync(Map<String, String> attributes) {
        return Futures.transformAsync(executeAttributesScriptAsync(attributes),
                this::executeFilterTransform,
                MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeFilterTransform,
                MoreExecutors.directExecutor());
    }

    ListenableFuture<Boolean> executeFilterTransform(JsonNode json) {
        if (json.isBoolean()) {
            return Futures.immediateFuture(json.asBoolean());
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    ListenableFuture<Set<String>> executeSwitchTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(Collections.singleton(result.asText()));
        }
        if (result.isArray()) {
            Set<String> nextStates = new HashSet<>();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    log.warn("Wrong result type: {}", val.getNodeType());
                    return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + val.getNodeType()));
                } else {
                    nextStates.add(val.asText());
                }
            }
            return Futures.immediateFuture(nextStates);
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeSwitchTransform,
                MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    private ListenableFuture<JsonNode> executeAttributesScriptAsync(Map<String, String> attributes) {
        String attributesStr;
        try {
            attributesStr = mapper.writeValueAsString(attributes);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(new ScriptException("Failed to convert attributes to json: " + e.getMessage()));
        }
        return executeScriptAsync(null, attributesStr);
    }

    ListenableFuture<JsonNode> executeScriptAsync(TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        String[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    ListenableFuture<JsonNode> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transformAsync(sandboxService.invokeFunction(tenantId, customerId, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(mapper.readTree(o.toString()));
                    } catch (Exception e) {
                        if (e.getCause() instanceof ScriptException) {
                            return Futures.immediateFailedFuture(e.getCause());
                        } else if (e.getCause() instanceof RuntimeException) {
                            return Futures.immediateFailedFuture(new ScriptException(e.getCause().getMessage()));
                        } else {
                            return Futures.immediateFailedFuture(new ScriptException(e));
                        }
                    }
                }, MoreExecutors.directExecutor());
    }

    public void destroy() {
        sandboxService.release(this.scriptId);
    }
}
