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

export type AccelerationMetricUnits = 'g₀' | 'm/s²' | 'km/h²' | 'Gal';
export type AccelerationImperialUnits = 'ft/s²';

export type AccelerationUnits = AccelerationMetricUnits | AccelerationImperialUnits;

const METRIC: TbMeasureUnits<AccelerationMetricUnits> = {
  ratio: 3.28084,
  units: {
    'g₀': {
      name: 'unit.g-force',
      tags: ['gravity', 'load'],
      to_anchor: 9.80665,
    },
    'm/s²': {
      name: 'unit.meters-per-second-squared',
      tags: ['peak to peak', 'root mean square (RMS)', 'vibration'],
      to_anchor: 1,
    },
    Gal: {
      name: 'unit.gal',
      tags: ['gravity', 'g-force'],
      to_anchor: 1,
    },
    'km/h²': {
      name: 'unit.kilometer-per-hour-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1 / 12960,
    }
  }
};

const IMPERIAL: TbMeasureUnits<AccelerationImperialUnits> = {
  ratio: 1 / 3.28084,
  units: {
    'ft/s²': {
      name: 'unit.foot-per-second-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1
    }
  }
};

const measure: TbMeasure<AccelerationUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
