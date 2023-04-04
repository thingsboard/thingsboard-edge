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
package org.thingsboard.integration.tuya.util;

public enum ServiceRPC {
    GET_STATUS("getStatus", "/v1.0/iot-03/devices/%s/status", false),
    GET_CATEGORY("getCategory", "/v1.0/iot-03/categories/%s/status", false),
    GET_LOGS("getLogs", "/v1.0/iot-03/devices/%s/logs", false),
    GET_REPORT_LOGS("getReportLogs", "/v1.0/iot-03/devices/%s/report-logs", false),
    GET_SPECIFICATION("getSpecification", "/v1.0/iot-03/devices/%s/specification", false),
    GET_FUNCTIONS("getFunctions", "/v1.0/iot-03/devices/%s/functions", false);

    public final String method;
    public final String path;
    public final boolean requiresParameter;

    ServiceRPC(String method, String path, Boolean requiresParameter) {
        this.method = method;
        this.path = path;
        this.requiresParameter = requiresParameter;
    }
}
