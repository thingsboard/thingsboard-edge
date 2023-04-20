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
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.ContentFormat;

public enum TbLwM2mVersion {
    VERSION_1_0(0, LwM2mVersion.V1_0, ContentFormat.TLV, false),
    VERSION_1_1(1, LwM2mVersion.V1_1, ContentFormat.TEXT, true);

    @Getter
    private final int code;
    @Getter
    private final LwM2mVersion version;
    @Getter
    private final ContentFormat contentFormat;
    @Getter
    private final boolean composite;

    TbLwM2mVersion(int code, LwM2mVersion version, ContentFormat contentFormat, boolean composite) {
        this.code = code;
        this.version = version;
        this.contentFormat = contentFormat;
        this.composite = composite;
    }

    public static TbLwM2mVersion fromVersion(LwM2mVersion version) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.equals(version)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeLwM2mVersion type : %s", version));
    }

    public static TbLwM2mVersion fromVersionStr(String versionStr) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.toString().equals(versionStr)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported contentFormatLwM2mVersion version : %s", versionStr));
    }

    public static TbLwM2mVersion fromCode(int code) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported codeLwM2mVersion code : %d", code));
    }
}

