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

export type AreaMetricUnits = 'mm²' | 'cm²' | 'm²' | 'a' | 'ha' | 'km²' | 'barn';
export type AreaImperialUnits = 'in²' | 'yd²' | 'ft²' | 'ac' | 'ml²' | 'cin';

export type AreaUnits = AreaMetricUnits | AreaImperialUnits;

const METRIC: TbMeasureUnits<AreaMetricUnits> = {
  ratio: 10.7639,
  units: {
    'mm²': {
      name: 'unit.square-millimeter',
      tags: ['lot', 'zone', 'space', 'region', 'square millimeters', 'sq-mm'],
      to_anchor: 1 / 1000000,
    },
    'cm²': {
      name: 'unit.square-centimeter',
      tags: ['lot', 'zone', 'space', 'region', 'square centimeters', 'sq-cm'],
      to_anchor: 1 / 10000,
    },
    'm²': {
      name: 'unit.square-meter',
      tags: ['lot', 'zone', 'space', 'region', 'square meters', 'sq-m'],
      to_anchor: 1,
    },
    a: {
      name: 'unit.are',
      tags: ['land measurement'],
      to_anchor: 100,
    },
    ha: {
      name: 'unit.hectare',
      tags: ['lot', 'zone', 'space', 'region', 'hectares'],
      to_anchor: 10000,
    },
    'km²': {
      name: 'unit.square-kilometer',
      tags: ['lot', 'zone', 'space', 'region', 'square kilometers', 'sq-km'],
      to_anchor: 1000000,
    },
    barn: {
      name: 'unit.barn',
      tags: ['cross-sectional area', 'particle physics', 'nuclear physics'],
      to_anchor: 1e-28,
    },
  }
};

const IMPERIAL: TbMeasureUnits<AreaImperialUnits> = {
  ratio: 1 / 10.7639,
  units: {
    'in²': {
      name: 'unit.square-inch',
      tags: ['lot', 'zone', 'space', 'region', 'square inches', 'sq-in'],
      to_anchor: 1 / 144,
    },
    'yd²': {
      name: 'unit.square-yard',
      tags: ['lot', 'zone', 'space', 'region', 'square yards', 'sq-yd'],
      to_anchor: 9,
    },
    'ft²': {
      name: 'unit.square-foot',
      tags: ['lot', 'zone', 'space', 'region', 'square feet', 'sq-ft'],
      to_anchor: 1,
    },
    ac: {
      name: 'unit.acre',
      tags: ['lot', 'zone', 'space', 'region', 'acres', 'a'],
      to_anchor: 43560,
    },
    'ml²': {
      name: 'unit.square-mile',
      tags: ['lot', 'zone', 'space', 'region', 'square mile', 'sq-mi'],
      to_anchor: 27878400,
    },
    cin: {
      name: 'unit.circular-inch',
      tags: ['circular measurement', 'circin'],
      to_anchor: Math.PI / 576
    }
  }
}

const measure: TbMeasure<AreaUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
