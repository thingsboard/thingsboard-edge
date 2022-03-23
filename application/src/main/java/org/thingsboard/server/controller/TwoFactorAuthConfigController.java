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
package org.thingsboard.server.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthConfigManager;
import org.thingsboard.server.service.security.auth.mfa.config.TwoFactorAuthSettings;
import org.thingsboard.server.service.security.auth.mfa.config.account.TotpTwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.config.account.TwoFactorAuthAccountConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthConfigController extends BaseController {

    private final TwoFactorAuthConfigManager twoFactorAuthConfigManager;
    private final TwoFactorAuthService twoFactorAuthService;


    @GetMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TwoFactorAuthAccountConfig getTwoFaAccountConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.PROFILE, Operation.READ);
        return twoFactorAuthConfigManager.getTwoFaAccountConfig(user.getTenantId(), user.getId()).orElse(null);
    }

    @PostMapping("/account/config/generate")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public TwoFactorAuthAccountConfig generateTwoFaAccountConfig(@RequestParam TwoFactorAuthProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.PROFILE, Operation.WRITE);
        return twoFactorAuthService.generateNewAccountConfig(user, providerType);
    }

    /* TMP */
    @PostMapping("/account/config/generate/qr")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void generateTwoFaAccountConfigWithQr(@RequestParam TwoFactorAuthProviderType providerType, HttpServletResponse response) throws Exception {
        TwoFactorAuthAccountConfig config = generateTwoFaAccountConfig(providerType);
        if (providerType == TwoFactorAuthProviderType.TOTP) {
            BitMatrix qr = new QRCodeWriter().encode(((TotpTwoFactorAuthAccountConfig) config).getAuthUrl(), BarcodeFormat.QR_CODE, 200, 200);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                MatrixToImageWriter.writeToStream(qr, "PNG", outputStream);
            }
        }
        response.setHeader("config", JacksonUtil.toString(config));
    }
    /* TMP */

    @PostMapping("/account/config/submit")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void submitTwoFaAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.PROFILE, Operation.WRITE);
        twoFactorAuthService.prepareVerificationCode(user, accountConfig, false);
    }

    @PostMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void verifyAndSaveTwoFaAccountConfig(@Valid @RequestBody TwoFactorAuthAccountConfig accountConfig,
                                                @RequestParam String verificationCode) throws Exception {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.PROFILE, Operation.WRITE);
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, verificationCode, accountConfig, false);
        if (verificationSuccess) {
            twoFactorAuthConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
        } else {
            throw new ThingsboardException("Verification code is incorrect", ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
    }

    @DeleteMapping("/account/config")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void deleteTwoFactorAuthAccountConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.PROFILE, Operation.WRITE);
        twoFactorAuthConfigManager.deleteTwoFaAccountConfig(user.getTenantId(), user.getId());
    }


    @GetMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public TwoFactorAuthSettings getTwoFactorAuthSettings() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        } else {
            accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.READ);
        }
        return twoFactorAuthConfigManager.getTwoFaSettings(user.getTenantId(), false).orElse(null);
    }

    @PostMapping("/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void saveTwoFactorAuthSettings(@RequestBody TwoFactorAuthSettings twoFactorAuthSettings) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        if (user.getAuthority() == Authority.SYS_ADMIN) {
            accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        } else {
            accessControlService.checkPermission(user, Resource.WHITE_LABELING, Operation.WRITE);
        }
        twoFactorAuthConfigManager.saveTwoFaSettings(getTenantId(), twoFactorAuthSettings);
    }

}
