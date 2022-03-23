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
public class TwoFactorAuthSettings {

    private boolean useSystemTwoFactorAuthSettings;
    @Valid
    private List<TwoFactorAuthProviderConfig> providers;

    @ApiModelProperty(example = "1:60 (1 request per minute)")
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code send rate limit configuration is invalid")
    private String verificationCodeSendRateLimit;
    @ApiModelProperty(example = "3:900 (3 requests per 15 minutes)")
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code check rate limit configuration is invalid")
    private String verificationCodeCheckRateLimit;
    @ApiModelProperty(example = "10")
    @Min(value = 0, message = "maximum number of verification failure before user lockout must be positive")
    private int maxVerificationFailuresBeforeUserLockout;
    @ApiModelProperty(value = "in seconds", example = "3600 (60 minutes)")
    @Min(value = 1, message = "total amount of time allotted for verification must be greater than 0")
    private Integer totalAllowedTimeForVerification;


    public Optional<TwoFactorAuthProviderConfig> getProviderConfig(TwoFactorAuthProviderType providerType) {
        return Optional.ofNullable(providers)
                .flatMap(providersConfigs -> providersConfigs.stream()
                        .filter(providerConfig -> providerConfig.getProviderType() == providerType)
                        .findFirst());
    }

}
