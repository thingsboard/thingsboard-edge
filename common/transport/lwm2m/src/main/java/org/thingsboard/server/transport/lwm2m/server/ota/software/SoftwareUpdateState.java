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
 * SW Update State R
 * 0: INITIAL Before downloading. (see 5.1.2.1)
 * 1: DOWNLOAD STARTED The downloading process has started and is on-going. (see 5.1.2.2)
 * 2: DOWNLOADED The package has been completely downloaded  (see 5.1.2.3)
 * 3: DELIVERED In that state, the package has been correctly downloaded and is ready to be installed.  (see 5.1.2.4)
 * If executing the Install Resource failed, the state remains at DELIVERED.
 * If executing the Install Resource was successful, the state changes from DELIVERED to INSTALLED.
 * After executing the UnInstall Resource, the state changes to INITIAL.
 * 4: INSTALLED
 */
public enum SoftwareUpdateState {
    INITIAL(0, "Initial"),
    DOWNLOAD_STARTED(1, "DownloadStarted"),
    DOWNLOADED(2, "Downloaded"),
    DELIVERED(3, "Delivered"),
    INSTALLED(4, "Installed");

    @Getter
    private int code;
    @Getter
    private String type;

    SoftwareUpdateState(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static SoftwareUpdateState fromUpdateStateSwByType(String type) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", type));
    }

    public static SoftwareUpdateState fromUpdateStateSwByCode(int code) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", code));
    }
}

