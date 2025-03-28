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
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import static org.thingsboard.server.common.data.selfregistration.CaptchaVersion.ENTERPRISE;

@Schema
@Data
@NoArgsConstructor
public class EnterpriseCaptchaParams implements CaptchaParams {

    @Schema(description = "Your Google Cloud project ID")
    protected String projectId;

    @Schema(description = "Service account credentials")
    private String serviceAccountCredentials;
    @Schema(description = "Service account credentials file name")
    private String serviceAccountCredentialsFileName;
    @Schema(description = "The reCAPTCHA key associated with android app.")
    protected String androidKey;
    @Schema(description = "The reCAPTCHA key associated with iOS app.")
    protected String iosKey;
    @Schema(description = "Optional action name used for logging")
    protected String logActionName;


    public EnterpriseCaptchaParams(String androidKey, String iOSKey, String logActionName) {
        this.androidKey = androidKey;
        this.iosKey = iOSKey;
        this.logActionName = logActionName;
    }

    @Override
    public String getVersion() {
        return ENTERPRISE.getName();
    }

    @Override
    public CaptchaParams toInfo(PlatformType platformType) {
        if (platformType == PlatformType.ANDROID) {
            return new EnterpriseCaptchaParams(androidKey, null, logActionName);
        } else if (platformType == PlatformType.IOS) {
            return new EnterpriseCaptchaParams( null, iosKey, logActionName);
        }
        return null;
    }
}
