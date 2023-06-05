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

export enum LimitedApi {
  ENTITY_EXPORT = 'ENTITY_EXPORT',
  ENTITY_IMPORT = 'ENTITY_IMPORT',
  NOTIFICATION_REQUESTS = 'NOTIFICATION_REQUESTS',
  NOTIFICATION_REQUESTS_PER_RULE = 'NOTIFICATION_REQUESTS_PER_RULE',
  REST_REQUESTS_PER_TENANT = 'REST_REQUESTS_PER_TENANT',
  REST_REQUESTS_PER_CUSTOMER = 'REST_REQUESTS_PER_CUSTOMER',
  WS_UPDATES_PER_SESSION = 'WS_UPDATES_PER_SESSION',
  CASSANDRA_QUERIES = 'CASSANDRA_QUERIES',
  TRANSPORT_MESSAGES_PER_TENANT = 'TRANSPORT_MESSAGES_PER_TENANT',
  TRANSPORT_MESSAGES_PER_DEVICE = 'TRANSPORT_MESSAGES_PER_DEVICE'
}

export const LimitedApiTranslationMap = new Map<LimitedApi, string>(
  [
    [LimitedApi.ENTITY_EXPORT, 'api-limit.entity-version-creation'],
    [LimitedApi.ENTITY_IMPORT, 'api-limit.entity-version-load'],
    [LimitedApi.NOTIFICATION_REQUESTS, 'api-limit.notification-requests'],
    [LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, 'api-limit.notification-requests-per-rule'],
    [LimitedApi.REST_REQUESTS_PER_TENANT, 'api-limit.rest-api-requests'],
    [LimitedApi.REST_REQUESTS_PER_CUSTOMER, 'api-limit.rest-api-requests-per-customer'],
    [LimitedApi.WS_UPDATES_PER_SESSION, 'api-limit.ws-updates-per-session'],
    [LimitedApi.CASSANDRA_QUERIES, 'api-limit.cassandra-queries'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_TENANT, 'api-limit.transport-messages'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE, 'api-limit.transport-messages-per-device']
  ]
);
