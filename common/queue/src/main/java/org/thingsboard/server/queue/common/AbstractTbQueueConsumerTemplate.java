/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.queue.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractTbQueueConsumerTemplate<R, T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private volatile boolean subscribed;
    protected volatile boolean stopped = false;
    protected volatile Set<TopicPartitionInfo> partitions;
    protected final Lock consumerLock = new ReentrantLock();

    @Getter
    private final String topic;

    public AbstractTbQueueConsumerTemplate(String topic) {
        this.topic = topic;
    }

    @Override
    public void subscribe() {
        consumerLock.lock();
        try {
            partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
            subscribed = false;
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        consumerLock.lock();
        try {
            this.partitions = partitions;
            subscribed = false;
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed && partitions == null) {
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                log.debug("Failed to await subscription", e);
            }
        } else {
            long pollStartTs = System.currentTimeMillis();
            consumerLock.lock();
            try {
                if (!subscribed) {
                    List<String> topicNames = partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
                    doSubscribe(topicNames);
                    subscribed = true;
                }

                List<R> records;
                if (partitions.isEmpty()) {
                    records = Collections.emptyList();
                } else {
                    records = doPoll(durationInMillis);
                }
                if (!records.isEmpty()) {
                    List<T> result = new ArrayList<>(records.size());
                    records.forEach(record -> {
                        try {
                            if (record != null) {
                                result.add(decode(record));
                            }
                        } catch (IOException e) {
                            log.error("Failed decode record: [{}]", record);
                            throw new RuntimeException("Failed to decode record: ", e);
                        }
                    });
                    return result;
                } else {
                    long pollDuration = System.currentTimeMillis() - pollStartTs;
                    if (pollDuration < durationInMillis) {
                        try {
                            Thread.sleep(durationInMillis - pollDuration);
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                log.error("Failed to wait.", e);
                            }
                        }
                    }
                }
            } finally {
                consumerLock.unlock();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
        consumerLock.lock();
        try {
            doCommit();
        } finally {
            consumerLock.unlock();
        }
    }

    @Override
    public void unsubscribe() {
        stopped = true;
        consumerLock.lock();
        try {
            doUnsubscribe();
        } finally {
            consumerLock.unlock();
        }
    }

    abstract protected List<R> doPoll(long durationInMillis);

    abstract protected T decode(R record) throws IOException;

    abstract protected void doSubscribe(List<String> topicNames);

    abstract protected void doCommit();

    abstract protected void doUnsubscribe();

}
