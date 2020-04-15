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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class TbPackCallback<T> implements TbCallback {
    private final CountDownLatch processingTimeoutLatch;
    private final ConcurrentMap<UUID, T> ackMap;
    private final ConcurrentMap<UUID, T> failedMap;
    private final UUID id;

    public TbPackCallback(UUID id,
                          CountDownLatch processingTimeoutLatch,
                          ConcurrentMap<UUID, T> ackMap,
                          ConcurrentMap<UUID, T> failedMap) {
        this.id = id;
        this.processingTimeoutLatch = processingTimeoutLatch;
        this.ackMap = ackMap;
        this.failedMap = failedMap;
    }

    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        T msg = ackMap.remove(id);
        if (msg != null && ackMap.isEmpty()) {
            processingTimeoutLatch.countDown();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        log.trace("[{}] ON FAILURE", id, t);
        T msg = ackMap.remove(id);
        if (msg != null) {
            failedMap.put(id, msg);
        }
        if (ackMap.isEmpty()) {
            processingTimeoutLatch.countDown();
        }
    }
}
