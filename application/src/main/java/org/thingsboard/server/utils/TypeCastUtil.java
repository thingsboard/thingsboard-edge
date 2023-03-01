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
package org.thingsboard.server.utils;

import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.kv.DataType;

import java.math.BigDecimal;
import java.util.Map;

public class TypeCastUtil {

    private TypeCastUtil() {}

    public static Map.Entry<DataType, Object> castValue(String value) {
        if (isNumber(value)) {
            String formattedValue = value.replace(',', '.');
            try {
                BigDecimal bd = new BigDecimal(formattedValue);
                if (bd.stripTrailingZeros().scale() > 0 || isSimpleDouble(formattedValue)) {
                    if (bd.scale() <= 16) {
                        return Map.entry(DataType.DOUBLE, bd.doubleValue());
                    }
                } else {
                    return Map.entry(DataType.LONG, bd.longValueExact());
                }
            } catch (RuntimeException ignored) {}
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Map.entry(DataType.BOOLEAN, Boolean.parseBoolean(value));
        }
        return Map.entry(DataType.STRING, value);
    }

    private static boolean isNumber(String value) {
        return NumberUtils.isNumber(value.replace(',', '.'));
    }

    private static boolean isSimpleDouble(String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }

}
