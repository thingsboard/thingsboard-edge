/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;

@Data
@EqualsAndHashCode
public abstract class AbstractSelfRegistrationParams implements SelfRegistrationParams {

    @Schema(description = "The text message to appear on login form")
    protected String title;
    @Schema(description = "Captcha site key for 'I'm not a robot' validation")
    protected CaptchaParams captcha;
    @Schema(description = "List of sign-up form fields")
    protected List<SignUpField> signUpFields;
    @Schema(description = "Show or hide 'Privacy Policy'")
    protected Boolean showPrivacyPolicy;
    @Schema(description = "Show or hide 'Terms of Use'")
    protected Boolean showTermsOfUse;
    @Schema(description = "Email to use for notifications when new user self-registered.")
    protected String notificationEmail;
    @Schema(description = "Prefix to add to created customer")
    protected String customerTitlePrefix;
    @Schema(description = "Id of the customer group customer wil be added to.")
    protected String customerGroupId;
    @Schema(description = "Group Permissions to assign for the new customer user.")
    protected List<GroupPermission> permissions;

}
