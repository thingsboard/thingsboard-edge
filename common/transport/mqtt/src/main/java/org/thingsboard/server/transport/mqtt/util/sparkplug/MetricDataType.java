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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;

import java.math.BigInteger;
import java.util.Date;

/**
 * Created by nickAS21 on 10.01.23
 */

@Slf4j
public enum MetricDataType {

    // Basic Types
    Int8(1, Byte.class),
    Int16(2, Short.class),
    Int32(3, Integer.class),
    Int64(4, Long.class),
    UInt8(5, Short.class),
    UInt16(6, Integer.class),
    UInt32(7, Long.class),
    UInt64(8, BigInteger.class),
    Float(9, Float.class),
    Double(10, Double.class),
    Boolean(11, Boolean.class),
    String(12, String.class),
    DateTime(13, Date.class),
    Text(14, String.class),

    // Custom Types for Metrics
    UUID(15, String.class),
    DataSet(16, SparkplugBProto.Payload.DataSet.class),
    Bytes(17, byte[].class),
    File(18, SparkplugMetricUtil.File.class),
    Template(19, SparkplugBProto.Payload.Template.class),

    // PropertyValue Types (20 and 21) are NOT metric datatypes

    // Unknown
    Unknown(0, Object.class);

    private Class<?> clazz = null;
    private int intValue = 0;

    /**
     * Constructor
     *
     * @param intValue the integer value of this {@link MetricDataType}
     * @param clazz    the {@link Class} type associated with this {@link MetricDataType}
     */
    private MetricDataType(int intValue, Class<?> clazz) {
        this.intValue = intValue;
        this.clazz = clazz;
    }

    /**
     * Checks the type of a specified value against the specified {@link MetricDataType}
     *
     * @param value the {@link Object} value to check against the {@link MetricDataType}
     * @throws AdaptorException if the value is not a valid type for the given {@link MetricDataType}
     */
    public void checkType(Object value) throws AdaptorException {
        if (value != null && !clazz.isAssignableFrom(value.getClass())) {
            String msgError = "Failed type check - " + clazz + " != " + ((value != null) ? value.getClass().toString() : "null");
            log.debug(msgError);
            throw new AdaptorException(msgError);
        }
    }

    /**
     * Returns an integer representation of the data type.
     *
     * @return an integer representation of the data type.
     */
    public int toIntValue() {
        return this.intValue;
    }

    /**
     * Converts the integer representation of the data type into a {@link MetricDataType} instance.
     *
     * @param i the integer representation of the data type.
     * @return a {@link MetricDataType} instance.
     */
    public static MetricDataType fromInteger(int i) {
        switch (i) {
            case 1:
                return Int8;
            case 2:
                return Int16;
            case 3:
                return Int32;
            case 4:
                return Int64;
            case 5:
                return UInt8;
            case 6:
                return UInt16;
            case 7:
                return UInt32;
            case 8:
                return UInt64;
            case 9:
                return Float;
            case 10:
                return Double;
            case 11:
                return Boolean;
            case 12:
                return String;
            case 13:
                return DateTime;
            case 14:
                return Text;
            case 15:
                return UUID;
            case 16:
                return DataSet;
            case 17:
                return Bytes;
            case 18:
                return File;
            case 19:
                return Template;
            default:
                return Unknown;
        }
    }

    /**
     * @return the class type for this DataType
     */
    public Class<?> getClazz() {
        return clazz;
    }


}