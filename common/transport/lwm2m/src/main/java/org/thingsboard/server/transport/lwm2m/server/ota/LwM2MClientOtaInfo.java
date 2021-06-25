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
package org.thingsboard.server.transport.lwm2m.server.ota;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MSoftwareUpdateStrategy;

import java.util.Optional;

@Data
public class LwM2MClientOtaInfo {

    private final String endpoint;
    private final OtaPackageType type;

    private String baseUrl;

    private boolean targetFetchFailure;
    private String targetName;
    private String targetVersion;
    private String targetUrl;

    private boolean currentFetchFailure;
    private String currentName;
    private String currentVersion3;
    private String currentVersion5;
    private Integer deliveryMethod;

    //TODO: use value from device if applicable;
    private LwM2MFirmwareUpdateStrategy fwStrategy;
    private LwM2MSoftwareUpdateStrategy swStrategy;
    private FirmwareUpdateState updateState;
    private FirmwareUpdateResult updateResult;

    private String failedPackageId;
    private int retryAttempts;

    public LwM2MClientOtaInfo(String endpoint, OtaPackageType type, Integer strategyCode, String baseUrl) {
        this.endpoint = endpoint;
        this.type = type;
        this.fwStrategy = strategyCode != null ? LwM2MFirmwareUpdateStrategy.fromStrategyFwByCode(strategyCode) : LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY;
        this.baseUrl = baseUrl;
    }

    public void updateTarget(String targetName, String targetVersion, Optional<String> newFirmwareUrl) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.targetUrl = newFirmwareUrl.orElse(null);
    }

    public boolean isUpdateRequired() {
        if (StringUtils.isEmpty(targetName) || StringUtils.isEmpty(targetVersion) || !isSupported()) {
            return false;
        } else {
            String targetPackageId = getPackageId(targetName, targetVersion);
            String currentPackageIdUsingObject5 = getPackageId(currentName, currentVersion5);
            if (StringUtils.isNotEmpty(failedPackageId) && failedPackageId.equals(targetPackageId)) {
                return false;
            } else {
                if (targetPackageId.equals(currentPackageIdUsingObject5)) {
                    return false;
                } else if (StringUtils.isNotEmpty(currentVersion3)) {
                    return !currentVersion3.contains(targetPackageId);
                } else {
                    return true;
                }
            }
        }
    }

    public boolean isSupported() {
        return StringUtils.isNotEmpty(currentName) || StringUtils.isNotEmpty(currentVersion5) || StringUtils.isNotEmpty(currentVersion3);
    }

    public void setUpdateResult(FirmwareUpdateResult updateResult) {
        this.updateResult = updateResult;
        switch (updateResult) {
            case INITIAL:
                break;
            case UPDATE_SUCCESSFULLY:
                retryAttempts = 0;
                break;
            default:
                failedPackageId = getPackageId(targetName, targetVersion);
                break;
        }
    }

    private static String getPackageId(String name, String version) {
        return (StringUtils.isNotEmpty(name) ? name : "") + (StringUtils.isNotEmpty(version) ? version : "");
    }

}
