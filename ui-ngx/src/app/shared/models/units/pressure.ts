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

export type PressureUnits = PressureMetricUnits | PressureImperialUnits;

export type PressureMetricUnits =
  | 'Pa'
  | 'kPa'
  | 'MPa'
  | 'GPa'
  | 'hPa'
  | 'mb'
  | 'mbar'
  | 'bar'
  | 'kbar'
  | 'Torr'
  | 'mmHg'
  | 'atm'
  | 'Pa/m²'
  | 'N/mm²'
  | 'N/m²'
  | 'kN/m²'
  | 'kgf/m²'
  | 'Pa/cm²';

export type PressureImperialUnits = 'psi' | 'ksi' | 'inHg' | 'psi/in²' | 'tonf/in²';

const METRIC: TbMeasureUnits<PressureMetricUnits> = {
  ratio: 0.00014503768078,
  units: {
    Pa: {
      name: 'unit.pascal',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight'],
      to_anchor: 0.001,
    },
    kPa: {
      name: 'unit.kilopascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1,
    },
    MPa: {
      name: 'unit.megapascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1000,
    },
    GPa: {
      name: 'unit.gigapascal',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1000000,
    },
    hPa: {
      name: 'unit.hectopascal',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure'],
      to_anchor: 0.1,
    },
    mbar: {
      name: 'unit.millibar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 0.1,
    },
    mb: {
      name: 'unit.millibar',
      tags: ['atmospheric pressure', 'air pressure', 'weather', 'altitude', 'flight'],
      to_anchor: 0.1,
    },
    bar: {
      name: 'unit.bar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 100,
    },
    kbar: {
      name: 'unit.kilobar',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 100000,
    },
    Torr: {
      name: 'unit.torr',
      tags: ['force', 'compression', 'tension', 'vacuum pressure'],
      to_anchor: 101325 / 760000,
    },
    mmHg: {
      name: 'unit.millimeters-of-mercury',
      tags: ['force', 'compression', 'tension', 'vacuum pressure'],
      to_anchor: 0.133322,
    },
    atm: {
      name: 'unit.atmospheres',
      tags: ['force', 'compression', 'tension', 'atmospheric pressure'],
      to_anchor: 101.325,
    },
    'Pa/m²': {
      name: 'unit.pascal-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'N/mm²': {
      name: 'unit.newton-per-square-millimeter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 1000,
    },
    'N/m²': {
      name: 'unit.newton-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'kN/m²': {
      name: 'unit.kilonewton-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 1,
    },
    'kgf/m²': {
      name: 'unit.kilogram-force-per-square-meter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.00980665,
    },
    'Pa/cm²': {
      name: 'unit.pascal-per-square-centimeter',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PressureImperialUnits> = {
  ratio: 1 / 0.00014503768078,
  units: {
    psi: {
      name: 'unit.pounds-per-square-inch',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 0.001,
    },
    ksi: {
      name: 'unit.kilopound-per-square-inch',
      tags: ['force', 'compression', 'tension'],
      to_anchor: 1,
    },
    inHg: {
      name: 'unit.inch-of-mercury',
      tags: ['force', 'compression', 'tension', 'vacuum pressure','atmospheric pressure', 'barometric pressure'],
      to_anchor: 0.000491154,
    },
    'psi/in²': {
      name: 'unit.pound-per-square-inch',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 0.001,
    },
    'tonf/in²': {
      name: 'unit.ton-force-per-square-inch',
      tags: ['stress', 'mechanical strength'],
      to_anchor: 2,
    },
  },
};

const measure: TbMeasure<PressureUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
