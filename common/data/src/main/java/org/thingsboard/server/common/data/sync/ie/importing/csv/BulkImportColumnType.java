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
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Getter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

@Getter
public enum BulkImportColumnType {
    NAME,
    TYPE,
    LABEL,
    SHARED_ATTRIBUTE(DataConstants.SHARED_SCOPE, true),
    SERVER_ATTRIBUTE(DataConstants.SERVER_SCOPE, true),
    TIMESERIES(true),
    ACCESS_TOKEN,
    X509,
    MQTT_CLIENT_ID,
    MQTT_USER_NAME,
    MQTT_PASSWORD,
    LWM2M_CLIENT_ENDPOINT("endpoint"),
    LWM2M_CLIENT_SECURITY_CONFIG_MODE("securityConfigClientMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_CLIENT_IDENTITY("identity"),
    LWM2M_CLIENT_KEY("key"),
    LWM2M_CLIENT_CERT("cert"),
    LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE("securityMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID("clientPublicKeyOrId"),
    LWM2M_BOOTSTRAP_SERVER_SECRET_KEY("clientSecretKey"),
    LWM2M_SERVER_SECURITY_MODE("securityMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID("clientPublicKeyOrId"),
    LWM2M_SERVER_CLIENT_SECRET_KEY("clientSecretKey"),
    IS_GATEWAY,
    DESCRIPTION,
    EDGE_LICENSE_KEY,
    CLOUD_ENDPOINT,
    ROUTING_KEY,
    SECRET;

    private String key;
    private String defaultValue;
    private boolean isKv = false;

    BulkImportColumnType() {
    }

    BulkImportColumnType(String key) {
        this.key = key;
    }

    BulkImportColumnType(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    BulkImportColumnType(boolean isKv) {
        this.isKv = isKv;
    }

    BulkImportColumnType(String key, boolean isKv) {
        this.key = key;
        this.isKv = isKv;
    }
}
