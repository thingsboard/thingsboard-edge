///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { ValidatorFn } from '@angular/forms';
import { isNotEmptyStr } from '@core/utils';

export const smtpPortPattern: RegExp = /^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;

export interface AdminSettings<T> {
  key: string;
  jsonValue: T;
}

export declare type SmtpProtocol = 'smtp' | 'smtps';

export interface MailServerSettings {
  useSystemMailSettings: boolean;
  mailFrom: string;
  smtpProtocol: SmtpProtocol;
  smtpHost: string;
  smtpPort: number;
  timeout: number;
  enableTls: boolean;
  username: string;
  password: string;
  enableProxy: boolean;
  proxyHost: string;
  proxyPort: number;
  proxyUser: string;
  proxyPassword: string;
}

export interface GeneralSettings {
  baseUrl: string;
}

export enum MailTemplate {
  test = 'test',
  activation = 'activation',
  accountActivated = 'accountActivated',
  accountLockout = 'accountLockout',
  resetPassword = 'resetPassword',
  passwordWasReset = 'passwordWasReset',
  userActivated = 'userActivated',
  userRegistered = 'userRegistered',
  apiUsageStateEnabled = 'apiUsageStateEnabled',
  apiUsageStateWarning = 'apiUsageStateWarning',
  apiUsageStateDisabled = 'apiUsageStateDisabled',
}

export const mailTemplateTranslations = new Map<MailTemplate, string>(
  [
    [MailTemplate.test, 'admin.mail-template.test'],
    [MailTemplate.activation, 'admin.mail-template.activation'],
    [MailTemplate.accountActivated, 'admin.mail-template.account-activated'],
    [MailTemplate.accountLockout, 'admin.mail-template.account-lockout'],
    [MailTemplate.resetPassword, 'admin.mail-template.reset-password'],
    [MailTemplate.passwordWasReset, 'admin.mail-template.password-was-reset'],
    [MailTemplate.userActivated, 'admin.mail-template.user-activated'],
    [MailTemplate.userRegistered, 'admin.mail-template.user-registered'],
    [MailTemplate.apiUsageStateEnabled, 'admin.mail-template.api-usage-state-enabled'],
    [MailTemplate.apiUsageStateWarning, 'admin.mail-template.api-usage-state-warning'],
    [MailTemplate.apiUsageStateDisabled, 'admin.mail-template.api-usage-state-disabled']
  ]
);

export interface MailTemplatesSettings {
  useSystemMailSettings?: any;
  [mailTemplate: string]: {
    subject: string;
    body: string;
  };
}

export interface UserPasswordPolicy {
  minimumLength: number;
  minimumUppercaseLetters: number;
  minimumLowercaseLetters: number;
  minimumDigits: number;
  minimumSpecialCharacters: number;
  passwordExpirationPeriodDays: number;
}

export interface SecuritySettings {
  passwordPolicy: UserPasswordPolicy;
}

export interface UpdateMessage {
  message: string;
  updateAvailable: boolean;
}

export const phoneNumberPattern = /^\+[1-9]\d{1,14}$/;
export const phoneNumberPatternTwilio = /^\+[1-9]\d{1,14}$|^(MG|PN).*$/;

export enum SmsProviderType {
  AWS_SNS = 'AWS_SNS',
  TWILIO = 'TWILIO'
}

export const smsProviderTypeTranslationMap = new Map<SmsProviderType, string>(
  [
    [SmsProviderType.AWS_SNS, 'admin.sms-provider-type-aws-sns'],
    [SmsProviderType.TWILIO, 'admin.sms-provider-type-twilio']
  ]
);

export interface AwsSnsSmsProviderConfiguration {
  accessKeyId?: string;
  secretAccessKey?: string;
  region?: string;
}

export interface TwilioSmsProviderConfiguration {
  accountSid?: string;
  accountToken?: string;
  numberFrom?: string;
}

export type SmsProviderConfigurations = AwsSnsSmsProviderConfiguration & TwilioSmsProviderConfiguration;

export interface SmsProviderConfiguration extends SmsProviderConfigurations {
  useSystemSmsSettings?: boolean;
  type: SmsProviderType;
}

export function smsProviderConfigurationValidator(required: boolean): ValidatorFn {
  return control => {
    const configuration: SmsProviderConfiguration = control.value;
    let errors = null;
    if (required) {
      let valid = false;
      if (configuration && configuration.type) {
        switch (configuration.type) {
          case SmsProviderType.AWS_SNS:
            const awsSnsConfiguration: AwsSnsSmsProviderConfiguration = configuration;
            valid = isNotEmptyStr(awsSnsConfiguration.accessKeyId) && isNotEmptyStr(awsSnsConfiguration.secretAccessKey)
              && isNotEmptyStr(awsSnsConfiguration.region);
            break;
          case SmsProviderType.TWILIO:
            const twilioConfiguration: TwilioSmsProviderConfiguration = configuration;
            valid = isNotEmptyStr(twilioConfiguration.numberFrom) && isNotEmptyStr(twilioConfiguration.accountSid)
              && isNotEmptyStr(twilioConfiguration.accountToken);
            break;
        }
      }
      if (!valid) {
        errors = {
          invalid: true
        };
      }
    }
    return errors;
  };
}

export interface TestSmsRequest {
  providerConfiguration: SmsProviderConfiguration;
  numberTo: string;
  message: string;
}

export function createSmsProviderConfiguration(type: SmsProviderType): SmsProviderConfiguration {
  let smsProviderConfiguration: SmsProviderConfiguration;
  if (type) {
    switch (type) {
      case SmsProviderType.AWS_SNS:
        const awsSnsSmsProviderConfiguration: AwsSnsSmsProviderConfiguration = {
          accessKeyId: '',
          secretAccessKey: '',
          region: 'us-east-1'
        };
        smsProviderConfiguration = {...awsSnsSmsProviderConfiguration, type: SmsProviderType.AWS_SNS};
        break;
      case SmsProviderType.TWILIO:
        const twilioSmsProviderConfiguration: TwilioSmsProviderConfiguration = {
          numberFrom: '',
          accountSid: '',
          accountToken: ''
        };
        smsProviderConfiguration = {...twilioSmsProviderConfiguration, type: SmsProviderType.TWILIO};
        break;
    }
  }
  return smsProviderConfiguration;
}
