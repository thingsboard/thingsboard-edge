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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TbMsgProfilerInfo {
    private final UUID msgId;
    private AtomicLong totalProcessingTime = new AtomicLong();
    private Lock stateLock = new ReentrantLock();
    private RuleNodeId currentRuleNodeId;
    private long stateChangeTime;

    public TbMsgProfilerInfo(UUID msgId) {
        this.msgId = msgId;
    }

    public void onStart(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            currentRuleNodeId = ruleNodeId;
            stateChangeTime = currentTime;
        } finally {
            stateLock.unlock();
        }
    }

    public long onEnd(RuleNodeId ruleNodeId) {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (ruleNodeId.equals(currentRuleNodeId)) {
                long processingTime = currentTime - stateChangeTime;
                stateChangeTime = currentTime;
                totalProcessingTime.addAndGet(processingTime);
                currentRuleNodeId = null;
                return processingTime;
            } else {
                log.trace("[{}] Invalid sequence of rule node processing detected. Expected [{}] but was [{}]", msgId, currentRuleNodeId, ruleNodeId);
                return 0;
            }
        } finally {
            stateLock.unlock();
        }
    }

    public Map.Entry<UUID, Long> onTimeout() {
        long currentTime = System.currentTimeMillis();
        stateLock.lock();
        try {
            if (currentRuleNodeId != null && stateChangeTime > 0) {
                long timeoutTime = currentTime - stateChangeTime;
                totalProcessingTime.addAndGet(timeoutTime);
                return new AbstractMap.SimpleEntry<>(currentRuleNodeId.getId(), timeoutTime);
            }
        } finally {
            stateLock.unlock();
        }
        return null;
    }
}
