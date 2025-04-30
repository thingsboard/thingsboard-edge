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
package org.thingsboard.server.queue.task;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TaskProcessorQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class TaskProcessor<T extends Task> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskProcessorQueueFactory queueFactory;
    @Autowired
    private JobStatsService statsService;

    private QueueConsumerManager<TbProtoQueueMsg<TaskProto>> taskConsumer;
    private ExecutorService consumerExecutor;

    private final Set<UUID> deletedTenants = ConcurrentHashMap.newKeySet();
    private final Set<UUID> discardedJobs = ConcurrentHashMap.newKeySet(); // fixme use caffeine

    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getJobType().name().toLowerCase() + "-task-consumer"));
        taskConsumer = QueueConsumerManager.<TbProtoQueueMsg<TaskProto>>builder() // fixme: should be consumer per partition
                .name(getJobType().name().toLowerCase() + "-tasks")
                .msgPackProcessor(this::processMsgs) // todo: max.poll.records = 1
                .pollInterval(125)
                .consumerCreator(() -> queueFactory.createTaskConsumer(getJobType()))
                .consumerExecutor(consumerExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        taskConsumer.subscribe();
        taskConsumer.launch();
    }

    @EventListener
    public void onComponentLifecycle(ComponentLifecycleMsg event) {
        EntityId entityId = event.getEntityId();
        switch (entityId.getEntityType()) {
            case JOB -> {
                if (event.getEvent() == ComponentLifecycleEvent.STOPPED) {
                    log.debug("Adding job {} to discarded", entityId);
                    addToDiscardedJobs(entityId.getId());
                }
            }
            case TENANT -> {
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    deletedTenants.add(entityId.getId());
                    log.debug("Adding tenant {} to deleted", entityId);
                }
            }
        }
    }

    private void processMsgs(List<TbProtoQueueMsg<TaskProto>> msgs, TbQueueConsumer<TbProtoQueueMsg<TaskProto>> consumer) throws Exception {
        for (TbProtoQueueMsg<TaskProto> msg : msgs) {
            try {
                Task task = JacksonUtil.fromString(msg.getValue().getValue(), Task.class);
                if (discardedJobs.contains(task.getJobId().getId())) {
                    log.info("Skipping task '{}' for cancelled job {}", task.getKey(), task.getJobId());
                    reportCancelled(task);
                    continue;
                } else if (deletedTenants.contains(task.getTenantId().getId())) {
                    log.info("Skipping task '{}' for deleted tenant {}", task.getKey(), task.getTenantId());
                    continue;
                }
                processTask((T) task);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to process msg: {}", msg, e);
            }
        }
        consumer.commit();
    }

    private void processTask(T task) throws Exception { // todo: timeout and task interruption
        task.setAttempt(task.getAttempt() + 1);
        log.info("Processing task: {}", task);
        try {
            process(task);
            reportSuccess(task);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process task (attempt {}): {}", task.getAttempt(), task, e);
            if (task.getAttempt() <= task.getRetries()) {
                processTask(task);
            } else {
                reportFailure(task, e);
            }
        }
    }

    public abstract void process(T task) throws Exception;

    private void reportSuccess(Task task) {
        TaskResult result = TaskResult.builder()
                .success(true)
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    private void reportFailure(Task task, Throwable error) {
        TaskResult result = TaskResult.builder()
                .failure(task.toFailure(error))
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    private void reportCancelled(Task task) {
        TaskResult result = TaskResult.builder()
                .discarded(true)
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    public void addToDiscardedJobs(UUID jobId) {
        discardedJobs.add(jobId);
    }

    @PreDestroy
    public void destroy() {
        taskConsumer.stop();
        consumerExecutor.shutdownNow();
    }


    public abstract JobType getJobType();

}
