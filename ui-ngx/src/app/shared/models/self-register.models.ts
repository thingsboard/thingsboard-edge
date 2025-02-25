///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { GroupPermission } from '@shared/models/group-permission.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import { DomainId } from '@shared/models/id/domain-id';

export type CaptchaVersion = 'v2' | 'v3' | 'enterprise';

export enum SelfRegistrationType {
  WEB = 'WEB',
  MOBILE = 'MOBILE'
}

export enum SignUpFieldId {
  EMAIL = 'EMAIL',
  PASSWORD = 'PASSWORD',
  REPEAT_PASSWORD = 'REPEAT_PASSWORD',
  FIRST_NAME = 'FIRST_NAME',
  LAST_NAME = 'LAST_NAME',
  PHONE = 'PHONE',
  COUNTRY = 'COUNTRY',
  CITY = 'CITY',
  STATE = 'STATE',
  ZIP = 'ZIP',
  ADDRESS = 'ADDRESS',
  ADDRESS2 = 'ADDRESS2'
}

export interface SignUpField {
  id: SignUpFieldId,
  label: string;
  required: boolean;
}

export const SignUpFieldMap = new Map<SignUpFieldId, SignUpField>([
  [
    SignUpFieldId.EMAIL,
    {
      id: SignUpFieldId.EMAIL,
      label: 'user.email',
      required: true
    }
  ],
  [
    SignUpFieldId.PASSWORD,
    {
      id: SignUpFieldId.PASSWORD,
      label: 'common.password',
      required: true
    }
  ],
  [
    SignUpFieldId.REPEAT_PASSWORD,
    {
      id: SignUpFieldId.REPEAT_PASSWORD,
      label: 'common.repeat-password',
      required: true
    }
  ],
  [
    SignUpFieldId.FIRST_NAME,
    {
      id: SignUpFieldId.FIRST_NAME,
      label: 'user.first-name',
      required: false
    }
  ],
  [
    SignUpFieldId.LAST_NAME,
    {
      id: SignUpFieldId.LAST_NAME,
      label: 'user.last-name',
      required: false
    }
  ],
  [
    SignUpFieldId.PHONE,
    {
      id: SignUpFieldId.PHONE,
      label: 'contact.phone',
      required: false
    }
  ],
  [
    SignUpFieldId.PHONE,
    {
      id: SignUpFieldId.PHONE,
      label: 'contact.phone',
      required: false
    }
  ],
  [
    SignUpFieldId.COUNTRY,
    {
      id: SignUpFieldId.COUNTRY,
      label: 'contact.country',
      required: false
    }
  ],
  [
    SignUpFieldId.CITY,
    {
      id: SignUpFieldId.CITY,
      label: 'contact.city',
      required: false
    }
  ],
  [
    SignUpFieldId.STATE,
    {
      id: SignUpFieldId.STATE,
      label: 'contact.state',
      required: false
    }
  ],
  [
    SignUpFieldId.ZIP,
    {
      id: SignUpFieldId.ZIP,
      label: 'contact.postal-code',
      required: false
    }
  ],
  [
    SignUpFieldId.ADDRESS,
    {
      id: SignUpFieldId.ADDRESS,
      label: 'contact.address',
      required: false
    }
  ],
  [
    SignUpFieldId.ADDRESS2,
    {
      id: SignUpFieldId.ADDRESS2,
      label: 'contact.address2',
      required: false
    }
  ]
])

export const defaultSignUpFields: SignUpFieldId[] =
  [SignUpFieldId.FIRST_NAME, SignUpFieldId.LAST_NAME, SignUpFieldId.EMAIL, SignUpFieldId.PASSWORD, SignUpFieldId.REPEAT_PASSWORD];

export const alwaysRequiredSignUpFields: SignUpFieldId[] =
  [SignUpFieldId.EMAIL, SignUpFieldId.PASSWORD, SignUpFieldId.REPEAT_PASSWORD];

export interface SignUpSelfRegistrationParams {
  title?: string;
  captcha?: CaptchaParams;
  signUpFields: Array<SignUpField>;
  activate?: boolean;
  showPrivacyPolicy?: boolean;
  showTermsOfUse?: boolean;
}

export interface WebSelfRegistrationParams extends AbstractSelfRegistrationParams {
  domainId?: DomainId;
}

export interface AbstractSelfRegistrationParams {
  enabled: boolean;
  title: string;
  captcha?: CaptchaParams;
  signUpFields: Array<SignUpField>;
  showPrivacyPolicy: boolean;
  showTermsOfUse: boolean;
  privacyPolicy?: string;
  termsOfUse?: string;
  notificationRecipient: NotificationTargetId;
  customerTitlePrefix: string;
  customerGroupId: EntityGroupId;
  permissions: GroupPermission[];
  defaultDashboard: DefaultDashboardParams;
  homeDashboard: HomeDashboardParams;
  customMenuId?: CustomMenuId;
  type: SelfRegistrationType;
}

export interface SignUpField {
  id: SignUpFieldId;
  label: string;
  required: boolean;
}

interface HomeDashboardParams {
  id: string;
  hideToolbar: boolean;
}

export interface CaptchaParams {
  siteKey?: string;
  version: CaptchaVersion;
  logActionName?: string;
  secretKey?: string;
  projectId?: string;
  androidKey?: string;
  iosKey?: string;
  serviceAccountCredentials?: string;
  serviceAccountCredentialsFileName?: string;
}

interface DefaultDashboardParams {
  id: string;
  fullscreen: boolean;
}

export interface MobileSelfRegistrationParams extends AbstractSelfRegistrationParams {
  redirect: MobileRedirectParams;
}

interface MobileRedirectParams {
  scheme: string;
  host: string;
}
