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
import { CustomerId } from '@shared/models/id/customer-id';
import { SchedulerEventId } from '@shared/models/id/scheduler-event-id';
import { EntityId } from '@shared/models/id/entity-id';
import * as moment_ from 'moment';

export enum SchedulerRepeatType {
  DAILY = 'DAILY',
  EVERY_N_DAYS = 'EVERY_N_DAYS',
  WEEKLY = 'WEEKLY',
  EVERY_N_WEEKS = 'EVERY_N_WEEKS',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
  TIMER = 'TIMER'
}

export const schedulerRepeatTypeTranslationMap = new Map<SchedulerRepeatType, string>(
  [
    [SchedulerRepeatType.DAILY, 'scheduler.daily'],
    [SchedulerRepeatType.EVERY_N_DAYS, 'scheduler.every-n-days'],
    [SchedulerRepeatType.WEEKLY, 'scheduler.weekly'],
    [SchedulerRepeatType.EVERY_N_WEEKS, 'scheduler.every-n-weeks'],
    [SchedulerRepeatType.MONTHLY, 'scheduler.monthly'],
    [SchedulerRepeatType.YEARLY, 'scheduler.yearly'],
    [SchedulerRepeatType.TIMER, 'scheduler.timer']
  ]
);

export const schedulerRepeatTypeToUnitMap = new Map<SchedulerRepeatType, moment_.unitOfTime.Base>(
  [
    [SchedulerRepeatType.MONTHLY, 'month'],
    [SchedulerRepeatType.YEARLY, 'year'],
  ]
);

export enum SchedulerTimeUnit {
  HOURS = 'HOURS',
  MINUTES = 'MINUTES',
  SECONDS = 'SECONDS'
}

export const schedulerTimeUnitToUnitMap = new Map<SchedulerTimeUnit, moment_.unitOfTime.Base>(
  [
    [SchedulerTimeUnit.HOURS, 'hours'],
    [SchedulerTimeUnit.MINUTES, 'minutes'],
    [SchedulerTimeUnit.SECONDS, 'seconds'],
  ]
);

export const schedulerTimeUnitTranslationMap = new Map<SchedulerTimeUnit, string>(
  [
    [SchedulerTimeUnit.HOURS, 'scheduler.hours'],
    [SchedulerTimeUnit.MINUTES, 'scheduler.minutes'],
    [SchedulerTimeUnit.SECONDS, 'scheduler.seconds']
  ]
);

export const schedulerTimeUnitRepeatTranslationMap = new Map<SchedulerTimeUnit, string>(
  [
    [SchedulerTimeUnit.HOURS, 'scheduler.every-hour'],
    [SchedulerTimeUnit.MINUTES, 'scheduler.every-minute'],
    [SchedulerTimeUnit.SECONDS, 'scheduler.every-second']
  ]
);

export const schedulerWeekday: string[] =
  [
    'scheduler.sunday',
    'scheduler.monday',
    'scheduler.tuesday',
    'scheduler.wednesday',
    'scheduler.thursday',
    'scheduler.friday',
    'scheduler.saturday'
  ];

export interface SchedulerEventSchedule {
  timezone?: string;
  startTime?: number;
  repeat?: {
    type: SchedulerRepeatType;
    endsOn: number;
    repeatOn?: number[];
    days?: number;
    weeks?: number;
    repeatInterval?: number;
    timeUnit?: SchedulerTimeUnit;
  };
}

export interface SchedulerEventInfo extends BaseData<SchedulerEventId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  originatorId?: EntityId;
  name: string;
  type: string;
  schedule: SchedulerEventSchedule;
  additionalInfo?: any;
}

export interface SchedulerEventWithCustomerInfo extends SchedulerEventInfo {
  customerTitle: string;
  customerIsPublic: boolean;
  typeName?: string;
}

export interface SchedulerEventConfiguration {
  originatorId?: EntityId;
  msgType?: string;
  msgBody?: any;
  metadata?: any;
}

export interface SchedulerEvent extends SchedulerEventInfo {
  configuration: SchedulerEventConfiguration;
}
