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
package org.thingsboard.server.service.job;

import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.settings.TasksQueueConfig;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TbCoreComponent
@Component
@Slf4j
public class JobStatsProcessor {

    private final JobService jobService;
    private final TasksQueueConfig queueConfig;
    private final QueueConsumerManager<TbProtoQueueMsg<JobStatsMsg>> jobStatsConsumer;
    private final ExecutorService consumerExecutor;

    public JobStatsProcessor(JobService jobService,
                             TasksQueueConfig queueConfig,
                             TbCoreQueueFactory queueFactory) {
        this.jobService = jobService;
        this.queueConfig = queueConfig;
        this.consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("job-stats-consumer"));
        this.jobStatsConsumer = QueueConsumerManager.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .name("job-stats")
                .msgPackProcessor(this::processStats)
                .pollInterval(queueConfig.getStatsPollInterval())
                .consumerCreator(queueFactory::createJobStatsConsumer)
                .consumerExecutor(consumerExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        jobStatsConsumer.subscribe();
        jobStatsConsumer.launch();
    }

    @SneakyThrows
    private void processStats(List<TbProtoQueueMsg<JobStatsMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<JobStatsMsg>> consumer) {
        Map<JobId, JobStats> stats = new HashMap<>();

        for (TbProtoQueueMsg<JobStatsMsg> msg : msgs) {
            JobStatsMsg statsMsg = msg.getValue();
            TenantId tenantId = TenantId.fromUUID(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
            JobId jobId = new JobId(new UUID(statsMsg.getJobIdMSB(), statsMsg.getJobIdLSB()));
            JobStats jobStats = stats.computeIfAbsent(jobId, __ -> new JobStats(tenantId, jobId));

            if (statsMsg.hasTaskResult()) {
                TaskResult taskResult = JacksonUtil.fromString(statsMsg.getTaskResult().getValue(), TaskResult.class);
                jobStats.getTaskResults().add(taskResult);
            }
            if (statsMsg.hasTotalTasksCount()) {
                jobStats.setTotalTasksCount(statsMsg.getTotalTasksCount());
            }
        }

        stats.forEach((jobId, jobStats) -> {
            TenantId tenantId = jobStats.getTenantId();
            try {
                log.debug("[{}][{}] Processing job stats: {}", tenantId, jobId, stats);
                jobService.processStats(tenantId, jobId, jobStats);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process job stats: {}", tenantId, jobId, jobStats, e);
            }
        });
        consumer.commit();

        Thread.sleep(queueConfig.getStatsProcessingInterval());
    }

    @PreDestroy
    private void destroy() {
        jobStatsConsumer.stop();
        consumerExecutor.shutdownNow();
    }

}
