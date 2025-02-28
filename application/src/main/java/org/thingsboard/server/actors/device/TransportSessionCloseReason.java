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
package org.thingsboard.server.actors.device;

import lombok.Getter;

@Getter
public enum TransportSessionCloseReason {

    UNKNOWN_REASON(0, "Unknown Reason.", "Session closed with unknown reason."),
    CREDENTIALS_UPDATED(1, "device credentials updated!", "Close session due to device credentials update."),
    MAX_CONCURRENT_SESSIONS_LIMIT_REACHED(2, "max concurrent sessions limit reached per device!", "Remove eldest session (max concurrent sessions limit reached per device)."),
    SESSION_TIMEOUT(3, "session timeout!", "Close session due to session timeout."),
    RPC_DELIVERY_TIMEOUT(4, "RPC delivery failed!", "Close session due to RPC delivery failure.");

    private final int protoNumber;
    private final String notificationMessage;
    private final String logMessage;

    TransportSessionCloseReason(int protoNumber, String notificationMessage, String logMessage) {
        this.protoNumber = protoNumber;
        this.notificationMessage = notificationMessage;
        this.logMessage = logMessage;
    }

}
