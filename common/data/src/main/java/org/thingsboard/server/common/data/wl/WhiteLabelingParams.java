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
package org.thingsboard.server.common.data.wl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Schema
@Data
@EqualsAndHashCode
public class WhiteLabelingParams {

    @Schema(description = "Logo image URL", example = "https://company.com/images/logo.png")
    protected String logoImageUrl;
    @Schema(description = "Logo image checksum. Used to detect the changes of the logo image.", accessMode = Schema.AccessMode.READ_ONLY)
    protected String logoImageChecksum;
    @Schema(description = "The height of a logo container. Logo image will be automatically scaled.")
    protected Integer logoImageHeight;
    @Schema(description = "White-labeled name of the platform", example = "My Company IoT Platform")
    protected String appTitle;
    @Schema(description = "JSON object that contains website icon url and type")
    protected Favicon favicon;
    @Schema(description = "Favicon image checksum. Used to detect the changes of the website icon", accessMode = Schema.AccessMode.READ_ONLY)
    protected String faviconChecksum;
    @Schema(description = "Complex JSON that describes structure of the Angular Material Palette. See [theming](https://material.angular.io/guide/theming) for more details")
    protected PaletteSettings paletteSettings;
    @Schema(description = "Base URL for help link")
    protected String helpLinkBaseUrl;
    @Schema(description = "Base URL for the repository with the UI help components (markdown)")
    protected String uiHelpBaseUrl;
    @Schema(description = "Enable or Disable help links")
    protected Boolean enableHelpLinks;
    @Schema(description = "Enable white-labeling", accessMode = Schema.AccessMode.READ_ONLY)
    protected boolean whiteLabelingEnabled = true;
    @Schema(description = "Show platform name and version on UI and login screen")
    protected Boolean showNameVersion;
    @Schema(description = "White-labeled platform name")
    protected String platformName;
    @Schema(description = "White-labeled platform version")
    protected String platformVersion;
    @Schema(description = "Custom CSS content")
    protected String customCss;

    public WhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        if (StringUtils.isEmpty(this.logoImageUrl)) {
            this.logoImageUrl = otherWlParams.logoImageUrl;
            this.logoImageChecksum = otherWlParams.logoImageChecksum;
        }
        if (this.logoImageHeight == null) {
            this.logoImageHeight = otherWlParams.logoImageHeight;
        }
        if (StringUtils.isEmpty(appTitle)) {
            this.appTitle = otherWlParams.appTitle;
        }
        if (favicon == null || StringUtils.isEmpty(favicon.getUrl())) {
            this.favicon = otherWlParams.favicon;
            this.faviconChecksum = otherWlParams.faviconChecksum;
        }
        if (this.paletteSettings == null) {
            this.paletteSettings = otherWlParams.paletteSettings;
        } else if (otherWlParams.paletteSettings != null) {
            this.paletteSettings.merge(otherWlParams.paletteSettings);
        }
        if (otherWlParams.helpLinkBaseUrl != null) {
            this.helpLinkBaseUrl = otherWlParams.helpLinkBaseUrl;
        }
        if (otherWlParams.uiHelpBaseUrl != null) {
            this.uiHelpBaseUrl = otherWlParams.uiHelpBaseUrl;
        }
        if (otherWlParams.enableHelpLinks != null) {
            this.enableHelpLinks = otherWlParams.enableHelpLinks;
        }
        if (this.showNameVersion == null) {
            this.showNameVersion = otherWlParams.showNameVersion;
            this.platformName = otherWlParams.platformName;
            this.platformVersion = otherWlParams.platformVersion;
        }
        if (!StringUtils.isEmpty(otherWlParams.customCss)) {
            if (StringUtils.isEmpty(this.customCss)) {
                this.customCss = otherWlParams.customCss;
            } else {
                this.customCss = otherWlParams.customCss + "\n" + this.customCss;
            }
        }
        return this;
    }

    public void prepareImages(String logoImageChecksum, String faviconChecksum) {
        if (!StringUtils.isEmpty(logoImageChecksum) && logoImageChecksum.equals(this.logoImageChecksum)) {
            this.logoImageUrl = null;
        }
        if (!StringUtils.isEmpty(faviconChecksum) && faviconChecksum.equals(this.faviconChecksum)) {
            this.favicon = null;
        }
    }

}
