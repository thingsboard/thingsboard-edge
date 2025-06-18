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

export type PowerDensityMetricUnits = 'mW/cm²' | 'W/cm²' | 'kW/cm²' | 'mW/m²' | 'W/m²' | 'kW/m²';
export type PowerDensityImperialUnits = 'W/in²' | 'kW/in²';

export type PowerDensityUnits = PowerDensityMetricUnits | PowerDensityImperialUnits;

const METRIC: TbMeasureUnits<PowerDensityMetricUnits> = {
  ratio: 0.00064516,
  units: {
    'mW/cm²': {
      name: 'unit.milliwatt-per-square-centimeter',
      tags: ['radiation intensity', 'sunlight intensity', 'signal power', 'intensity', 'UV Intensity'],
      to_anchor: 10000,
    },
    'W/cm²': {
      name: 'unit.watt-per-square-centimeter',
      tags: ['intensity of power'],
      to_anchor: 10000,
    },
    'kW/cm²': {
      name: 'unit.kilowatt-per-square-centimeter',
      tags: ['intensity of power'],
      to_anchor: 10000000,
    },
    'mW/m²': {
      name: 'unit.milliwatt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 0.001,
    },
    'W/m²': {
      name: 'unit.watt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 1,
    },
    'kW/m²': {
      name: 'unit.kilowatt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 1000,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PowerDensityImperialUnits> = {
  ratio: 1 / 0.00064516,
  units: {
    'W/in²': {
      name: 'unit.watt-per-square-inch',
      tags: ['intensity of power'],
      to_anchor: 1,
    },
    'kW/in²': {
      name: 'unit.kilowatt-per-square-inch',
      tags: ['intensity of power'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<PowerDensityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
