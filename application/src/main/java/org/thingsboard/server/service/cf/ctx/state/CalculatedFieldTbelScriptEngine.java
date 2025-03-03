/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;

import javax.script.ScriptException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CalculatedFieldTbelScriptEngine implements CalculatedFieldScriptEngine {

    private final TbelInvokeService tbelInvokeService;

    private final UUID scriptId;
    private final TenantId tenantId;

    public CalculatedFieldTbelScriptEngine(TenantId tenantId, TbelInvokeService tbelInvokeService, String script, String... argNames) {
        this.tenantId = tenantId;
        this.tbelInvokeService = tbelInvokeService;
        try {
            this.scriptId = this.tbelInvokeService.eval(tenantId, ScriptType.CALCULATED_FIELD_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
        }
    }

    @Override
    public ListenableFuture<Object> executeScriptAsync(Object[] args) {
        log.trace("Executing script async, args {}", args);
        return Futures.transformAsync(tbelInvokeService.invokeScript(tenantId, null, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(o);
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

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(Object[] args) {
        return Futures.transform(executeScriptAsync(args), JacksonUtil::valueToTree, MoreExecutors.directExecutor());
    }

    @Override
    public void destroy() {
        tbelInvokeService.release(this.scriptId);
    }
}
