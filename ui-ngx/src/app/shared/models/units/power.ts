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

export type PowerUnits = PowerMetricUnits | PowerImperialUnits;

export type PowerMetricUnits = 'W' | 'μW' | 'mW' | 'kW' | 'MW' | 'GW' | 'PS';
export type PowerImperialUnits = 'BTU/s' | 'ft-lb/s' | 'hp' | 'BTU/h';

const METRIC: TbMeasureUnits<PowerMetricUnits> = {
  ratio: 0.737562149,
  units: {
    W: {
      name: 'unit.watt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1,
    },
    μW: {
      name: 'unit.microwatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 0.000001,
    },
    mW: {
      name: 'unit.milliwatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 0.001,
    },
    kW: {
      name: 'unit.kilowatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1000,
    },
    MW: {
      name: 'unit.megawatt',
      tags: [ 'horsepower', 'performance', 'electricity'],
      to_anchor: 1000000,
    },
    GW: {
      name: 'unit.gigawatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1000000000,
    },
    PS: {
      name: 'unit.metric-horsepower',
      tags: ['performance'],
      to_anchor: 735.49875,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PowerImperialUnits> = {
  ratio: 1 / 0.737562149,
  units: {
    'BTU/s': {
      name: 'unit.btu-per-second',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 778.16937,
    },
    'ft-lb/s': {
      name: 'unit.foot-pound-per-second',
      tags: ['mechanical power'],
      to_anchor: 1,
    },
    hp: {
      name: 'unit.horsepower',
      tags: ['performance', 'electricity'],
      to_anchor: 550,
    },
    'BTU/h': {
      name: 'unit.btu-per-hour',
      tags: ['heat transfer', 'thermal energy', 'HVAC'],
      to_anchor: 0.216158,
    },
  },
};

const measure: TbMeasure<PowerUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
