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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { AlarmId } from '@shared/models/id/alarm-id';
import { EntityId } from '@shared/models/id/entity-id';
import { TimePageLink } from '@shared/models/page/page-link';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityType } from '@shared/models/entity-type.models';
import { CustomerId } from '@shared/models/id/customer-id';
import { TableCellButtonActionDescriptor } from '@home/components/widget/lib/table-widget.models';

export enum AlarmsMode {
  ALL,
  ENTITY
}

export enum AlarmSeverity {
  CRITICAL = 'CRITICAL',
  MAJOR = 'MAJOR',
  MINOR = 'MINOR',
  WARNING = 'WARNING',
  INDETERMINATE = 'INDETERMINATE'
}

export enum AlarmStatus {
  ACTIVE_UNACK = 'ACTIVE_UNACK',
  ACTIVE_ACK = 'ACTIVE_ACK',
  CLEARED_UNACK = 'CLEARED_UNACK',
  CLEARED_ACK = 'CLEARED_ACK'
}

export enum AlarmSearchStatus {
  ANY = 'ANY',
  ACTIVE = 'ACTIVE',
  CLEARED = 'CLEARED',
  ACK = 'ACK',
  UNACK = 'UNACK'
}

export const alarmSeverityTranslations = new Map<AlarmSeverity, string>(
  [
    [AlarmSeverity.CRITICAL, 'alarm.severity-critical'],
    [AlarmSeverity.MAJOR, 'alarm.severity-major'],
    [AlarmSeverity.MINOR, 'alarm.severity-minor'],
    [AlarmSeverity.WARNING, 'alarm.severity-warning'],
    [AlarmSeverity.INDETERMINATE, 'alarm.severity-indeterminate']
  ]
);

export const alarmStatusTranslations = new Map<AlarmStatus, string>(
  [
    [AlarmStatus.ACTIVE_UNACK, 'alarm.display-status.ACTIVE_UNACK'],
    [AlarmStatus.ACTIVE_ACK, 'alarm.display-status.ACTIVE_ACK'],
    [AlarmStatus.CLEARED_UNACK, 'alarm.display-status.CLEARED_UNACK'],
    [AlarmStatus.CLEARED_ACK, 'alarm.display-status.CLEARED_ACK'],
  ]
);

export const alarmSearchStatusTranslations = new Map<AlarmSearchStatus, string>(
  [
    [AlarmSearchStatus.ANY, 'alarm.search-status.ANY'],
    [AlarmSearchStatus.ACTIVE, 'alarm.search-status.ACTIVE'],
    [AlarmSearchStatus.CLEARED, 'alarm.search-status.CLEARED'],
    [AlarmSearchStatus.ACK, 'alarm.search-status.ACK'],
    [AlarmSearchStatus.UNACK, 'alarm.search-status.UNACK']
  ]
);

export const alarmSeverityColors = new Map<AlarmSeverity, string>(
  [
    [AlarmSeverity.CRITICAL, 'red'],
    [AlarmSeverity.MAJOR, 'orange'],
    [AlarmSeverity.MINOR, '#ffca3d'],
    [AlarmSeverity.WARNING, '#abab00'],
    [AlarmSeverity.INDETERMINATE, 'green']
  ]
);

export interface Alarm extends BaseData<AlarmId> {
  tenantId: TenantId;
  customerId: CustomerId;
  type: string;
  originator: EntityId;
  severity: AlarmSeverity;
  status: AlarmStatus;
  startTs: number;
  endTs: number;
  ackTs: number;
  clearTs: number;
  propagate: boolean;
  details?: any;
}

export interface AlarmInfo extends Alarm {
  originatorName: string;
}

export interface AlarmDataInfo extends AlarmInfo {
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
  [key: string]: any;
}

export const simulatedAlarm: AlarmInfo = {
  id: new AlarmId(NULL_UUID),
  tenantId: new TenantId(NULL_UUID),
  customerId: new CustomerId(NULL_UUID),
  createdTime: new Date().getTime(),
  startTs: new Date().getTime(),
  endTs: 0,
  ackTs: 0,
  clearTs: 0,
  originatorName: 'Simulated',
  originator: {
    entityType: EntityType.DEVICE,
    id: '1'
  },
  type: 'TEMPERATURE',
  severity: AlarmSeverity.MAJOR,
  status: AlarmStatus.ACTIVE_UNACK,
  details: {
    message: 'Temperature is high!'
  },
  propagate: false
};

export interface AlarmField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export const alarmFields: {[fieldName: string]: AlarmField} = {
  createdTime: {
    keyName: 'createdTime',
    value: 'createdTime',
    name: 'alarm.created-time',
    time: true
  },
  startTime: {
    keyName: 'startTime',
    value: 'startTs',
    name: 'alarm.start-time',
    time: true
  },
  endTime: {
    keyName: 'endTime',
    value: 'endTs',
    name: 'alarm.end-time',
    time: true
  },
  ackTime: {
    keyName: 'ackTime',
    value: 'ackTs',
    name: 'alarm.ack-time',
    time: true
  },
  clearTime: {
    keyName: 'clearTime',
    value: 'clearTs',
    name: 'alarm.clear-time',
    time: true
  },
  originator: {
    keyName: 'originator',
    value: 'originatorName',
    name: 'alarm.originator'
  },
  originatorType: {
    keyName: 'originatorType',
    value: 'originator.entityType',
    name: 'alarm.originator-type'
  },
  type: {
    keyName: 'type',
    value: 'type',
    name: 'alarm.type'
  },
  severity: {
    keyName: 'severity',
    value: 'severity',
    name: 'alarm.severity'
  },
  status: {
    keyName: 'status',
    value: 'status',
    name: 'alarm.status'
  }
};

export class AlarmQuery {

  affectedEntityId: EntityId;
  pageLink: TimePageLink;
  searchStatus: AlarmSearchStatus;
  status: AlarmStatus;
  fetchOriginator: boolean;

  constructor(entityId: EntityId, pageLink: TimePageLink,
              searchStatus: AlarmSearchStatus, status: AlarmStatus,
              fetchOriginator: boolean) {
    this.affectedEntityId = entityId;
    this.pageLink = pageLink;
    this.searchStatus = searchStatus;
    this.status = status;
    this.fetchOriginator = fetchOriginator;
  }

  public toQuery(): string {
    let query = this.affectedEntityId ? `/${this.affectedEntityId.entityType}/${this.affectedEntityId.id}` : '';
    query += this.pageLink.toQuery();
    if (this.searchStatus) {
      query += `&searchStatus=${this.searchStatus}`;
    } else if (this.status) {
      query += `&status=${this.status}`;
    }
    if (typeof this.fetchOriginator !== 'undefined' && this.fetchOriginator !== null) {
      query += `&fetchOriginator=${this.fetchOriginator}`;
    }
    return query;
  }

}
