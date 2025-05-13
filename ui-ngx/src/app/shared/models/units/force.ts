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

export type ForceUnits = ForceMetricUnits | ForceImperialUnits;

export type ForceMetricUnits = 'N' | 'kN' | 'dyn';
export type ForceImperialUnits = 'lbf' | 'kgf' | 'klbf' | 'pdl' | 'kip';

const METRIC: TbMeasureUnits<ForceMetricUnits> = {
  ratio: 0.224809,
  units: {
    N: {
      name: 'unit.newton',
      tags: ['pressure', 'push', 'pull', 'weight'],
      to_anchor: 1,
    },
    kN: {
      name: 'unit.kilonewton',
      to_anchor: 1000,
    },
    dyn: {
      name: 'unit.dyne',
      to_anchor: 0.00001,
    },
  },
};

const IMPERIAL: TbMeasureUnits<ForceImperialUnits> = {
  ratio: 4.44822,
  units: {
    lbf: {
      name: 'unit.pound-force',
      to_anchor: 1,
    },
    kgf: {
      name: 'unit.kilogram-force',
      to_anchor: 2.20462,
    },
    klbf: {
      name: 'unit.kilopound-force',
      to_anchor: 1000,
    },
    pdl: {
      name: 'unit.poundal',
      to_anchor: 0.031081,
    },
    kip: {
      name: 'unit.kip',
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<ForceUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
