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
package org.thingsboard.server.common.data.job.task;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.job.JobType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DummyTaskResult extends TaskResult {

    private DummyTaskFailure failure;

    @Builder
    private DummyTaskResult(boolean success, boolean discarded, DummyTaskFailure failure) {
        super(success, discarded);
        this.failure = failure;
    }

    public static DummyTaskResult success(DummyTask task) {
        return DummyTaskResult.builder()
                .success(true)
                .build();
    }

    public static DummyTaskResult failed(DummyTask task, Throwable error) {
        return DummyTaskResult.builder()
                .failure(DummyTaskFailure.builder()
                        .error(error.getMessage())
                        .number(task.getNumber())
                        .failAlways(task.isFailAlways())
                        .build())
                .build();
    }

    public static DummyTaskResult discarded(DummyTask task) {
        return DummyTaskResult.builder()
                .discarded(true)
                .build();
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    public static class DummyTaskFailure extends TaskFailure {

        private int number;
        private boolean failAlways;

    }

}
