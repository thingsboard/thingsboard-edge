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

export type CapacitanceUnits = 'F' | 'mF' | 'μF' | 'nF' | 'pF' | 'kF' | 'MF' | 'GF' | 'TF';

const METRIC: TbMeasureUnits<CapacitanceUnits> = {
  units: {
    F: {
      name: 'unit.farad',
      tags: ['electric capacitance'],
      to_anchor: 1,
    },
    mF: {
      name: 'unit.millifarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-3,
    },
    μF: {
      name: 'unit.microfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-6,
    },
    nF: {
      name: 'unit.nanofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-9,
    },
    pF: {
      name: 'unit.picofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-12,
    },
    kF: {
      name: 'unit.kilofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e3,
    },
    MF: {
      name: 'unit.megafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e6,
    },
    GF: {
      name: 'unit.gigafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e9,
    },
    TF: {
      name: 'unit.terfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e12,
    },
  },
};

const measure: TbMeasure<CapacitanceUnits> = {
  METRIC,
};

export default measure;
