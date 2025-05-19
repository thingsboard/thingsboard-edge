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

export type AmountOfSubstanceUnits = 'mol' | 'nmol' | 'μmol' | 'mmol' | 'kmol';

const METRIC: TbMeasureUnits<AmountOfSubstanceUnits> = {
  units: {
    mol: {
      name: 'unit.mole',
      tags: ['chemical amount'],
      to_anchor: 1,
    },
    nmol: {
      name: 'unit.nanomole',
      tags: ['chemical amount'],
      to_anchor: 0.000000001,
    },
    μmol: {
      name: 'unit.micromole',
      tags: ['chemical amount'],
      to_anchor: 0.000001,
    },
    mmol: {
      name: 'unit.millimole',
      tags: ['chemical amount'],
      to_anchor: 0.001,
    },
    kmol: {
      name: 'unit.kilomole',
      tags: ['chemical amount'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<AmountOfSubstanceUnits> = {
  METRIC,
};

export default measure;
