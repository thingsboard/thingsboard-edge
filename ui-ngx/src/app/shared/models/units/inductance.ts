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

export type InductanceMetricUnits = 'H' | 'mH' | 'µH' | 'nH' | 'T·m/A';
export type InductanceUnits = InductanceMetricUnits;

const METRIC: TbMeasureUnits<InductanceMetricUnits> = {
  units: {
    H: {
      name: 'unit.henry',
      tags: ['inductance', 'magnetic induction', 'H'],
      to_anchor: 1,
    },
    mH: {
      name: 'unit.millihenry',
      tags: ['inductance', 'millihenry', 'mH'],
      to_anchor: 0.001,
    },
    µH: {
      name: 'unit.microhenry',
      tags: ['inductance', 'microhenry', 'µH'],
      to_anchor: 1e-6,
    },
    nH: {
      name: 'unit.nanohenry',
      tags: ['inductance', 'nanohenry', 'nH'],
      to_anchor: 1e-9,
    },
    'T·m/A': {
      name: 'unit.tesla-meter-per-ampere',
      tags: ['magnetic field', 'Tesla Meter per Ampere', 'T·m/A', 'magnetic flux'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<InductanceUnits> = {
  METRIC,
};

export default measure;
