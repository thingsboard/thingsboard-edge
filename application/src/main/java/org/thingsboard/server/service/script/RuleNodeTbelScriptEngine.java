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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.execution.ExecutionArrayList;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
public class RuleNodeTbelScriptEngine extends RuleNodeScriptEngine<TbelInvokeService, Object> {

    public RuleNodeTbelScriptEngine(TenantId tenantId, TbelInvokeService scriptInvokeService, String script, String... argNames) {
        this(tenantId, scriptInvokeService, ScriptType.RULE_NODE_SCRIPT, script, argNames);
    }

    public RuleNodeTbelScriptEngine(TenantId tenantId, TbelInvokeService scriptInvokeService, ScriptType scriptType, String script, String... argNames) {
        super(tenantId, scriptInvokeService, scriptType, script, argNames);
    }

    @Override
    protected ListenableFuture<Boolean> executeFilterTransform(Object result) {
        if (result instanceof Boolean) {
            return Futures.immediateFuture((Boolean) result);
        }
        return wrongResultType(result);
    }

    @Override
    protected Object prepareAttributes(Map<String, KvEntry> attributes) {
        var result = new HashMap<>();
        if (attributes != null) {
            attributes.forEach((k, v) -> {
                switch (v.getDataType()) {
                    case STRING:
                        v.getStrValue().ifPresent(val -> result.put(k, val));
                        break;
                    case BOOLEAN:
                        v.getBooleanValue().ifPresent(val -> result.put(k, val));
                        break;
                    case DOUBLE:
                        v.getDoubleValue().ifPresent(val -> result.put(k, val));
                        break;
                    case LONG:
                        v.getLongValue().ifPresent(val -> result.put(k, val));
                        break;
                    case JSON:
                        v.getJsonValue().ifPresent(val -> result.put(k, JacksonUtil.toJsonNode(val)));
                        break;
                }
            });
        }
        return result;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg((Map) result, msg)));
        } else if (result instanceof Collection) {
            List<TbMsg> res = new ArrayList<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof Map) {
                    res.add(unbindMsg((Map) resObject, msg));
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    @Override
    protected ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(unbindMsg((Map) result, prevMsg));
        }
        return wrongResultType(result);
    }

    @Override
    protected ListenableFuture<String> executeToStringTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture((String) result);
        } else {
            return Futures.immediateFuture(JacksonUtil.toString(result));
        }
    }

    @Override
    protected ListenableFuture<Set<String>> executeSwitchTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture(Collections.singleton((String) result));
        } else if (result instanceof Collection) {
            Set<String> res = new HashSet<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof String) {
                    res.add((String) resObject);
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), JacksonUtil::valueToTree, MoreExecutors.directExecutor());

    }

    @Override
    protected Object convertResult(Object result) {
        return result;
    }

    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        Object[] args = new Object[3];
        if (msg.getData() != null) {
            args[0] = JacksonUtil.fromString(msg.getData(), Object.class);
        } else {
            args[0] = new HashMap<>();
        }
        args[1] = new HashMap<>(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    private static TbMsg unbindMsg(Map msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.containsKey(RuleNodeScriptFactory.MSG)) {
            data = JacksonUtil.toString(msgData.get(RuleNodeScriptFactory.MSG));
        }
        if (msgData.containsKey(RuleNodeScriptFactory.METADATA)) {
            Object msgMetadataObj = msgData.get(RuleNodeScriptFactory.METADATA);
            if (msgMetadataObj instanceof Map) {
                metadata = ((Map<?, ?>) msgMetadataObj).entrySet().stream().filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            } else {
                metadata = JacksonUtil.convertValue(msgMetadataObj, new TypeReference<>() {
                });
            }
        }
        if (msgData.containsKey(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).toString();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
        return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
    }

    private static <T> ListenableFuture<T> wrongResultType(Object result) {
        String className = toClassName(result);
        log.warn("Wrong result type: {}", className);
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + className));
    }

    private static String toClassName(Object result) {
        return result != null ? result.getClass().getSimpleName() : "null";
    }
}
