///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { ActivationMethod } from '@shared/models/user.model';

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
  userRegistered = 'userRegistered'
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
    [MailTemplate.userRegistered, 'admin.mail-template.user-registered']
  ]
);

export interface MailTemplatesSettings {
  useSystemMailSettings?: any;
  [mailTemplate: string]: {
    subject: string;
    body: string;
  }
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
