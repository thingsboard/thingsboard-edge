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
package org.thingsboard.server.common.data.mobile.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.validation.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MobileAppVersionInfo {

    @Schema(description = "Minimum supported version")
    @Length(fieldName = "minVersion", max = 20)
    private String minVersion;

    @Schema(description = "Release notes of minimum supported version")
    @Length(fieldName = "minVersionReleaseNotes", max = 40000)
    private String minVersionReleaseNotes;

    @Schema(description = "Latest supported version")
    @Length(fieldName = "latestVersion", max = 20)
    private String latestVersion;

    @Schema(description = "Release notes of latest supported version")
    @Length(fieldName = "latestVersionReleaseNotes", max = 40000)
    private String latestVersionReleaseNotes;

    public MobileAppVersionInfo(MobileAppVersionInfo mobileAppVersionInfo) {
        this.minVersion = mobileAppVersionInfo.getMinVersion();
        this.minVersionReleaseNotes = mobileAppVersionInfo.getMinVersionReleaseNotes();
        this.latestVersion = mobileAppVersionInfo.getLatestVersion();
        this.latestVersionReleaseNotes = mobileAppVersionInfo.getLatestVersionReleaseNotes();
    }

}
