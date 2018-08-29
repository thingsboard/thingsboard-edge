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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractNashornJsSandboxService implements JsSandboxService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;

    private final Map<UUID, String> functionsMap = new ConcurrentHashMap<>();
    private final Map<UUID,AtomicInteger> blackListedFunctions = new ConcurrentHashMap<>();
    private final Map<String, Pair<UUID, AtomicInteger>> scriptToId = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> scriptIdToCount = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (useJsSandbox()) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = Executors.newFixedThreadPool(getMonitorThreadPoolSize());
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(getMaxCpuTime());
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
            sandbox.setMaxPreparedStatements(30);
        } else {
            NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
            engine = factory.getScriptEngine(new String[]{"--no-java"});
        }
    }

    @PreDestroy
    public void stop() {
        if  (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    protected abstract boolean useJsSandbox();

    protected abstract int getMonitorThreadPoolSize();

    protected abstract long getMaxCpuTime();

    protected abstract int getMaxErrors();

    @Override
    public ListenableFuture<UUID> eval(JsScriptType scriptType, String scriptBody, String... argNames) {
        Pair<UUID, AtomicInteger> deduplicated = deduplicate(scriptType, scriptBody);
        UUID scriptId = deduplicated.getLeft();
        AtomicInteger duplicateCount = deduplicated.getRight();

        if(duplicateCount.compareAndSet(0, 1)) {
            String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
            String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
            try {
                if (useJsSandbox()) {
                    sandbox.eval(jsScript);
                } else {
                    engine.eval(jsScript);
                }
                functionsMap.put(scriptId, functionName);
            } catch (Exception e) {
                duplicateCount.decrementAndGet();
                log.warn("Failed to compile JS script: {}", e.getMessage(), e);
                return Futures.immediateFailedFuture(e);
            }
        } else {
            duplicateCount.incrementAndGet();
        }
        return Futures.immediateFuture(scriptId);
    }

    @Override
    public ListenableFuture<Object> invokeFunction(UUID scriptId, Object... args) {
        String functionName = functionsMap.get(scriptId);
        if (functionName == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
        }
        if (!isBlackListed(scriptId)) {
            try {
                Object result;
                if (useJsSandbox()) {
                    result = sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
                } else {
                    result = ((Invocable)engine).invokeFunction(functionName, args);
                }
                return Futures.immediateFuture(result);
            } catch (Exception e) {
                blackListedFunctions.computeIfAbsent(scriptId, key -> new AtomicInteger(0)).incrementAndGet();
                return Futures.immediateFailedFuture(e);
            }
        } else {
            return Futures.immediateFailedFuture(
                    new RuntimeException("Script is blacklisted due to maximum error count " + getMaxErrors() + "!"));
        }
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        AtomicInteger count = scriptIdToCount.get(scriptId);
        if(count != null) {
            if(count.decrementAndGet() > 0) {
                return Futures.immediateFuture(null);
            }
        }

        String functionName = functionsMap.get(scriptId);
        if (functionName != null) {
            try {
                if (useJsSandbox()) {
                    sandbox.eval(functionName + " = undefined;");
                } else {
                    engine.eval(functionName + " = undefined;");
                }
                functionsMap.remove(scriptId);
                blackListedFunctions.remove(scriptId);
            } catch (ScriptException e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    private boolean isBlackListed(UUID scriptId) {
        if (blackListedFunctions.containsKey(scriptId)) {
            AtomicInteger errorCount = blackListedFunctions.get(scriptId);
            return errorCount.get() >= getMaxErrors();
        } else {
            return false;
        }
    }

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        switch (scriptType) {
            case RULE_NODE_SCRIPT:
                return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
            case ATTRIBUTES_SCRIPT:
                return AttributesScriptFactory.generateAttributesScript(functionName, scriptBody);
            case UPLINK_CONVERTER_SCRIPT:
                return UplinkConverterScriptFactory.generateUplinkConverterScript(functionName, scriptBody);
            case DOWNLINK_CONVERTER_SCRIPT:
                return DownlinkConverterScriptFactory.generateDownlinkConverterScript(functionName, scriptBody);
            default:
                throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
        }
    }

    private Pair<UUID, AtomicInteger> deduplicate(JsScriptType scriptType, String scriptBody) {
        Pair<UUID, AtomicInteger> precomputed = Pair.of(UUID.randomUUID(), new AtomicInteger());

        Pair<UUID, AtomicInteger> pair = scriptToId.computeIfAbsent(deduplicateKey(scriptType, scriptBody), i -> precomputed);
        AtomicInteger duplicateCount = scriptIdToCount.computeIfAbsent(pair.getLeft(), i -> pair.getRight());
        return Pair.of(pair.getLeft(), duplicateCount);
    }

    private String deduplicateKey(JsScriptType scriptType, String scriptBody) {
        return scriptType + "_" + scriptBody;
    }
}
