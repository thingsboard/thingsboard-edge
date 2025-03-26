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
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;

import java.util.Set;

public final class DebugModeUtil {

    public static final int DEBUG_MODE_DEFAULT_DURATION_MINUTES = 15;

    private DebugModeUtil() {
    }

    public static int getMaxDebugAllDuration(int tenantProfileDuration, int systemDefaultDuration) {
        if (tenantProfileDuration > 0) {
            return tenantProfileDuration;
        } else {
            return systemDefaultDuration > 0 ? systemDefaultDuration : DEBUG_MODE_DEFAULT_DURATION_MINUTES;
        }
    }

    public static boolean isDebugAllAvailable(HasDebugSettings debugSettingsAware) {
        var debugSettings = debugSettingsAware.getDebugSettings();
        return debugSettings != null && debugSettings.getAllEnabledUntil() > System.currentTimeMillis();
    }

    public static boolean isDebugAvailable(HasDebugSettings debugSettingsAware, String nodeConnection) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled() &&
                    (TbNodeConnectionType.FAILURE.equals(nodeConnection) || "ERROR".equals(nodeConnection) || "FAILURE".equals(nodeConnection));
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware, Set<String> nodeConnections) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && nodeConnections != null && debugSettings.isFailuresEnabled() && nodeConnections.contains(TbNodeConnectionType.FAILURE);
        }
    }

    public static boolean isDebugIntegrationFailuresAvailable(HasDebugSettings debugSettingsAware) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled();
        }
    }

    public static boolean isDebugFailuresAvailable(HasDebugSettings debugSettingsAware) {
        if (isDebugAllAvailable(debugSettingsAware)) {
            return true;
        } else {
            var debugSettings = debugSettingsAware.getDebugSettings();
            return debugSettings != null && debugSettings.isFailuresEnabled();
        }
    }
    
}
