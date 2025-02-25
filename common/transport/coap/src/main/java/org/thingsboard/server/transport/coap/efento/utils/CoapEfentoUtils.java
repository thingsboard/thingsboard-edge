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
package org.thingsboard.server.transport.coap.efento.utils;

import com.google.gson.JsonObject;
import org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_FLOODING;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OK_ALARM;
import static org.thingsboard.server.gen.transport.coap.MeasurementTypeProtos.MeasurementType.MEASUREMENT_TYPE_OUTPUT_CONTROL;

public class CoapEfentoUtils {

    public static final int PULSE_CNT_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int PULSE_CNT_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int ELEC_METER_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int ELEC_METER_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int PULSE_CNT_ACC_WIDE_MINOR_METADATA_FACTOR = 6;
    public static final int PULSE_CNT_ACC_WIDE_MAJOR_METADATA_FACTOR = 4;
    public static final int WATER_METER_ACC_MINOR_METADATA_FACTOR = 6;
    public static final int WATER_METER_ACC_MAJOR_METADATA_FACTOR = 4;
    public static final int IAQ_METADATA_FACTOR = 3;
    public static final int STATIC_IAQ_METADATA_FACTOR = 3;
    public static final int CO2_GAS_METADATA_FACTOR = 3;
    public static final int CO2_EQUIVALENT_METADATA_FACTOR = 3;
    public static final int BREATH_VOC_METADATA_FACTOR = 3;


    public static String convertByteArrayToString(byte[] a) {
        StringBuilder out = new StringBuilder();
        for (byte b : a) {
            out.append(String.format("%02X", b));
        }
        return out.toString();
    }

    public static String convertTimestampToUtcString(long timestampInMillis) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String utcZone = "UTC";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(utcZone));
        return String.format("%s UTC", simpleDateFormat.format(new Date(timestampInMillis)));
    }

    public static JsonObject setDefaultMeasurements(String serialNumber, boolean batteryStatus, long measurementPeriod, long nextTransmissionAtMillis, long signal, long startTimestampMillis) {
        JsonObject values = new JsonObject();
        values.addProperty("serial", serialNumber);
        values.addProperty("battery", batteryStatus ? "ok" : "low");
        values.addProperty("measured_at", convertTimestampToUtcString(startTimestampMillis));
        values.addProperty("next_transmission_at", convertTimestampToUtcString(nextTransmissionAtMillis));
        values.addProperty("signal", signal);
        values.addProperty("measurement_interval", measurementPeriod);
        return values;
    }

    public static boolean isBinarySensor(MeasurementType type) {
        return type == MEASUREMENT_TYPE_OK_ALARM || type == MEASUREMENT_TYPE_FLOODING || type == MEASUREMENT_TYPE_OUTPUT_CONTROL;
    }

    public static boolean isSensorError(int sampleOffset) {
        return sampleOffset >= 8355840 && sampleOffset <= 8388607;
    }

}
