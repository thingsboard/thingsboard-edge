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
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "local", matchIfMissing = true)
@Service
public class NashornJsInvokeService extends AbstractJsInvokeService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;
    private ListeningExecutorService jsExecutor;

    private final ReentrantLock evalLock = new ReentrantLock();

    @Value("${js.local.use_js_sandbox}")
    private boolean useJsSandbox;

    @Value("${js.local.monitor_thread_pool_size}")
    private int monitorThreadPoolSize;

    @Value("${js.local.max_cpu_time}")
    private long maxCpuTime;

    @Getter
    @Value("${js.local.max_errors}")
    private int maxErrors;

    @Getter
    @Value("${js.local.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    @Getter
    @Value("${js.local.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    @Getter
    @Value("${js.local.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${js.local.js_thread_pool_size:50}")
    private int jsExecutorThreadPoolSize;

    public NashornJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Override
    protected String getStatsName() {
        return "Nashorn JS Invoke Stats";
    }

    @Override
    protected Executor getCallbackExecutor() {
        return MoreExecutors.directExecutor();
    }

    @Scheduled(fixedDelayString = "${js.local.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
        jsExecutor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(jsExecutorThreadPoolSize));
        if (useJsSandbox) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = ThingsBoardExecutors.newWorkStealingPool(monitorThreadPoolSize, "nashorn-js-monitor");
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(maxCpuTime);
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
            sandbox.setMaxPreparedStatements(30);
        } else {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("nashorn");
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        super.stop();
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo scriptInfo, String jsScript) {
        return jsExecutor.submit(() -> {
            try {
                evalLock.lock();
                try {
                    if (useJsSandbox) {
                        sandbox.eval(jsScript);
                    } else {
                        engine.eval(jsScript);
                    }
                } finally {
                    evalLock.unlock();
                }
                scriptInfoMap.put(scriptId, scriptInfo);
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, jsScript, e);
            }
        });
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo scriptInfo, Object[] args) {
        return jsExecutor.submit(() -> {
            try {
                if (useJsSandbox) {
                    return sandbox.getSandboxedInvocable().invokeFunction(scriptInfo.getFunctionName(), args);
                } else {
                    return ((Invocable) engine).invokeFunction(scriptInfo.getFunctionName(), args);
                }
            } catch (ScriptException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, null, e);
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, e);
            }
        });
    }

    protected void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws ScriptException {
        if (useJsSandbox) {
            sandbox.eval(scriptInfo.getFunctionName() + " = undefined;");
        } else {
            engine.eval(scriptInfo.getFunctionName() + " = undefined;");
        }
    }

    @Override
    protected boolean isLocal() {
        return true;
    }
}
