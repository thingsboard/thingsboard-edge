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

export type MagneticFluxDensityUnits = 'T' | 'mT' | 'μT' | 'nT' | 'kT' | 'MT' | 'G' | 'kG' | 'γ' | 'A/m' | 'Oe';

const METRIC: TbMeasureUnits<MagneticFluxDensityUnits> = {
  units: {
    T: {
      name: 'unit.tesla',
      tags: ['magnetic field strength'],
      to_anchor: 1,
    },
    mT: {
      name: 'unit.millitesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.001,
    },
    μT: {
      name: 'unit.microtesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.000001,
    },
    nT: {
      name: 'unit.nanotesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.000000001,
    },
    kT: {
      name: 'unit.kilotesla',
      tags: ['magnetic field strength'],
      to_anchor: 1000,
    },
    MT: {
      name: 'unit.megatesla',
      tags: ['magnetic field strength'],
      to_anchor: 1000000,
    },
    G: {
      name: 'unit.gauss',
      tags: ['magnetic field strength'],
      to_anchor: 0.0001,
    },
    kG: {
      name: 'unit.kilogauss',
      tags: ['magnetic field strength'],
      to_anchor: 0.1,
    },
    γ: {
      name: 'unit.gamma',
      to_anchor: 0.000000001,
    },
    'A/m': {
      name: 'unit.ampere-per-meter',
      tags: ['magnetic field strength', 'magnetic field intensity'],
      to_anchor: 0.00000125663706143591,
    },
    Oe: {
      name: 'unit.oersted',
      tags: ['magnetic field strength'],
      to_anchor: 0.0001,
    },
  },
};

const measure: TbMeasure<MagneticFluxDensityUnits> = {
  METRIC,
};

export default measure;
