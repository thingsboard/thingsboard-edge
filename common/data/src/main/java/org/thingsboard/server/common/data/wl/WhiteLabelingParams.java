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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@ApiModel
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhiteLabelingParams {

    @ApiModelProperty(position = 1, value = "Logo image URL", example = "https://company.com/images/logo.png")
    protected String logoImageUrl;
    @ApiModelProperty(position = 2, value = "The height of a logo container. Logo image will be automatically scaled.")
    protected Integer logoImageHeight;
    @ApiModelProperty(position = 3, value = "White-labeled name of the platform", example = "My Company IoT Platform")
    protected String appTitle;
    @ApiModelProperty(position = 4, value = "JSON object that contains website icon url and type")
    protected Favicon favicon;
    @ApiModelProperty(position = 5, value = "Complex JSON that describes structure of the Angular Material Palette. See [theming](https://material.angular.io/guide/theming) for more details")
    protected PaletteSettings paletteSettings;
    @ApiModelProperty(position = 6, value = "Base URL for help link")
    protected String helpLinkBaseUrl;
    @ApiModelProperty(position = 7, value = "Base URL for the repository with the UI help components (markdown)")
    protected String uiHelpBaseUrl;
    @ApiModelProperty(position = 8, value = "Enable or Disable help links")
    protected Boolean enableHelpLinks;
    @ApiModelProperty(position = 9, value = "Enable white-labeling", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    protected boolean whiteLabelingEnabled = true;
    @ApiModelProperty(position = 10, value = "Show platform name and version on UI and login screen")
    protected Boolean showNameVersion;
    @ApiModelProperty(position = 11, value = "White-labeled platform name")
    protected String platformName;
    @ApiModelProperty(position = 12, value = "White-labeled platform version")
    protected String platformVersion;
    @ApiModelProperty(position = 13, value = "Custom CSS content")
    protected String customCss;

    public WhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        if (StringUtils.isEmpty(this.logoImageUrl)) {
            this.logoImageUrl = otherWlParams.logoImageUrl;
        }
        if (this.logoImageHeight == null) {
            this.logoImageHeight = otherWlParams.logoImageHeight;
        }
        if (StringUtils.isEmpty(appTitle)) {
            this.appTitle = otherWlParams.appTitle;
        }
        if (favicon == null || StringUtils.isEmpty(favicon.getUrl())) {
            this.favicon = otherWlParams.favicon;
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
}
