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

export type MassUnits = MassMetricUnits | MassImperialUnits;

export type MassMetricUnits = 'ng' | 'mcg' | 'mg' | 'g' | 'kg' | 't' | 'Da' | 'ct';
export type MassImperialUnits = 'oz' | 'lb' | 'st' | 'short tons' | 'gr' | 'dr' | 'qr' | 'cwt' | 'slug';

const METRIC: TbMeasureUnits<MassMetricUnits> = {
  ratio: 1 / 453.59237,
  units: {
    ng: {
      name: 'unit.nanogram',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1e-9,
    },
    mcg: {
      name: 'unit.microgram',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1e-6,
    },
    mg: {
      name: 'unit.milligram',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1e-3,
    },
    g: {
      name: 'unit.gram',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1,
    },
    kg: {
      name: 'unit.kilogram',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1000,
    },
    t: {
      name: 'unit.tonne',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1000000,
    },
    Da: {
      name: 'unit.dalton',
      tags: ['atomic mass unit', 'AMU', 'unified atomic mass unit'],
      to_anchor: 1.66053906660e-24,
    },
    ct: {
      name: 'unit.carat',
      tags: ['gemstone', 'pearl', 'jewelry', 'carat', 'ct'],
      to_anchor: 0.2,
    },
  },
};

const IMPERIAL: TbMeasureUnits<MassImperialUnits> = {
  ratio: 453.59237,
  units: {
    oz: {
      name: 'unit.ounce',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1 / 16,
    },
    lb: {
      name: 'unit.pound',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 1,
    },
    st: {
      name: 'unit.stone',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 14,
    },
    'short tons': {
      name: 'unit.short-tons',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 2000,
    },
    gr: {
      name: 'unit.grain',
      tags: ['measurement'],
      to_anchor: 1 / 7000,
    },
    dr: {
      name: 'unit.drachm',
      tags: ['measurement'],
      to_anchor: 1 / 256,
    },
    qr: {
      name: 'unit.quarter',
      tags: ['measurement'],
      to_anchor: 28,
    },
    cwt: {
      name: 'unit.hundredweight-count',
      tags: ['weight', 'heaviness', 'load'],
      to_anchor: 100,
    },
    slug: {
      name: 'unit.slug',
      tags: ['measurement'],
      to_anchor: 32.174,
    },
  },
};

const measure: TbMeasure<MassUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
