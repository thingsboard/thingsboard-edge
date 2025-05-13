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

export type KinematicViscosityMetricUnits = 'm²/s' | 'cm²/s' | 'St' | 'cSt';
export type KinematicViscosityImperialUnits = 'ft²/s' | 'in²/s';

export type KinematicViscosityUnits = KinematicViscosityMetricUnits | KinematicViscosityImperialUnits;

const METRIC: TbMeasureUnits<KinematicViscosityMetricUnits> = {
  ratio: 10.7639104167097,
  units: {
    'm²/s': {
      name: 'unit.square-meter-per-second',
      to_anchor: 1,
    },
    'cm²/s': {
      name: 'unit.square-centimeter-per-second',
      to_anchor: 1e-4,
    },
    St: {
      name: 'unit.stoke',
      to_anchor: 1e-4,
    },
    cSt: {
      name: 'unit.centistokes',
      to_anchor: 1e-6,
    },
  },
};

const IMPERIAL: TbMeasureUnits<KinematicViscosityImperialUnits> = {
  ratio: 0.09290304,
  units: {
    'ft²/s': {
      name: 'unit.square-foot-per-second',
      to_anchor: 0.09290304,
    },
    'in²/s': {
      name: 'unit.square-inch-per-second',
      to_anchor: 0.00064516,
    },
  },
};

const measure: TbMeasure<KinematicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
