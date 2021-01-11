/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.api.converter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.js.api.JsInvokeService;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Slf4j
public abstract class AbstractJSEvaluator {

    protected final JsInvokeService jsInvokeService;
    private final JsScriptType scriptType;
    private final String script;
    protected final TenantId tenantId;
    protected final EntityId entityId;

    protected volatile UUID scriptId;
    private volatile boolean isErrorScript = false;


    public AbstractJSEvaluator(TenantId tenantId, JsInvokeService jsInvokeService, EntityId entityId, JsScriptType scriptType, String script) {
        this.jsInvokeService = jsInvokeService;
        this.scriptType = scriptType;
        this.script = script;
        this.tenantId = tenantId;
        this.entityId = entityId;
    }

    public void destroy() {
        if (this.scriptId != null) {
            this.jsInvokeService.release(this.scriptId);
        }
    }

    void validateSuccessfulScriptLazyInit() {
        if (this.scriptId != null) {
            return;
        }

        if (isErrorScript) {
            throw new IllegalArgumentException("Can't compile uplink converter script ");
        }

        synchronized (this) {
            if (this.scriptId == null) {
                try {
                    this.scriptId = this.jsInvokeService.eval(tenantId, scriptType, script).get();
                } catch (Exception e) {
                    isErrorScript = true;
                    throw new IllegalArgumentException("Can't compile script: " + e.getMessage(), e);
                }
            }
        }
    }

}
