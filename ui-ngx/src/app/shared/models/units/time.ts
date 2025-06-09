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

import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TimeUnits = TimeMetricUnits;

export type TimeMetricUnits =
  | 'ns'
  | 'μs'
  | 'ms'
  | 's'
  | 'min'
  | 'h'
  | 'd'
  | 'wk'
  | 'mo'
  | 'yr';

const daysInYear = 365.25;

const METRIC: TbMeasureUnits<TimeMetricUnits> = {
  units: {
    ns: {
      name: 'unit.nanosecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000000000
    },
    μs: {
      name: 'unit.microsecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000000
    },
    ms: {
      name: 'unit.millisecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000
    },
    s: {
      name: 'unit.second',
      tags: ['duration', 'interval'],
      to_anchor: 1,
    },
    min: {
      name: 'unit.minute',
      tags: ['duration', 'interval'],
      to_anchor: 60,
    },
    h: {
      name: 'unit.hour',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60,
    },
    d: {
      name: 'unit.day',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24,
    },
    wk: {
      name: 'unit.week',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24 * 7,
    },
    mo: {
      name: 'unit.month',
      tags: ['duration', 'interval'],
      to_anchor: (60 * 60 * 24 * daysInYear) / 12,
    },
    yr: {
      name: 'unit.year',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24 * daysInYear,
    },
  }
};

const measure: TbMeasure<TimeUnits> = {
  METRIC,
};

export default measure;
