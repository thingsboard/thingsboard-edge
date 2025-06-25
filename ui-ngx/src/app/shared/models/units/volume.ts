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

export type VolumeUnits = VolumeMetricUnits | VolumeImperialUnits;

export type VolumeMetricUnits =
  | 'mm³'
  | 'cm³'
  | 'µL'
  | 'mL'
  | 'L'
  | 'hL'
  | 'm³'
  | 'km³';

export type VolumeImperialUnits =
  | 'tsp'
  | 'tbsp'
  | 'in³'
  | 'fl-oz'
  | 'cup'
  | 'pt'
  | 'qt'
  | 'gal'
  | 'ft³'
  | 'yd³'
  | 'bbl'
  | 'gi'
  | 'hhd';

const METRIC: TbMeasureUnits<VolumeMetricUnits> = {
  ratio: 33.8140226,
  units: {
    'mm³': {
      name: 'unit.cubic-millimeter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000000,
    },
    'cm³': {
      name: 'unit.cubic-centimeter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000,
    },
    µL: {
      name: 'unit.microliter',
      tags: ['liquid measurement'],
      to_anchor: 0.000001,
    },
    mL: {
      name: 'unit.milliliter',
      tags: ['capacity', 'extent'],
      to_anchor: 1 / 1000,
    },
    L: {
      name: 'unit.liter',
      tags: ['capacity', 'extent'],
      to_anchor: 1,
    },
    hL: {
      name: 'unit.hectoliter',
      tags: ['capacity', 'extent'],
      to_anchor: 100,
    },
    'm³': {
      name: 'unit.cubic-meter',
      tags: ['capacity', 'extent'],
      to_anchor: 1000,
    },
    'km³': {
      name: 'unit.cubic-kilometer',
      tags: ['capacity', 'extent'],
      to_anchor: 1000000000000,
    },
  },
};

const IMPERIAL: TbMeasureUnits<VolumeImperialUnits> = {
  ratio: 1 / 33.8140226,
  units: {
    tsp: {
      name: 'unit.teaspoon',
      tags: ['cooking measurement'],
      to_anchor: 1 / 6,
    },
    tbsp: {
      name: 'unit.tablespoon',
      tags: ['cooking measurement'],
      to_anchor: 1 / 2,
    },
    'in³': {
      name: 'unit.cubic-inch',
      tags: ['capacity', 'extent'],
      to_anchor: 0.55411,
    },
    'fl-oz': {
      name: 'unit.fluid-ounce',
      tags: ['capacity', 'extent'],
      to_anchor: 1,
    },
    cup: {
      name: 'unit.cup',
      tags: ['cooking measurement'],
      to_anchor: 8,
    },
    pt: {
      name: 'unit.pint',
      tags: ['capacity', 'extent'],
      to_anchor: 16,
    },
    qt: {
      name: 'unit.quart',
      tags: ['capacity', 'extent'],
      to_anchor: 32,
    },
    gal: {
      name: 'unit.gallon',
      tags: ['capacity', 'extent'],
      to_anchor: 128,
    },
    'ft³': {
      name: 'unit.cubic-foot',
      tags: ['capacity', 'extent'],
      to_anchor: 957.506,
    },
    'yd³': {
      name: 'unit.cubic-yard',
      tags: ['capacity', 'extent'],
      to_anchor: 25852.7,
    },
    bbl: {
      name: 'unit.oil-barrels',
      tags: ['capacity', 'extent'],
      to_anchor: 5376,
    },
    gi: {
      name: 'unit.gill',
      tags: ['liquid measurement'],
      to_anchor: 4,
    },
    hhd: {
      name: 'unit.hogshead',
      tags: ['liquid measurement'],
      to_anchor: 8064,
    },
  },
};

const measure: TbMeasure<VolumeUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
