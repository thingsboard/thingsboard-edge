/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
public enum LwM2mOperationType {

    READ(0, "Read", true),
    DISCOVER(1, "Discover", true),
    DISCOVER_ALL(2, "DiscoverAll", false),
    OBSERVE_READ_ALL(3, "ObserveReadAll", false),

    OBSERVE(4, "Observe", true),
    OBSERVE_CANCEL(5, "ObserveCancel", true),
    OBSERVE_CANCEL_ALL(6, "ObserveCancelAll", false),
    EXECUTE(7, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(8, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(9, "WriteUpdate", true),
    WRITE_ATTRIBUTES(10, "WriteAttributes", true),
    DELETE(11, "Delete", true),

    // only for RPC
    FW_UPDATE(12, "FirmwareUpdate", false);

//        FW_READ_INFO(12, "FirmwareReadInfo"),
//        SW_READ_INFO(15, "SoftwareReadInfo"),
//        SW_UPDATE(16, "SoftwareUpdate"),
//        SW_UNINSTALL(18, "SoftwareUninstall");

    @Getter
    private final int code;
    @Getter
    private final String type;
    @Getter
    private final boolean hasObjectId;

    LwM2mOperationType(int code, String type, boolean hasObjectId) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
    }

    public static LwM2mOperationType fromType(String type) {
        for (LwM2mOperationType to : LwM2mOperationType.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
