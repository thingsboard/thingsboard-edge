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
package org.thingsboard.server.service.entitiy.cf;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.common.data.job.JobStatus.PENDING;
import static org.thingsboard.server.common.data.job.JobStatus.QUEUED;
import static org.thingsboard.server.common.data.job.JobStatus.RUNNING;

@Component
@RequiredArgsConstructor
public class CalculatedFieldReprocessingValidator {

    public static final String NO_ARGUMENTS = "no arguments defined.";
    public static final String NO_TELEMETRY_ARGS = "at least one time series based argument ('Latest telemtry' or 'Time series rolling') should be specified.";
    public static final String NO_OUTPUT = "no output defined.";
    public static final String INVALID_OUTPUT_TYPE = "output type 'Attribute' is not supported.";

    private final JobService jobService;
    private final TbelInvokeService tbelInvokeService;
    private final ApiLimitService apiLimitService;

    public CFReprocessingValidationResponse validate(CalculatedField calculatedField) {
        return checkJobStatus(calculatedField.getTenantId(), calculatedField.getId())
                .or(() -> checkArguments(calculatedField.getConfiguration().getArguments()))
                .or(() -> checkExpression(calculatedField))
                .or(() -> checkOutput(calculatedField.getConfiguration().getOutput()))
                .orElse(CFReprocessingValidationResponse.valid());
    }

    private Optional<CFReprocessingValidationResponse> checkJobStatus(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        Job job = jobService.findLatestJobByKey(tenantId, calculatedFieldId.getId().toString());
        if (job != null && job.getStatus().isOneOf(QUEUED, PENDING, RUNNING)) {
            return Optional.of(CFReprocessingValidationResponse.invalid("Calculated field reprocessing is already " + job.getStatus().name().toLowerCase(), job.getStatus()));
        }
        return Optional.empty();
    }

    private Optional<CFReprocessingValidationResponse> checkArguments(Map<String, Argument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Optional.of(CFReprocessingValidationResponse.invalid(NO_ARGUMENTS));
        }
        boolean containsTelemetry = arguments.values().stream()
                .anyMatch(arg -> ArgumentType.TS_LATEST.equals(arg.getRefEntityKey().getType()) ||
                        ArgumentType.TS_ROLLING.equals(arg.getRefEntityKey().getType()));

        if (!containsTelemetry) {
            return Optional.of(CFReprocessingValidationResponse.invalid(NO_TELEMETRY_ARGS));
        }
        return Optional.empty();
    }

    private Optional<CFReprocessingValidationResponse> checkExpression(CalculatedField calculatedField) {
        CalculatedFieldCtx ctx = new CalculatedFieldCtx(calculatedField, tbelInvokeService, apiLimitService);
        try {
            ctx.init();
            return Optional.of(CFReprocessingValidationResponse.valid());
        } catch (Exception e) {
            return Optional.of(CFReprocessingValidationResponse.invalid(e.getMessage()));
        }
    }

    private Optional<CFReprocessingValidationResponse> checkOutput(Output output) {
        if (output == null) {
            return Optional.of(CFReprocessingValidationResponse.invalid(NO_OUTPUT));
        }
        if (OutputType.ATTRIBUTES.equals(output.getType())) {
            return Optional.of(CFReprocessingValidationResponse.invalid(INVALID_OUTPUT_TYPE));
        }
        return Optional.empty();
    }

    public record CFReprocessingValidationResponse(boolean isValid, String message, JobStatus lastJobStatus) {

        public static CFReprocessingValidationResponse valid() {
            return new CFReprocessingValidationResponse(true, null, null);
        }

        public static CFReprocessingValidationResponse invalid(String message) {
            return new CFReprocessingValidationResponse(false, "Calculated field cannot be reprocessed: " + message, null);
        }

        public static CFReprocessingValidationResponse invalid(String message, JobStatus jobStatus) {
            return new CFReprocessingValidationResponse(false, message, jobStatus);
        }

    }

}
