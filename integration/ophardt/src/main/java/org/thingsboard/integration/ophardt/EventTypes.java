/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.ophardt;

public enum EventTypes {

    ACTIVATION(1),
    MAINVOLTAGE(2),
    BATTERYSTATECRITICAL(3),
    BATTERYSTATEGOOD(4),
    BATTERYSTATEUNKNOWN(5),
    BATTERYSTATEWEAK(6),
    BOTTLEIN(7),
    BOTTLEOUT(8),
    DEVICECLOSED(9),
    DEVICEOPENED(10),
    DEVICELOCKED(11),
    DEVICEUNLOCKED(12),
    DOORCLOSED(13),
    DOOROPENED(14),
    EMPTY(15),
    FILLINGLEVEL(16),
    FULL(17),
    INUSE(18),
    IR_ARRAY_MEASUREMENT(19),
    LIDOPEN(20),
    LIDCLOSED(21),
    MEASUREMENTERROR(22),
    PAPERIN(23),
    PAPEROUT(24),
    RESET(25),
    SENSORERROR(26),
    UNKNOWN(27),
    WASTELEVEL(28),
    FORMAT(29),
    WATCHDOG(30),
    NEW_SYSTEM_TIME(31),
    SYSTEM_ERROR(32);

    long value;

    public long getValue() {
        return value;
    }

    EventTypes(long value) {
        this.value = value;
    }

    public static String getEventTypeByValue(long value) {
        for (EventTypes eventType : EventTypes.values()) {
            if (eventType.getValue() == value) {
                return eventType.name();
            }
        }
        throw new RuntimeException("Invalid value for event type!");
    }

}
