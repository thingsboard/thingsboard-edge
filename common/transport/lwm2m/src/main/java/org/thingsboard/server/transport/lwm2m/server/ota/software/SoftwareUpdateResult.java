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
package org.thingsboard.server.transport.lwm2m.server.ota.software;

import lombok.Getter;

/**
 * SW Update Result
 * Contains the result of downloading or installing/uninstalling the software
 * 0: Initial value.
 * - Prior to download any new package in the Device, Update Result MUST be reset to this initial value.
 * - One side effect of executing the Uninstall resource is to reset Update Result to this initial value "0".
 * 1: Downloading.
 * - The package downloading process is on-going.
 * 2: Software successfully installed.
 * 3: Successfully Downloaded and package integrity verified
 * (( 4-49, for expansion, of other scenarios))
 * ** Failed
 * 50: Not enough storage for the new software package.
 * 51: Out of memory during downloading process.
 * 52: Connection lost during downloading process.
 * 53: Package integrity check failure.
 * 54: Unsupported package type.
 * 56: Invalid URI
 * 57: Device defined update error
 * 58: Software installation failure
 * 59: Uninstallation Failure during forUpdate(arg=0)
 * 60-200 : (for expansion, selection to be in blocks depending on new introduction of features)
 * This Resource MAY be reported by sending Observe operation.
 */
public enum SoftwareUpdateResult {
    INITIAL(0, "Initial value", false),
    DOWNLOADING(1, "Downloading", false),
    SUCCESSFULLY_INSTALLED(2, "Software successfully installed", false),
    SUCCESSFULLY_DOWNLOADED_VERIFIED(3, "Successfully Downloaded and package integrity verified", false),
    NOT_ENOUGH_STORAGE(50, "Not enough storage for the new software package", true),
    OUT_OFF_MEMORY(51, "Out of memory during downloading process", true),
    CONNECTION_LOST(52, "Connection lost during downloading process", false),
    PACKAGE_CHECK_FAILURE(53, "Package integrity check failure.", false),
    UNSUPPORTED_PACKAGE_TYPE(54, "Unsupported package type", false),
    INVALID_URI(56, "Invalid URI", true),
    UPDATE_ERROR(57, "Device defined update error", true),
    INSTALL_FAILURE(58, "Software installation failure", true),
    UN_INSTALL_FAILURE(59, "Uninstallation Failure during forUpdate(arg=0)", true);

    @Getter
    private int code;
    @Getter
    private String type;
    @Getter
    private boolean isAgain;

    SoftwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.isAgain = isAgain;
    }

    public static SoftwareUpdateResult fromUpdateResultSwByType(String type) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result type  : %s", type));
    }

    public static SoftwareUpdateResult fromUpdateResultSwByCode(int code) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result code  : %s", code));
    }
}
