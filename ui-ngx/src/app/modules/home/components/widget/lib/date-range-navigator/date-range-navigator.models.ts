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

import * as _moment from 'moment';

export type DateRangeInterval = 'hour' | 'day' | 'week' | 'twoWeeks' | 'month' | 'threeMonths' | 'sixMonths';

export interface DateRangeNavigatorSettings {
  hidePicker: boolean;
  onePanel: boolean;
  autoConfirm: boolean;
  showTemplate: boolean;
  firstDayOfWeek: number;
  hideInterval: boolean;
  initialInterval: DateRangeInterval;
  hideStepSize: boolean;
  stepSize: DateRangeInterval;
  hideLabels: boolean;
  useSessionStorage: boolean;
}

export interface DateIntervalEntry {
  ts: number;
  label: string;
}

export interface DateRangeNavigatorModel {
  chosenLabel?: string;
  startDate?: _moment.Moment;
  endDate?: _moment.Moment;
}

export function cloneDateRangeNavigatorModel(model: DateRangeNavigatorModel): DateRangeNavigatorModel {
  const cloned: DateRangeNavigatorModel = {};
  cloned.chosenLabel = model.chosenLabel;
  cloned.startDate = model.startDate ? model.startDate.clone() : undefined;
  cloned.endDate = model.endDate ? model.endDate.clone() : undefined;
  return cloned;
}

export function getFormattedDate(model: DateRangeNavigatorModel): string {
  let template: string;

  const startDate = model.startDate;
  const endDate = model.endDate;

  if (startDate.diff(endDate, 'days') === 0) {
    template = startDate.format('DD MMM YYYY'); // datePipe.transform(startDate, 'dd MMM yyyy');
  } else {
    let startDateFormat = 'DD';
    if (startDate.month() !== endDate.month() || startDate.year() !== endDate.year()) {
      startDateFormat += ' MMM';
    }
    if (startDate.year() !== endDate.year()) {
      startDateFormat += ' YYYY';
    }
    template = startDate.format(startDateFormat) + ' - ' + endDate.format('DD MMM YYYY');
  }
  return template;
}

const hour = 3600000;
const day = 86400000;
const week = 604800000;
const month = 2629743000;

export const dateIntervalsMap: {[key: string]: DateIntervalEntry} = {
  hour: {
    ts: hour,
    label: 'Hour'
  },
  day: {
    ts: day,
    label: 'Day'
  },
  week: {
    ts: week,
    label: 'Week'
  },
  twoWeeks: {
    ts: week * 2,
    label: '2 weeks'
  },
  month: {
    ts: month,
    label: 'Month'
  },
  threeMonths: {
    ts: month * 3,
    label: '3 months'
  },
  sixMonths: {
    ts: month * 6,
    label: '6 months'
  }
};
