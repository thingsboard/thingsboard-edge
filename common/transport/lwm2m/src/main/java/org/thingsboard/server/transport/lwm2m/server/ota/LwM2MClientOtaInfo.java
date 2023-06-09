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
package org.thingsboard.server.transport.lwm2m.server.ota;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;

import java.util.Optional;

@Data
@NoArgsConstructor
public abstract class LwM2MClientOtaInfo<Strategy, State, Result> {

    private String endpoint;
    private String baseUrl;

    protected String targetName;
    protected String targetVersion;
    protected String targetTag;
    protected String targetUrl;

    //TODO: use value from device if applicable;
    protected Strategy strategy;
    protected State updateState;
    protected Result result;
    protected OtaPackageUpdateStatus status;

    protected String failedPackageId;
    protected int retryAttempts;

    protected String currentName;
    protected String currentVersion3;
    protected String currentVersion;

    public LwM2MClientOtaInfo(String endpoint, String baseUrl, Strategy strategy) {
        this.endpoint = endpoint;
        this.baseUrl = baseUrl;
        this.strategy = strategy;
    }

    public void updateTarget(String targetName, String targetVersion, Optional<String> newTargetUrl, Optional<String> newTargetTag) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.targetUrl = newTargetUrl.orElse(null);
        this.targetTag = newTargetTag.orElse(null);
    }

    @JsonIgnore
    public boolean isUpdateRequired() {
        if (StringUtils.isEmpty(targetName) || StringUtils.isEmpty(targetVersion) || !isSupported()) {
            return false;
        } else {
            String targetPackageId = getPackageId(targetName, targetVersion);
            String currentPackageId = getPackageId(currentName, currentVersion);
            if (StringUtils.isNotEmpty(failedPackageId) && failedPackageId.equals(targetPackageId)) {
                return false;
            } else {
                if (targetPackageId.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(targetTag) && targetTag.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(currentVersion3)) {
                    if (StringUtils.isNotEmpty(targetTag) && currentVersion3.contains(targetTag)) {
                        return false;
                    }
                    return !currentVersion3.contains(targetPackageId);
                } else {
                    return true;
                }
            }
        }
    }

    @JsonIgnore
    public boolean isSupported() {
        return StringUtils.isNotEmpty(currentName) || StringUtils.isNotEmpty(currentVersion) || StringUtils.isNotEmpty(currentVersion3);
    }

    @JsonIgnore
    public boolean isAssigned() {
        return StringUtils.isNotEmpty(targetName) && StringUtils.isNotEmpty(targetVersion);
    }

    public abstract void update(Result result);

    protected static String getPackageId(String name, String version) {
        return (StringUtils.isNotEmpty(name) ? name : "") + (StringUtils.isNotEmpty(version) ? version : "");
    }

    public abstract OtaPackageType getType();

    @JsonIgnore
    public String getTargetPackageId() {
        return getPackageId(targetName, targetVersion);
    }
}
