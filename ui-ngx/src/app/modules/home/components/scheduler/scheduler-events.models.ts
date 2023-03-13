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

export enum schedulerCalendarView {
  month = 'month',
  week = 'week',
  day = 'day',
  listYear = 'listYear',
  listMonth = 'listMonth',
  listWeek = 'listWeek',
  listDay = 'listDay',
  agendaWeek = 'agendaWeek',
  agendaDay = 'agendaDay'
}

export const schedulerCalendarViewValueMap = new Map<schedulerCalendarView, string>(
  [
    [schedulerCalendarView.month, 'dayGridMonth'],
    [schedulerCalendarView.week, 'dayGridWeek'],
    [schedulerCalendarView.day, 'dayGridDay'],
    [schedulerCalendarView.listYear, 'listYear'],
    [schedulerCalendarView.listMonth, 'listMonth'],
    [schedulerCalendarView.listWeek, 'listWeek'],
    [schedulerCalendarView.listDay, 'listDay'],
    [schedulerCalendarView.agendaWeek, 'timeGridWeek'],
    [schedulerCalendarView.agendaDay, 'timeGridDay']
  ]
);

export const schedulerCalendarViewTranslationMap = new Map<schedulerCalendarView, string>(
  [
    [schedulerCalendarView.month, 'scheduler.month'],
    [schedulerCalendarView.week, 'scheduler.week'],
    [schedulerCalendarView.day, 'scheduler.day'],
    [schedulerCalendarView.listYear, 'scheduler.list-year'],
    [schedulerCalendarView.listMonth, 'scheduler.list-month'],
    [schedulerCalendarView.listWeek, 'scheduler.list-week'],
    [schedulerCalendarView.listDay, 'scheduler.list-day'],
    [schedulerCalendarView.agendaWeek, 'scheduler.agenda-week'],
    [schedulerCalendarView.agendaDay, 'scheduler.agenda-day']
  ]
);

export interface CustomSchedulerEventType {
  name: string;
  value: string;
  originator: boolean;
  msgType: boolean;
  metadata: boolean;
  template: string;
}

export interface SchedulerEventsWidgetSettings {
  title: string;
  displayCreatedTime: boolean;
  displayType: boolean;
  displayCustomer: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  defaultSortOrder: string;
  noDataDisplayMessage: string;
  enabledViews: 'both' | 'list' | 'calendar';
  forceDefaultEventType: string;
  customEventTypes: CustomSchedulerEventType[];
}
