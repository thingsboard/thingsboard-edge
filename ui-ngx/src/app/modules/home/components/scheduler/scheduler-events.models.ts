///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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

export interface SchedulerEventsWidgetSettings {
  title: string;
  displayCreatedTime: boolean;
  displayType: boolean;
  displayCustomer: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  defaultSortOrder: string;
  enabledViews: 'both' | 'list' | 'calendar';
  forceDefaultEventType: string;
  customEventTypes: {
    name: string;
    value: string;
    originator: boolean;
    msgType: boolean;
    metadata: boolean;
    template: string;
  }[]
}
