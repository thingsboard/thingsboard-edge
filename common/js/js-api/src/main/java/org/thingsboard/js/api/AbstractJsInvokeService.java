/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.js.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService implements JsInvokeService {

    private final Optional<TbApiUsageStateClient> apiUsageStateClient;
    private final Optional<TbApiUsageReportClient> apiUsageReportClient;
    protected ScheduledExecutorService timeoutExecutorService;
    protected Map<UUID, String> scriptIdToNameMap = new ConcurrentHashMap<>();
    protected Map<UUID, DisableListInfo> disabledFunctions = new ConcurrentHashMap<>();

    protected AbstractJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        this.apiUsageStateClient = apiUsageStateClient;
        this.apiUsageReportClient = apiUsageReportClient;
    }

    public void init(long maxRequestsTimeout) {
        if (maxRequestsTimeout > 0) {
            timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("nashorn-js-timeout"));
        }
    }

    public void stop() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, JsScriptType scriptType, String scriptBody, String... argNames) {
        if (!apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled()) {
            UUID scriptId = UUID.randomUUID();
            String functionName = "invokeInternal_" + scriptId.toString().replace('-', '_');
            String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
            return doEval(scriptId, functionName, jsScript);
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("JS Execution is disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<Object> invokeFunction(TenantId tenantId, UUID scriptId, Object... args) {
        if (!apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled()) {
            String functionName = scriptIdToNameMap.get(scriptId);
            if (functionName == null) {
                return Futures.immediateFailedFuture(new RuntimeException("No compiled script found for scriptId: [" + scriptId + "]!"));
            }
            if (!isDisabled(scriptId)) {
                apiUsageReportClient.ifPresent(client -> client.report(tenantId, ApiUsageRecordKey.JS_EXEC_COUNT, 1));
                return doInvokeFunction(scriptId, functionName, args);
            } else {
                return Futures.immediateFailedFuture(
                        new RuntimeException("Script invocation is blocked due to maximum error count " + getMaxErrors() + "!"));
            }
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("JS Execution is disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        String functionName = scriptIdToNameMap.get(scriptId);
        if (functionName != null) {
            try {
                scriptIdToNameMap.remove(scriptId);
                disabledFunctions.remove(scriptId);
                doRelease(scriptId, functionName);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args);

    protected abstract void doRelease(UUID scriptId, String functionName) throws Exception;

    protected abstract int getMaxErrors();

    protected abstract boolean isLocal();

    protected abstract long getMaxBlacklistDuration();

    protected void onScriptExecutionError(UUID scriptId) {
        disabledFunctions.computeIfAbsent(scriptId, key -> new DisableListInfo()).incrementAndGet();
    }

    private String generateJsScript(JsScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        switch (scriptType) {
            case RULE_NODE_SCRIPT:
                return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
            case ATTRIBUTES_SCRIPT:
                return AttributesScriptFactory.generateAttributesScript(functionName, scriptBody);
            case UPLINK_CONVERTER_SCRIPT:
                return UplinkConverterScriptFactory.generateUplinkConverterScript(functionName, scriptBody, isLocal());
            case DOWNLINK_CONVERTER_SCRIPT:
                return DownlinkConverterScriptFactory.generateDownlinkConverterScript(functionName, scriptBody, isLocal());
            default:
                throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
        }
    }

    private boolean isDisabled(UUID scriptId) {
        DisableListInfo errorCount = disabledFunctions.get(scriptId);
        if (errorCount != null) {
            if (errorCount.getExpirationTime() <= System.currentTimeMillis()) {
                disabledFunctions.remove(scriptId);
                return false;
            } else {
                return errorCount.get() >= getMaxErrors();
            }
        } else {
            return false;
        }
    }

    private class DisableListInfo {
        private final AtomicInteger counter;
        private long expirationTime;

        private DisableListInfo() {
            this.counter = new AtomicInteger(0);
        }

        public int get() {
            return counter.get();
        }

        public int incrementAndGet() {
            int result = counter.incrementAndGet();
            expirationTime = System.currentTimeMillis() + getMaxBlacklistDuration();
            return result;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }
}
