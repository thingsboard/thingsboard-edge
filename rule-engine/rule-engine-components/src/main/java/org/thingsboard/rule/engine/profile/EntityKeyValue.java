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
package org.thingsboard.rule.engine.profile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.kv.DataType;

@EqualsAndHashCode
class EntityKeyValue {

    @Getter
    private DataType dataType;
    private Long lngValue;
    private Double dblValue;
    private Boolean boolValue;
    private String strValue;

    public Long getLngValue() {
        return dataType == DataType.LONG ? lngValue : null;
    }

    public void setLngValue(Long lngValue) {
        this.dataType = DataType.LONG;
        this.lngValue = lngValue;
    }

    public Double getDblValue() {
        return dataType == DataType.DOUBLE ? dblValue : null;
    }

    public void setDblValue(Double dblValue) {
        this.dataType = DataType.DOUBLE;
        this.dblValue = dblValue;
    }

    public Boolean getBoolValue() {
        return dataType == DataType.BOOLEAN ? boolValue : null;
    }

    public void setBoolValue(Boolean boolValue) {
        this.dataType = DataType.BOOLEAN;
        this.boolValue = boolValue;
    }

    public String getStrValue() {
        return dataType == DataType.STRING ? strValue : null;
    }

    public void setStrValue(String strValue) {
        this.dataType = DataType.STRING;
        this.strValue = strValue;
    }

    public void setJsonValue(String jsonValue) {
        this.dataType = DataType.JSON;
        this.strValue = jsonValue;
    }

    public String getJsonValue() {
        return dataType == DataType.JSON ? strValue : null;
    }

    boolean isSet() {
        return dataType != null;
    }

    static EntityKeyValue fromString(String s) {
        EntityKeyValue result = new EntityKeyValue();
        result.setStrValue(s);
        return result;
    }

    static EntityKeyValue fromBool(boolean b) {
        EntityKeyValue result = new EntityKeyValue();
        result.setBoolValue(b);
        return result;
    }

    static EntityKeyValue fromLong(long l) {
        EntityKeyValue result = new EntityKeyValue();
        result.setLngValue(l);
        return result;
    }

    static EntityKeyValue fromDouble(double d) {
        EntityKeyValue result = new EntityKeyValue();
        result.setDblValue(d);
        return result;
    }

    static EntityKeyValue fromJson(String s) {
        EntityKeyValue result = new EntityKeyValue();
        result.setJsonValue(s);
        return result;
    }

}
