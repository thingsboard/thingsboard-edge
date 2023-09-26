/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

@Slf4j
public class NoXssValidator implements ConstraintValidator<NoXss, Object> {
    private static final AntiSamy xssChecker = new AntiSamy();
    private static final Policy xssPolicy;

    static {
        xssPolicy = Optional.ofNullable(NoXssValidator.class.getClassLoader().getResourceAsStream("xss-policy.xml"))
                .map(inputStream -> {
                    try {
                        return Policy.getInstance(inputStream);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("XSS policy file not found"));
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        String stringValue;
        if (value instanceof CharSequence || value instanceof JsonNode) {
            stringValue = value.toString();
        } else {
            return true;
        }
        return isValid(stringValue);
    }

    public static boolean isValid(String stringValue) {
        if (stringValue.isEmpty()) {
            return true;
        }
        try {
            return xssChecker.scan(stringValue, xssPolicy).getNumberOfErrors() == 0;
        } catch (ScanException | PolicyException e) {
            return false;
        }
    }

}
