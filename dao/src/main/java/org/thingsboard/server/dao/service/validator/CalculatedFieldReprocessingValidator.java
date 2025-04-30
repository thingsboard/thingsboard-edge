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
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;

import java.util.Map;

@Component
public class CalculatedFieldReprocessingValidator {

    public CFReprocessingValidationResponse validate(CalculatedField calculatedField) {
        CFReprocessingValidationResponse argsCheck = checkArguments(calculatedField.getConfiguration().getArguments());
        if (!argsCheck.isValid()) {
            return argsCheck;
        }

        CFReprocessingValidationResponse outputCheck = checkOutput(calculatedField.getConfiguration().getOutput());
        if (!outputCheck.isValid()) {
            return outputCheck;
        }

        return CFReprocessingValidationResponse.valid();
    }

    private CFReprocessingValidationResponse checkOutput(Output output) {
        if (output == null) {
            return CFReprocessingValidationResponse.invalid("Calculated field has no output defined.");
        }

        if (OutputType.ATTRIBUTES.equals(output.getType())) {
            return CFReprocessingValidationResponse.invalid("Calculated field with output type ATTRIBUTE cannot be reprocessed.");
        }

        return CFReprocessingValidationResponse.valid();
    }

    private CFReprocessingValidationResponse checkArguments(Map<String, Argument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return CFReprocessingValidationResponse.invalid("Calculated field has no arguments defined.");
        }

        boolean containsTelemetry = arguments.values().stream()
                .anyMatch(arg -> ArgumentType.TS_LATEST.equals(arg.getRefEntityKey().getType()) ||
                        ArgumentType.TS_ROLLING.equals(arg.getRefEntityKey().getType()));

        if (!containsTelemetry) {
            return CFReprocessingValidationResponse.invalid("Calculated field must contain at least one time series based argument (TS_LATEST or TS_ROLLING).");
        }

        return CFReprocessingValidationResponse.valid();
    }

    public record CFReprocessingValidationResponse(boolean isValid, String message) {

        public static CFReprocessingValidationResponse valid() {
            return new CFReprocessingValidationResponse(true, null);
        }

        public static CFReprocessingValidationResponse invalid(String message) {
            return new CFReprocessingValidationResponse(false, message);
        }

    }

}
