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

export type SpeedUnits = SpeedMetricUnits | SpeedImperialUnits;

export type SpeedMetricUnits = 'm/s' | 'km/h' | 'mm/min' | 'mm/s';
export type SpeedImperialUnits = 'mph' | 'kt' | 'ft/s' | 'ft/min' | 'in/s' | 'in/h';

const METRIC: TbMeasureUnits<SpeedMetricUnits> = {
  ratio: 1 / 1.609344,
  units: {
    'm/s': {
      name: 'unit.meter-per-second',
      tags: ['velocity', 'pace', 'peak', 'peak to peak', 'root mean square (RMS)', 'vibration', 'wind speed', 'weather'],
      to_anchor: 3.6,
    },
    'km/h': {
      name: 'unit.kilometer-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 1,
    },
    'mm/min': {
      name: 'unit.millimeters-per-minute',
      tags: ['feed rate', 'cutting feed rate'],
      to_anchor: 0.06,
    },
    'mm/s': {
      name: 'unit.millimeters-per-second',
      tags: ['velocity', 'vibration rate'],
      to_anchor: 0.0036,
    },
  },
};

const IMPERIAL: TbMeasureUnits<SpeedImperialUnits> = {
  ratio: 1.609344,
  units: {
    mph: {
      name: 'unit.mile-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 1,
    },
    kt: {
      name: 'unit.knot',
      tags: ['velocity', 'pace'],
      to_anchor: 1.150779,
    },
    'ft/s': {
      name: 'unit.foot-per-second',
      tags: ['velocity', 'pace'],
      to_anchor: 0.681818,
    },
    'ft/min': {
      name: 'unit.foot-per-minute',
      tags: ['velocity', 'pace'],
      to_anchor: 0.0113636,
    },
    'in/s': {
      name: 'unit.inch-per-second',
      tags: ['velocity', 'pace'],
      to_anchor: 0.0568182,
    },
    'in/h': {
      name: 'unit.inch-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 0.00001578,
    },
  },
};

const measure: TbMeasure<SpeedUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
