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
package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private final String queueName;
    @Getter
    private final boolean success;
    @Getter
    private final boolean timeout;
    @Getter
    private final TbMsgPackProcessingContext ctx;

    public TbRuleEngineProcessingResult(String queueName, boolean timeout, TbMsgPackProcessingContext ctx) {
        this.queueName = queueName;
        this.timeout = timeout;
        this.ctx = ctx;
        this.success = !timeout && ctx.getPendingMap().isEmpty() && ctx.getFailedMap().isEmpty();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getPendingMap() {
        return ctx.getPendingMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getSuccessMap() {
        return ctx.getSuccessMap();
    }

    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getFailedMap() {
        return ctx.getFailedMap();
    }

    public ConcurrentMap<TenantId, RuleEngineException> getExceptionsMap() {
        return ctx.getExceptionsMap();
    }
}
