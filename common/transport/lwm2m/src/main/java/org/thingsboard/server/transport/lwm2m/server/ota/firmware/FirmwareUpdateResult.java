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
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

import lombok.Getter;

/**
 * FW Update Result
 * 0: Initial value. Once the updating process is initiated (Download /Update), this Resource MUST be reset to Initial value.
 * 1: Firmware updated successfully.
 * 2: Not enough flash memory for the new firmware package.
 * 3: Out of RAM during downloading process.
 * 4: Connection lost during downloading process.
 * 5: Integrity check failure for new downloaded package.
 * 6: Unsupported package type.
 * 7: Invalid URI.
 * 8: Firmware update failed.
 * 9: Unsupported protocol.
 */
public enum FirmwareUpdateResult {
    INITIAL(0, "Initial value", false),
    UPDATE_SUCCESSFULLY(1, "Firmware updated successfully", false),
    NOT_ENOUGH(2, "Not enough flash memory for the new firmware package", false),
    OUT_OFF_MEMORY(3, "Out of RAM during downloading process", false),
    CONNECTION_LOST(4, "Connection lost during downloading process", true),
    INTEGRITY_CHECK_FAILURE(5, "Integrity check failure for new downloaded package", true),
    UNSUPPORTED_TYPE(6, "Unsupported package type", false),
    INVALID_URI(7, "Invalid URI", false),
    UPDATE_FAILED(8, "Firmware update failed", false),
    UNSUPPORTED_PROTOCOL(9, "Unsupported protocol", false);

    @Getter
    private int code;
    @Getter
    private String type;
    @Getter
    private boolean again;

    FirmwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.again = isAgain;
    }

    public static FirmwareUpdateResult fromUpdateResultFwByType(String type) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result type  : %s", type));
    }

    public static FirmwareUpdateResult fromUpdateResultFwByCode(int code) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result code  : %s", code));
    }
}
