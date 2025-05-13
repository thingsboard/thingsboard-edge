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

export type DynamicViscosityMetricUnits = 'Pa·s' | 'cP' | 'P' | 'N·s/m²' | 'dyn·s/cm²' | 'kg/(m·s)';
export type DynamicViscosityImperialUnits = 'lb/(ft·h)';

export type DynamicViscosityUnits = DynamicViscosityMetricUnits | DynamicViscosityImperialUnits;

const METRIC: TbMeasureUnits<DynamicViscosityMetricUnits> = {
  ratio: 2419.0883293091,
  units: {
    'Pa·s': {
      name: 'unit.pascal-second',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
    cP: {
      name: 'unit.centipoise',
      tags: ['fluid mechanics'],
      to_anchor: 0.001,
    },
    P: {
      name: 'unit.poise',
      tags: ['fluid mechanics'],
      to_anchor: 0.1,
    },
    'N·s/m²': {
      name: 'unit.newton-second-per-square-meter',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
    'dyn·s/cm²': {
      name: 'unit.dyne-second-per-square-centimeter',
      tags: ['fluid mechanics'],
      to_anchor: 0.1,
    },
    'kg/(m·s)': {
      name: 'unit.kilogram-per-meter-second',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<DynamicViscosityImperialUnits> = {
  ratio: 0.00041337887,
  units: {
    'lb/(ft·h)': {
      name: 'unit.pound-per-foot-hour',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DynamicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
