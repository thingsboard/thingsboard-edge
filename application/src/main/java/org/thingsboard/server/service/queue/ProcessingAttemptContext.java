/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ProcessingAttemptContext {

    private final TbRuleEngineSubmitStrategy submitStrategy;

    private final CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> successMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> failedMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<TenantId, RuleEngineException> exceptionsMap = new ConcurrentHashMap<>();

    public ProcessingAttemptContext(TbRuleEngineSubmitStrategy submitStrategy) {
        this.submitStrategy = submitStrategy;
        this.pendingMap = submitStrategy.getPendingMap();
    }

    public boolean await(long packProcessingTimeout, TimeUnit milliseconds) throws InterruptedException {
        return processingTimeoutLatch.await(packProcessingTimeout, milliseconds);
    }

    public void onSuccess(UUID id) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        synchronized (pendingMap) {
            msg = pendingMap.remove(id);
            if (msg != null) {
                empty = pendingMap.isEmpty();
            }
        }
        if (msg != null) {
            successMap.put(id, msg);
        }
        submitStrategy.onSuccess(id);
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    public void onFailure(TenantId tenantId, UUID id, RuleEngineException e) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        synchronized (pendingMap) {
            msg = pendingMap.remove(id);
            if (msg != null) {
                empty = pendingMap.isEmpty();
            }
        }
        if (msg != null) {
            failedMap.put(id, msg);
            exceptionsMap.putIfAbsent(tenantId, e);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }
}
