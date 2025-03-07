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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class TbelCfTsMultiDoubleVal implements TbelCfObject {

    public static final long OBJ_SIZE = 32L; // Approximate calculation;

    private final long ts;
    private final double[] values;

    @JsonIgnore
    public double getV1() {
        return getV(0);
    }

    @JsonIgnore
    public double getV2() {
        return getV(1);
    }

    @JsonIgnore
    public double getV3() {
        return getV(2);
    }

    @JsonIgnore
    public double getV4() {
        return getV(3);
    }

    @JsonIgnore
    public double getV5() {
        return getV(4);
    }

    private double getV(int idx) {
        if (values.length < idx + 1) {
            throw new IllegalArgumentException("Can't get value at index " + idx + ". There are " + values.length + " values present.");
        } else {
            return values[idx];
        }
    }

    @Override
    public long memorySize() {
        return OBJ_SIZE + values.length * 8L;
    }
}
