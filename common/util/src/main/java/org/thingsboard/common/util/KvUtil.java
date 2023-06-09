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
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.kv.KvEntry;

public class KvUtil {

    public static String getStringValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(String::valueOf).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(String::valueOf).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(String::valueOf).orElse(null);
            case STRING:
                return entry.getStrValue().orElse("");
            case JSON:
                return entry.getJsonValue().orElse("");
            default:
                return null;
        }
    }

    public static Double getDoubleValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(Long::doubleValue).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(e -> e ? 1.0 : 0).orElse(null);
            case STRING:
                try {
                    return Double.parseDouble(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    public static Boolean getBoolValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(e -> e != 0).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(e -> e != 0).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().orElse(null);
            case STRING:
                try {
                    return Boolean.parseBoolean(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

}
