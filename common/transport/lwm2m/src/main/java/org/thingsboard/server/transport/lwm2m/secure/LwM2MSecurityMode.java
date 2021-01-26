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
package org.thingsboard.server.transport.lwm2m.secure;

public enum LwM2MSecurityMode {

    PSK(0, "psk"),
    RPK(1, "rpk"),
    X509(2, "x509"),
    NO_SEC(3, "no_sec"),
    X509_EST(4, "x509_est"),
    REDIS(7, "redis"),
    DEFAULT_MODE(255, "default_mode");

    public int code;
    public String  subEndpoint;

    LwM2MSecurityMode(int code, String subEndpoint) {
        this.code = code;
        this.subEndpoint = subEndpoint;
    }

    public static LwM2MSecurityMode fromSecurityMode(long code) {
        return fromSecurityMode((int) code);
    }

    public static LwM2MSecurityMode fromSecurityMode(int code) {
        for (LwM2MSecurityMode sm : LwM2MSecurityMode.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security code : %d", code));
    }


    public static LwM2MSecurityMode fromSecurityMode(String  subEndpoint) {
        for (LwM2MSecurityMode sm : LwM2MSecurityMode.values()) {
            if (sm.subEndpoint.equals(subEndpoint)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security subEndpoint : %d", subEndpoint));
    }
}
