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

import { TranslateService } from '@ngx-translate/core';

export interface RateLimits {
  value: string;
  time: string;
}

export enum RateLimitsType {
  DEVICE_MESSAGES = 'DEVICE_MESSAGES',
  DEVICE_TELEMETRY_MESSAGES = 'DEVICE_TELEMETRY_MESSAGES',
  DEVICE_TELEMETRY_DATA_POINTS = 'DEVICE_TELEMETRY_DATA_POINTS',
  TENANT_MESSAGES = 'TENANT_MESSAGES',
  TENANT_TELEMETRY_MESSAGES = 'TENANT_TELEMETRY_MESSAGES',
  TENANT_TELEMETRY_DATA_POINTS = 'TENANT_TELEMETRY_DATA_POINTS',
  TENANT_SERVER_REST_LIMITS_CONFIGURATION = 'TENANT_SERVER_REST_LIMITS_CONFIGURATION',
  CUSTOMER_SERVER_REST_LIMITS_CONFIGURATION = 'CUSTOMER_SERVER_REST_LIMITS_CONFIGURATION',
  WS_UPDATE_PER_SESSION_RATE_LIMIT = 'WS_UPDATE_PER_SESSION_RATE_LIMIT',
  CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION = 'CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION',
  TENANT_ENTITY_EXPORT_RATE_LIMIT = 'TENANT_ENTITY_EXPORT_RATE_LIMIT',
  TENANT_ENTITY_IMPORT_RATE_LIMIT = 'TENANT_ENTITY_IMPORT_RATE_LIMIT'
}

export const rateLimitsLabelTranslationMap = new Map<RateLimitsType, string>(
  [
    [RateLimitsType.TENANT_MESSAGES, 'tenant-profile.rate-limits.transport-tenant-msg'],
    [RateLimitsType.TENANT_TELEMETRY_MESSAGES, 'tenant-profile.rate-limits.transport-tenant-telemetry-msg'],
    [RateLimitsType.TENANT_TELEMETRY_DATA_POINTS, 'tenant-profile.rate-limits.transport-tenant-telemetry-data-points'],
    [RateLimitsType.DEVICE_MESSAGES, 'tenant-profile.rate-limits.transport-device-msg'],
    [RateLimitsType.DEVICE_TELEMETRY_MESSAGES, 'tenant-profile.rate-limits.transport-device-telemetry-msg'],
    [RateLimitsType.DEVICE_TELEMETRY_DATA_POINTS, 'tenant-profile.rate-limits.transport-device-telemetry-data-points'],
    [RateLimitsType.TENANT_SERVER_REST_LIMITS_CONFIGURATION, 'tenant-profile.transport-tenant-msg-rate-limit'],
    [RateLimitsType.CUSTOMER_SERVER_REST_LIMITS_CONFIGURATION, 'tenant-profile.customer-rest-limits'],
    [RateLimitsType.WS_UPDATE_PER_SESSION_RATE_LIMIT, 'tenant-profile.ws-limit-updates-per-session'],
    [RateLimitsType.CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION, 'tenant-profile.cassandra-tenant-limits-configuration'],
    [RateLimitsType.TENANT_ENTITY_EXPORT_RATE_LIMIT, 'tenant-profile.tenant-entity-export-rate-limit'],
    [RateLimitsType.TENANT_ENTITY_IMPORT_RATE_LIMIT, 'tenant-profile.tenant-entity-import-rate-limit'],
  ]
);

export const rateLimitsDialogTitleTranslationMap = new Map<RateLimitsType, string>(
  [
    [RateLimitsType.TENANT_MESSAGES, 'tenant-profile.rate-limits.edit-transport-tenant-msg-title'],
    [RateLimitsType.TENANT_TELEMETRY_MESSAGES, 'tenant-profile.rate-limits.edit-transport-tenant-telemetry-msg-title'],
    [RateLimitsType.TENANT_TELEMETRY_DATA_POINTS, 'tenant-profile.rate-limits.edit-transport-tenant-telemetry-data-points-title'],
    [RateLimitsType.DEVICE_MESSAGES, 'tenant-profile.rate-limits.edit-transport-device-msg-title'],
    [RateLimitsType.DEVICE_TELEMETRY_MESSAGES, 'tenant-profile.rate-limits.edit-transport-device-telemetry-msg-title'],
    [RateLimitsType.DEVICE_TELEMETRY_DATA_POINTS, 'tenant-profile.rate-limits.edit-transport-device-telemetry-data-points-title'],
    [RateLimitsType.TENANT_SERVER_REST_LIMITS_CONFIGURATION, 'tenant-profile.rate-limits.edit-transport-tenant-msg-rate-limit-title'],
    [RateLimitsType.CUSTOMER_SERVER_REST_LIMITS_CONFIGURATION, 'tenant-profile.rate-limits.edit-customer-rest-limits-title'],
    [RateLimitsType.WS_UPDATE_PER_SESSION_RATE_LIMIT, 'tenant-profile.rate-limits.edit-ws-limit-updates-per-session-title'],
    [RateLimitsType.CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION, 'tenant-profile.rate-limits.edit-cassandra-tenant-limits-configuration-title'],
    [RateLimitsType.TENANT_ENTITY_EXPORT_RATE_LIMIT, 'tenant-profile.rate-limits.edit-tenant-entity-export-rate-limit-title'],
    [RateLimitsType.TENANT_ENTITY_IMPORT_RATE_LIMIT, 'tenant-profile.rate-limits.edit-tenant-entity-import-rate-limit-title'],
  ]
);

export function stringToRateLimitsArray(rateLimits: string): Array<RateLimits> {
  const result: Array<RateLimits> = [];
  if (rateLimits?.length > 0) {
    const rateLimitsArrays = rateLimits.split(',');
    for (let i = 0; i < rateLimitsArrays.length; i++) {
      const [value, time] = rateLimitsArrays[i].split(':');
      const rateLimitControl = {
        value,
        time
      };
      result.push(rateLimitControl);
    }
  }
  return result;
}

export function rateLimitsArrayToString(rateLimits: Array<RateLimits>): string {
  let result = '';
  for (let i = 0; i < rateLimits.length; i++) {
    result = result.concat(rateLimits[i].value, ':', rateLimits[i].time);
    if ((rateLimits.length > 1) && (i !== rateLimits.length - 1)) {
      result = result.concat(',');
    }
  }
  return result;
}

export function rateLimitsArrayToHtml(translate: TranslateService, rateLimitsArray: Array<RateLimits>): string {
  const rateLimitsHtml = rateLimitsArray.map((rateLimits, index) => {
    const isLast: boolean = index === rateLimitsArray.length - 1;
    return rateLimitsToHtml(translate, rateLimits, isLast);
  });
  let result: string;
  if (rateLimitsHtml.length > 1) {
    const butLessThanText = translate.instant('tenant-profile.rate-limits.but-less-than');
    result = rateLimitsHtml.join(' <span class="disabled">' + butLessThanText + '</span> ');
  } else {
    result = rateLimitsHtml[0];
  }
  return result;
}

function rateLimitsToHtml(translate: TranslateService, rateLimit: RateLimits, isLast: boolean): string {
  const value = rateLimit.value;
  const time = rateLimit.time;
  const operation = translate.instant('tenant-profile.rate-limits.messages-per');
  const seconds = translate.instant('tenant-profile.rate-limits.sec');
  const comma = isLast ? '' : ',';
  return `<span class="tb-rate-limits-value">${value}</span>
          <span>${operation}</span>
          <span class="tb-rate-limits-value"> ${time}</span>
          <span>${seconds}${comma}</span><br>`;
}
