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

export enum ApiUsageStateValue {
  ENABLED = 'ENABLED',
  WARNING = 'WARNING',
  DISABLED = 'DISABLED'
}

export const ApiUsageStateValueTranslationMap = new Map<ApiUsageStateValue, string>([
  [ApiUsageStateValue.ENABLED, 'notification.enabled'],
  [ApiUsageStateValue.WARNING, 'notification.warning'],
  [ApiUsageStateValue.DISABLED, 'notification.disabled'],
]);

export enum ApiFeature {
  TRANSPORT = 'TRANSPORT',
  DB = 'DB',
  RE = 'RE',
  JS = 'JS',
  EMAIL = 'EMAIL',
  SMS = 'SMS',
  ALARM = 'ALARM'
}

export const ApiFeatureTranslationMap = new Map<ApiFeature, string>([
  [ApiFeature.TRANSPORT, 'api-usage.device-api'],
  [ApiFeature.DB, 'api-usage.telemetry-persistence'],
  [ApiFeature.RE, 'api-usage.rule-engine-executions'],
  [ApiFeature.JS, 'api-usage.javascript-executions'],
  [ApiFeature.EMAIL, 'api-usage.email-messages'],
  [ApiFeature.SMS, 'api-usage.sms-messages'],
  [ApiFeature.ALARM, 'api-usage.alarm'],
]);
