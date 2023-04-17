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
package org.thingsboard.server.service.install.migrate;

import lombok.Data;

@Data
public class CassandraToSqlColumnData {

    private String value;
    private String originalValue;
    private int constraintCounter = 0;

    public CassandraToSqlColumnData(String value) {
        this.value = value;
        this.originalValue = value;
    }

    public int nextContraintCounter() {
        return ++constraintCounter;
    }

    public String getNextConstraintStringValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String newValue = this.originalValue + counter;
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = this.originalValue.substring(0, this.originalValue.length()-overflow) + counter;
        }
        return newValue;
    }

    public String getNextConstraintEmailValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String[] emailValues = this.originalValue.split("@");
        String newValue = emailValues[0] + "+" + counter + "@" + emailValues[1];
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = emailValues[0].substring(0, emailValues[0].length()-overflow) + "+" + counter + "@" + emailValues[1];
        }
        return newValue;
    }

    public String getLogValue() {
        if (this.value != null && this.value.length() > 255) {
            return this.value.substring(0, 255) + "...[truncated " + (this.value.length() - 255) + " symbols]";
        }
        return this.value;
    }

}
