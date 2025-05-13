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

export type RadiationDoseUnits = 'Gy' | 'Sv' | 'Rad' | 'Rem' | 'R' | 'C/kg' | 'cps';

const METRIC: TbMeasureUnits<RadiationDoseUnits> = {
  units: {
    Sv: {
      name: 'unit.sievert',
      tags: ['sievert', 'radiation dose equivalent', 'Sv'],
      to_anchor: 1,
    },
    Gy: {
      name: 'unit.gray',
      tags: ['absorbed dose', 'gray', 'Gy'],
      to_anchor: 1,
    },
    Rad: {
      name: 'unit.rad',
      tags: ['rad'],
      to_anchor: 0.01,
    },
    Rem: {
      name: 'unit.rem',
      tags: ['radiation dose equivalent'],
      to_anchor: 0.01,
    },
    R: {
      name: 'unit.roentgen',
      tags: ['radiation exposure'],
      to_anchor: 0.0093,
    },
    'C/kg': {
      name: 'unit.coulombs-per-kilogram',
      tags: ['radiation exposure', 'electric charge-to-mass ratio'],
      to_anchor: 34,
    },
    cps: {
      name: 'unit.cps',
      tags: ['radiation detection'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiationDoseUnits> = {
  METRIC,
};

export default measure;
