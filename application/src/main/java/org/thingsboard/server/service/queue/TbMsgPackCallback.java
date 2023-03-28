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
package org.thingsboard.server.service.queue;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TbMsgPackCallback implements TbMsgCallback {
    private final UUID id;
    private final TenantId tenantId;
    private final TbMsgPackProcessingContext ctx;
    private final long startMsgProcessing;
    private final Timer successfulMsgTimer;
    private final Timer failedMsgTimer;

    public TbMsgPackCallback(UUID id, TenantId tenantId, TbMsgPackProcessingContext ctx) {
        this(id, tenantId, ctx, null, null);
    }

    public TbMsgPackCallback(UUID id, TenantId tenantId, TbMsgPackProcessingContext ctx, Timer successfulMsgTimer, Timer failedMsgTimer) {
        this.id = id;
        this.tenantId = tenantId;
        this.ctx = ctx;
        this.successfulMsgTimer = successfulMsgTimer;
        this.failedMsgTimer = failedMsgTimer;
        startMsgProcessing = System.currentTimeMillis();
    }

    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        if (successfulMsgTimer != null) {
            successfulMsgTimer.record(System.currentTimeMillis() - startMsgProcessing, TimeUnit.MILLISECONDS);
        }
        ctx.onSuccess(id);
    }

    @Override
    public void onFailure(RuleEngineException e) {
        log.trace("[{}] ON FAILURE", id, e);
        if (failedMsgTimer != null) {
            failedMsgTimer.record(System.currentTimeMillis() - startMsgProcessing, TimeUnit.MILLISECONDS);
        }
        ctx.onFailure(tenantId, id, e);
    }

    @Override
    public boolean isMsgValid() {
        return !ctx.isCanceled();
    }

    @Override
    public void onProcessingStart(RuleNodeInfo ruleNodeInfo) {
        log.trace("[{}] ON PROCESSING START: {}", id, ruleNodeInfo);
        ctx.onProcessingStart(id, ruleNodeInfo);
    }

    @Override
    public void onProcessingEnd(RuleNodeId ruleNodeId) {
        log.trace("[{}] ON PROCESSING END: {}", id, ruleNodeId);
        ctx.onProcessingEnd(id, ruleNodeId);
    }
}
