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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class AbstractScriptEvaluator {

    protected final ScriptInvokeService scriptInvokeService;
    private final ScriptType scriptType;
    private final String script;
    protected final TenantId tenantId;
    protected final EntityId entityId;

    protected volatile UUID scriptId;
    private volatile boolean isErrorScript = false;


    public AbstractScriptEvaluator(TenantId tenantId, ScriptInvokeService scriptInvokeService, EntityId entityId, ScriptType scriptType, String script) {
        this.scriptInvokeService = scriptInvokeService;
        this.scriptType = scriptType;
        this.script = script;
        this.tenantId = tenantId;
        this.entityId = entityId;
    }

    public void destroy() {
        if (this.scriptId != null) {
            this.scriptInvokeService.release(this.scriptId);
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
                    this.scriptId = this.scriptInvokeService.eval(tenantId, scriptType, script, this.getArgNames()).get();
                } catch (Exception e) {
                    if (!(e.getCause() instanceof TimeoutException)) {
                        isErrorScript = true;
                    }
                    throw new IllegalArgumentException("Can't compile script: " + e.getMessage(), e);
                }
            }
        }
    }

    protected abstract String[] getArgNames();

}
