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
package org.thingsboard.server.common.data.selfregistration;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;

@Data
@EqualsAndHashCode
public class SelfRegistrationParams extends SignUpSelfRegistrationParams {

    @ApiModelProperty(position = 1, name = "ID of the administration settings that store this parameters")
    private String adminSettingsId;
    @ApiModelProperty(position = 6, name = "Domain name for self registration URL. Typically this matches the domain name from the Login White Labeling page.")
    private String domainName;
    @ApiModelProperty(position = 7, name = "Secret key to validate the Captcha. Should match the Captcha Site Key.")
    private String captchaSecretKey;
    @ApiModelProperty(position = 8, name = "Privacy policy text. Supports HTML.")
    private String privacyPolicy;
    @ApiModelProperty(position = 9, name = "Terms of User text. Supports HTML.")
    private String termsOfUse;
    @ApiModelProperty(position = 10, name = "Email to use for notifications when new user self-registered.")
    private String notificationEmail;
    @ApiModelProperty(position = 11, name = "Default dashboard Id to assign for the new user.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String defaultDashboardId;
    @ApiModelProperty(position = 12, name = "Set default dashboard to full screen mode.")
    private boolean defaultDashboardFullscreen;
    @ApiModelProperty(position = 13, name = "Group Permissions to assign for the new customer user.")
    private List<GroupPermission> permissions;
    @ApiModelProperty(position = 14, name = "Mobile application verification settings. Package name filter. Contains id of android or iOS application.")
    private String pkgName;
    @ApiModelProperty(position = 15, name = "Mobile application verification settings. Used to verify the mobile application signup request.")
    private String appSecret;
    @ApiModelProperty(position = 16, name = "Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String appScheme;
    @ApiModelProperty(position = 17, name = "Mobile application verification settings. Used for callback to mobile application once user is registered.")
    private String appHost;

}
