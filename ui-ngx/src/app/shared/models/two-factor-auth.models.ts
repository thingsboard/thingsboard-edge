///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

export interface TwoFactorAuthSettings {
  maxVerificationFailuresBeforeUserLockout: number;
  providers: Array<TwoFactorAuthProviderConfig>;
  totalAllowedTimeForVerification: number;
  useSystemTwoFactorAuthSettings: boolean;
  verificationCodeCheckRateLimit: string;
  minVerificationCodeSendPeriod: number;
}

export interface TwoFactorAuthSettingsForm extends TwoFactorAuthSettings{
  providers: Array<TwoFactorAuthProviderConfigForm>;
  verificationCodeCheckRateLimitEnable: boolean;
  verificationCodeCheckRateLimitNumber: number;
  verificationCodeCheckRateLimitTime: number;
}

export type TwoFactorAuthProviderConfig = Partial<TotpTwoFactorAuthProviderConfig | SmsTwoFactorAuthProviderConfig |
                                                    EmailTwoFactorAuthProviderConfig>;

export type TwoFactorAuthProviderConfigForm = Partial<TotpTwoFactorAuthProviderConfig | SmsTwoFactorAuthProviderConfig |
  EmailTwoFactorAuthProviderConfig> & TwoFactorAuthProviderFormConfig;

export interface TotpTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  issuerName: string;
}

export interface SmsTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  smsVerificationMessageTemplate: string;
  verificationCodeLifetime: number;
}

export interface EmailTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  verificationCodeLifetime: number;
}

export interface TwoFactorAuthProviderFormConfig {
  enable: boolean;
}

export enum TwoFactorAuthProviderType{
  TOTP = 'TOTP',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
  BACKUP_CODE = 'BACKUP_CODE'
}

interface GeneralTwoFactorAuthAccountConfig {
  providerType: TwoFactorAuthProviderType;
  useByDefault: boolean;
}

export interface TotpTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  authUrl: string;
}

export interface SmsTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  phoneNumber: string;
}

export interface EmailTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  email: string;
}

export interface BackupCodeTwoFactorAuthAccountConfig extends GeneralTwoFactorAuthAccountConfig {
  codesLeft: number;
  codes?: Array<string>;
}

export type TwoFactorAuthAccountConfig = TotpTwoFactorAuthAccountConfig | SmsTwoFactorAuthAccountConfig |
  EmailTwoFactorAuthAccountConfig | BackupCodeTwoFactorAuthAccountConfig;

export interface AccountTwoFaSettings {
  configs: AccountTwoFaSettingProviders;
}

export type AccountTwoFaSettingProviders = {
  [key in TwoFactorAuthProviderType]?: TwoFactorAuthAccountConfig;
};

export interface TwoFaProviderInfo {
  type: TwoFactorAuthProviderType;
  default: boolean;
  contact?: string;
  minVerificationCodeSendPeriod?: number;
}

export interface TwoFactorAuthProviderData {
  name: string;
  description: string;
  activatedHint: string;
}

export interface TwoFactorAuthProviderLoginData extends Omit<TwoFactorAuthProviderData, 'activatedHint'> {
  icon: string;
  placeholder: string;
}

export const twoFactorAuthProvidersData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
        name: 'security.2fa.provider.totp',
        description: 'security.2fa.provider.totp-description',
        activatedHint: 'security.2fa.provider.totp-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
        name: 'security.2fa.provider.sms',
        description: 'security.2fa.provider.sms-description',
        activatedHint: 'security.2fa.provider.sms-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
        name: 'security.2fa.provider.email',
        description: 'security.2fa.provider.email-description',
        activatedHint: 'security.2fa.provider.email-hint'
      }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
        name: 'security.2fa.provider.backup_code',
        description: 'security.2fa.provider.backup-code-description',
        activatedHint: 'security.2fa.provider.backup-code-hint'
      }
    ]
  ]
);

export const twoFactorAuthProvidersLoginData = new Map<TwoFactorAuthProviderType, TwoFactorAuthProviderLoginData>(
  [
    [
      TwoFactorAuthProviderType.TOTP, {
        name: 'security.2fa.provider.totp',
        description: 'login.totp-auth-description',
        placeholder: 'login.totp-auth-placeholder',
        icon: 'mdi:cellphone-key'
      }
    ],
    [
      TwoFactorAuthProviderType.SMS, {
        name: 'security.2fa.provider.sms',
        description: 'login.sms-auth-description',
        placeholder: 'login.sms-auth-placeholder',
        icon: 'mdi:message-reply-text-outline'
      }
    ],
    [
      TwoFactorAuthProviderType.EMAIL, {
        name: 'security.2fa.provider.email',
        description: 'login.email-auth-description',
        placeholder: 'login.email-auth-placeholder',
        icon: 'mdi:email-outline'
      }
    ],
    [
      TwoFactorAuthProviderType.BACKUP_CODE, {
        name: 'security.2fa.provider.backup_code',
        description: 'login.backup-code-auth-description',
        placeholder: 'login.backup-code-auth-placeholder',
        icon: 'mdi:lock-outline'
      }
    ]
  ]
);
