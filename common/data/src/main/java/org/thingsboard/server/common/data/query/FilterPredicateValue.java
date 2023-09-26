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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;

@Data
public class FilterPredicateValue<T> implements Serializable {

    @Getter
    @NoXss
    private final T defaultValue;
    @Getter
    @NoXss
    private final T userValue;
    @Getter
    @Valid
    private final DynamicValue<T> dynamicValue;

    public FilterPredicateValue(T defaultValue) {
        this(defaultValue, null, null);
    }

    @JsonCreator
    public FilterPredicateValue(@JsonProperty("defaultValue") T defaultValue,
                                @JsonProperty("userValue") T userValue,
                                @JsonProperty("dynamicValue") DynamicValue<T> dynamicValue) {
        this.defaultValue = defaultValue;
        this.userValue = userValue;
        this.dynamicValue = dynamicValue;
    }

    @JsonIgnore
    public T getValue() {
        if (this.userValue != null) {
            return this.userValue;
        } else {
            if (this.dynamicValue != null && this.dynamicValue.getResolvedValue() != null) {
                return this.dynamicValue.getResolvedValue();
            } else {
                return defaultValue;
            }
        }
    }

    public static FilterPredicateValue<Double> fromDouble(double value) {
        return new FilterPredicateValue<>(value);
    }

    public static FilterPredicateValue<String> fromString(String value) {
        return new FilterPredicateValue<>(value);
    }

    public static FilterPredicateValue<Boolean> fromBoolean(boolean value) {
        return new FilterPredicateValue<>(value);
    }
}
