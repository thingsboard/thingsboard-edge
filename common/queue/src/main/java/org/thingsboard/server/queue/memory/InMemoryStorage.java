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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class InMemoryStorage {
    private static InMemoryStorage instance;
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage;
    private volatile boolean stopped;

    private InMemoryStorage() {
        storage = new ConcurrentHashMap<>();
        stopped = false;
    }

    public static InMemoryStorage getInstance() {
        if (instance == null) {
            synchronized (InMemoryStorage.class) {
                if (instance == null) {
                    instance = new InMemoryStorage();
                }
            }
        }
        return instance;
    }

    public boolean put(String topic, TbQueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    public <T extends TbQueueMsg> List<T> get(String topic, long durationInMillis) {
        if (storage.containsKey(topic)) {
            try {
                List<T> entities;
                T first = (T) storage.get(topic).poll(durationInMillis, TimeUnit.MILLISECONDS);
                if (first != null) {
                    entities = new ArrayList<>();
                    entities.add(first);
                    List<TbQueueMsg> otherList = new ArrayList<>();
                    storage.get(topic).drainTo(otherList, 999);
                    for (TbQueueMsg other : otherList) {
                        entities.add((T) other);
                    }
                } else {
                    entities = Collections.emptyList();
                }
                return entities;
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.warn("Queue was interrupted", e);
                }
            }
        }
        return Collections.emptyList();
    }

    public void stop() {
        stopped = true;
    }
}
