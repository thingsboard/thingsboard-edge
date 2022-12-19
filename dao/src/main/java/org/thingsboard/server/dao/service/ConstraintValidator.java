/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.exception.DataValidationException;

import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ConstraintValidator {

    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    public static void validateFields(Object data) {
        validateFields(data, "Validation error: ");
    }

    public static void validateFields(Object data, String errorPrefix) {
        List<String> constraintsViolations = getConstraintsViolations(data);
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + String.join(", ", constraintsViolations));
        }
    }

    public static List<String> getConstraintsViolations(Object data) {
        return fieldsValidator.validate(data).stream()
                .map(constraintViolation -> {
                    Path propertyPath = constraintViolation.getPropertyPath();
                    String property = Iterators.getLast(propertyPath.iterator()).toString();
                    return property + " " + constraintViolation.getMessage();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private static void initializeValidators() {
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        ConstraintMapping constraintMapping = validatorConfiguration.createConstraintMapping();
        constraintMapping.constraintDefinition(NoXss.class).validatedBy(NoXssValidator.class);
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        validatorConfiguration.addMapping(constraintMapping);

        fieldsValidator = validatorConfiguration.buildValidatorFactory().getValidator();
    }

}
