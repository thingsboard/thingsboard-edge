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
package org.thingsboard.server.queue.common.consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Slf4j
public class QueueConsumerManager<M extends TbQueueMsg> {

    private final String name;
    private final MsgPackProcessor<M> msgPackProcessor;
    private final long pollInterval;
    private final ExecutorService consumerExecutor;
    private final String threadPrefix;

    @Getter
    private final TbQueueConsumer<M> consumer;
    private volatile boolean stopped;

    @Builder
    public QueueConsumerManager(String name, MsgPackProcessor<M> msgPackProcessor,
                                long pollInterval, Supplier<TbQueueConsumer<M>> consumerCreator,
                                ExecutorService consumerExecutor, String threadPrefix) {
        this.name = name;
        this.pollInterval = pollInterval;
        this.msgPackProcessor = msgPackProcessor;
        this.consumerExecutor = consumerExecutor;
        this.threadPrefix = threadPrefix;
        this.consumer = consumerCreator.get();
    }

    public void subscribe() {
        consumer.subscribe();
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        consumer.subscribe(partitions);
    }

    public void launch() {
        log.info("[{}] Launching consumer", name);
        consumerExecutor.submit(() -> {
            if (threadPrefix != null) {
                ThingsBoardThreadFactory.addThreadNamePrefix(threadPrefix);
            }
            try {
                consumerLoop(consumer);
            } catch (Throwable e) {
                log.error("Failure in consumer loop", e);
            }
            log.info("[{}] Consumer stopped", name);
        });
    }

    private void consumerLoop(TbQueueConsumer<M> consumer) {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<M> msgs = consumer.poll(pollInterval);
                if (msgs.isEmpty()) {
                    continue;
                }
                msgPackProcessor.process(msgs, consumer);
            } catch (Exception e) {
                if (!consumer.isStopped()) {
                    log.warn("Failed to process messages from queue", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException interruptedException) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
                    }
                }
            }
        }
    }

    public void stop() {
        log.debug("[{}] Stopping consumer", name);
        stopped = true;
        consumer.unsubscribe();
    }

    public interface MsgPackProcessor<M extends TbQueueMsg> {
        void process(List<M> msgs, TbQueueConsumer<M> consumer) throws Exception;
    }

}
