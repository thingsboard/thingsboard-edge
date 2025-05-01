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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.DummyTask;
import org.thingsboard.server.common.data.job.task.DummyTaskResult;
import org.thingsboard.server.common.data.job.task.DummyTaskResult.DummyTaskFailure;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DummyJobProcessor implements JobProcessor {

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        DummyJobConfiguration configuration = job.getConfiguration();
        if (configuration.getGeneralError() != null) {
            for (int number = 1; number <= configuration.getSubmittedTasksBeforeGeneralError(); number++) {
                taskConsumer.accept(createTask(job, configuration, number, null, false));
            }
            Thread.sleep(configuration.getTaskProcessingTimeMs() * (configuration.getSubmittedTasksBeforeGeneralError() / 2)); // sleeping so that some tasks are processed
            throw new RuntimeException(configuration.getGeneralError());
        }

        int taskNumber = 1;
        for (int i = 0; i < configuration.getSuccessfulTasksCount(); i++) {
            taskConsumer.accept(createTask(job, configuration, taskNumber, null, false));
            taskNumber++;
        }
        if (configuration.getErrors() != null) {
            for (int i = 0; i < configuration.getFailedTasksCount(); i++) {
                taskConsumer.accept(createTask(job, configuration, taskNumber, configuration.getErrors(), false));
                taskNumber++;
            }
            for (int i = 0; i < configuration.getPermanentlyFailedTasksCount(); i++) {
                taskConsumer.accept(createTask(job, configuration, taskNumber, configuration.getErrors(), true));
                taskNumber++;
            }
        }
        return configuration.getSuccessfulTasksCount() + configuration.getFailedTasksCount() + configuration.getPermanentlyFailedTasksCount();
    }

    @Override
    public void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) throws Exception {
        for (TaskResult taskFailure : taskFailures) {
            DummyTaskFailure failure = ((DummyTaskResult) taskFailure).getFailure();
            taskConsumer.accept(createTask(job, job.getConfiguration(), failure.getNumber(), failure.isFailAlways() ?
                    List.of(failure.getError()) : Collections.emptyList(), failure.isFailAlways()));
        }
    }

    private DummyTask createTask(Job job, DummyJobConfiguration configuration, int number, List<String> errors, boolean failAlways) {
        return DummyTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .retries(configuration.getRetries())
                .number(number)
                .processingTimeMs(configuration.getTaskProcessingTimeMs())
                .errors(errors)
                .failAlways(failAlways)
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.DUMMY;
    }

}
