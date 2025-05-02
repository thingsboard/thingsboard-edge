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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskResultProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

@Lazy
@Service
@Slf4j
public class JobStatsService {

    private final TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> producer;

    public JobStatsService(TaskProcessorQueueFactory queueFactory) {
        this.producer = queueFactory.createJobStatsProducer();
    }

    public void reportTaskResult(TenantId tenantId, JobId jobId, TaskResult result) {
        report(tenantId, jobId, JobStatsMsg.newBuilder()
                .setTaskResult(TaskResultProto.newBuilder()
                        .setValue(JacksonUtil.toString(result))
                        .build()));
    }

    public void reportAllTasksSubmitted(TenantId tenantId, JobId jobId, int tasksCount) {
        report(tenantId, jobId, JobStatsMsg.newBuilder()
                .setTotalTasksCount(tasksCount));
    }

    private void report(TenantId tenantId, JobId jobId, JobStatsMsg.Builder statsMsg) {
        log.debug("[{}] Reporting: {}", jobId, statsMsg);
        statsMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setJobIdMSB(jobId.getId().getMostSignificantBits())
                .setJobIdLSB(jobId.getId().getLeastSignificantBits());

        TbProtoQueueMsg<JobStatsMsg> msg = new TbProtoQueueMsg<>(jobId.getId(), statsMsg.build());
        producer.send(TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build(), msg, TbQueueCallback.EMPTY);
    }

}
