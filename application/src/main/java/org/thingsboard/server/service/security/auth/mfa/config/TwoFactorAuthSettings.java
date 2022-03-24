/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.security.auth.mfa.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

@Data
@ApiModel
public class TwoFactorAuthSettings {

    @ApiModelProperty(value = "Option for tenant admins to use 2FA settings configured by sysadmin. " +
            "If this param is set to true, then the settings will not be validated for constraints " +
            "(if it is a tenant admin; for sysadmin this param is ignored)")
    private boolean useSystemTwoFactorAuthSettings;
    @ApiModelProperty(value = "The list of 2FA providers' configs. Users will only be allowed to use 2FA providers from this list.")
    @Valid
    private List<TwoFactorAuthProviderConfig> providers;

    @ApiModelProperty(value = "Rate limit configuration for verification code sending. The format is standard: 'amountOfRequests:periodInSeconds'. " +
            "The value of '1:60' would limit verification code sending requests to one per minute.", example = "1:60", required = false)
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code send rate limit configuration is invalid")
    private String verificationCodeSendRateLimit;
    @ApiModelProperty(value = "Rate limit configuration for verification code checking.", example = "3:900", required = false)
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code check rate limit configuration is invalid")
    private String verificationCodeCheckRateLimit;
    @ApiModelProperty(value = "Maximum number of verification failures before a user gets disabled.", example = "10", required = false)
    @Min(value = 0, message = "maximum number of verification failure before user lockout must be positive")
    private int maxVerificationFailuresBeforeUserLockout;
    @ApiModelProperty(value = "Total amount of time in seconds allotted for verification. " +
            "Basically, this property sets a lifetime for pre-verification token. If not set, default value of 30 minutes is used.", example = "3600", required = false)
    @Min(value = 1, message = "total amount of time allotted for verification must be greater than 0")
    private Integer totalAllowedTimeForVerification;


    public Optional<TwoFactorAuthProviderConfig> getProviderConfig(TwoFactorAuthProviderType providerType) {
        return Optional.ofNullable(providers)
                .flatMap(providersConfigs -> providersConfigs.stream()
                        .filter(providerConfig -> providerConfig.getProviderType() == providerType)
                        .findFirst());
    }

}
