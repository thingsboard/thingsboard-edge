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
package org.thingsboard.server.service.job.task;

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.DummyTask;
import org.thingsboard.server.common.data.job.task.DummyTaskResult;
import org.thingsboard.server.queue.task.TaskProcessor;

@RequiredArgsConstructor
public class DummyTaskProcessor extends TaskProcessor<DummyTask, DummyTaskResult> {

    @Override
    public DummyTaskResult process(DummyTask task) throws Exception {
        if (task.getProcessingTimeMs() > 0) {
            Thread.sleep(task.getProcessingTimeMs());
        }
        if (task.isFailAlways()) {
            throw new RuntimeException(task.getErrors().get(0));
        }
        if (task.getErrors() != null && task.getAttempt() <= task.getErrors().size()) {
            String error = task.getErrors().get(task.getAttempt() - 1);
            throw new RuntimeException(error);
        }
        return DummyTaskResult.success();
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
