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
package org.thingsboard.server.common.data.firmware;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.HasFirmware;
import org.thingsboard.server.common.data.id.FirmwareId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.common.data.firmware.FirmwareType.FIRMWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareType.SOFTWARE;

@Slf4j
public class FirmwareUtil {

    public static final List<String> ALL_FW_ATTRIBUTE_KEYS;

    public static final List<String> ALL_SW_ATTRIBUTE_KEYS;

    static {
        ALL_FW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (FirmwareKey key : FirmwareKey.values()) {
            ALL_FW_ATTRIBUTE_KEYS.add(getAttributeKey(FIRMWARE, key));

        }

        ALL_SW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (FirmwareKey key : FirmwareKey.values()) {
            ALL_SW_ATTRIBUTE_KEYS.add(getAttributeKey(SOFTWARE, key));

        }
    }

    public static List<String> getAttributeKeys(FirmwareType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return ALL_FW_ATTRIBUTE_KEYS;
            case SOFTWARE:
                return ALL_SW_ATTRIBUTE_KEYS;
        }
        return Collections.emptyList();
    }

    public static String getAttributeKey(FirmwareType type, FirmwareKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTargetTelemetryKey(FirmwareType type, FirmwareKey key) {
        return getTelemetryKey("target_", type, key);
    }

    public static String getCurrentTelemetryKey(FirmwareType type, FirmwareKey key) {
        return getTelemetryKey("current_", type, key);
    }

    private static String getTelemetryKey(String prefix, FirmwareType type, FirmwareKey key) {
        return prefix + type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTelemetryKey(FirmwareType type, FirmwareKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    public static FirmwareId getFirmwareId(HasFirmware entity, FirmwareType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return entity.getFirmwareId();
            case SOFTWARE:
                return entity.getSoftwareId();
            default:
                log.warn("Unsupported firmware type: [{}]", firmwareType);
                return null;
        }
    }
}
