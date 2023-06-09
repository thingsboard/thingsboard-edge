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
package org.thingsboard.server.common.data.signup;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by igor on 12/13/16.
 */
@ApiModel
@ToString
public class SignUpRequest {

    @Getter
    @Setter
    @ApiModelProperty(position = 1, value = "First Name", example = "John")
    private String firstName;
    @Getter
    @Setter
    @ApiModelProperty(position = 2, value = "Last Name", example = "Doe")
    private String lastName;
    @Getter
    @Setter
    @ApiModelProperty(position = 3, value = "Email will be used for new user to login", example = "john.doe@company.com")
    private String email;
    @Getter
    @Setter
    @ApiModelProperty(position = 4, value = "New User Password", example = "secret")
    private String password;
    @Getter
    @Setter
    @ApiModelProperty(position = 5, value = "Response from reCAPTCHA validation")
    private String recaptchaResponse;
    @Getter
    @Setter
    @ApiModelProperty(position = 6, value = "For mobile apps only. Mobile app package name")
    private String pkgName;
    @Getter
    @Setter
    @ApiModelProperty(position = 7, value = "For mobile apps only. Mobile app secret")
    private String appSecret;

    public SignUpRequest() {
        super();
    }

}
