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
package org.thingsboard.js.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.common.util.ThingsBoardExecutors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractNashornJsInvokeService extends AbstractJsInvokeService {

    private NashornSandbox sandbox;
    private ScriptEngine engine;
    private ExecutorService monitorExecutorService;
    private ListeningExecutorService jsExecutor;

    private final AtomicInteger jsPushedMsgs = new AtomicInteger(0);
    private final AtomicInteger jsInvokeMsgs = new AtomicInteger(0);
    private final AtomicInteger jsEvalMsgs = new AtomicInteger(0);
    private final AtomicInteger jsFailedMsgs = new AtomicInteger(0);
    private final AtomicInteger jsTimeoutMsgs = new AtomicInteger(0);
    private final FutureCallback<UUID> evalCallback = new JsStatCallback<>(jsEvalMsgs, jsTimeoutMsgs, jsFailedMsgs);
    private final FutureCallback<Object> invokeCallback = new JsStatCallback<>(jsInvokeMsgs, jsTimeoutMsgs, jsFailedMsgs);

    private final ReentrantLock evalLock = new ReentrantLock();

    @Value("${js.local.max_requests_timeout:0}")
    private long maxRequestsTimeout;

    @Value("${js.local.stats.enabled:true}")
    private boolean statsEnabled;

    @Value("${js.local.js_thread_pool_size:50}")
    private int jsExecutorThreadPoolSize;

    public AbstractNashornJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Scheduled(fixedDelayString = "${js.local.stats.print_interval_ms:10000}")
    public void printStats() {
        if (statsEnabled) {
            int pushedMsgs = jsPushedMsgs.getAndSet(0);
            int invokeMsgs = jsInvokeMsgs.getAndSet(0);
            int evalMsgs = jsEvalMsgs.getAndSet(0);
            int failed = jsFailedMsgs.getAndSet(0);
            int timedOut = jsTimeoutMsgs.getAndSet(0);
            if (pushedMsgs > 0 || invokeMsgs > 0 || evalMsgs > 0 || failed > 0 || timedOut > 0) {
                log.info("Nashorn JS Invoke Stats: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        pushedMsgs, invokeMsgs + evalMsgs, invokeMsgs, evalMsgs, failed, timedOut);
            }
        }
    }

    @PostConstruct
    public void init() {
        super.init(maxRequestsTimeout);
        jsExecutor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(jsExecutorThreadPoolSize));
        if (useJsSandbox()) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = ThingsBoardExecutors.newWorkStealingPool(getMonitorThreadPoolSize(), "nashorn-js-monitor");
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(getMaxCpuTime());
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
            sandbox.setMaxPreparedStatements(30);
        } else {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("nashorn");
        }
    }

    @PreDestroy
    public void stop() {
        super.stop();
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
    }

    protected abstract boolean useJsSandbox();

    protected abstract int getMonitorThreadPoolSize();

    protected abstract long getMaxCpuTime();

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String scriptHash, String functionName, String jsScript) {
        jsPushedMsgs.incrementAndGet();
        ListenableFuture<UUID> result = jsExecutor.submit(() -> {
            try {
                evalLock.lock();
                try {
                    if (useJsSandbox()) {
                        sandbox.eval(jsScript);
                    } else {
                        engine.eval(jsScript);
                    }
                } finally {
                    evalLock.unlock();
                }
                scriptIdToNameAndHashMap.put(scriptId, Pair.of(functionName, scriptHash));
                return scriptId;
            } catch (Exception e) {
                log.debug("Failed to compile JS script: {}", e.getMessage(), e);
                throw new ExecutionException(e);
            }
        });
        if (maxRequestsTimeout > 0) {
            result = Futures.withTimeout(result, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(result, evalCallback, MoreExecutors.directExecutor());
        return result;
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String scriptHash, String functionName, Object[] args) {
        jsPushedMsgs.incrementAndGet();
        ListenableFuture<Object> result = jsExecutor.submit(() -> {
            try {
                if (useJsSandbox()) {
                    return sandbox.getSandboxedInvocable().invokeFunction(functionName, args);
                } else {
                    return ((Invocable) engine).invokeFunction(functionName, args);
                }
            } catch (ScriptException e) {
                throw new ExecutionException(e);
            } catch (Exception e) {
                onScriptExecutionError(scriptId, e, functionName);
                throw new ExecutionException(e);
            }
        });

        if (maxRequestsTimeout > 0) {
            result = Futures.withTimeout(result, maxRequestsTimeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(result, invokeCallback, MoreExecutors.directExecutor());
        return result;
    }

    protected void doRelease(UUID scriptId, String scriptHash, String functionName) throws ScriptException {
        if (useJsSandbox()) {
            sandbox.eval(functionName + " = undefined;");
        } else {
            engine.eval(functionName + " = undefined;");
        }
    }

}
