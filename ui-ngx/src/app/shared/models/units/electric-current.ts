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

export type ElectricCurrentUnits = 'A' | 'pA' | 'nA' | 'μA' | 'mA' | 'kA' | 'MA' | 'GA';

const METRIC: TbMeasureUnits<ElectricCurrentUnits> = {
  units: {
    A: {
      name: 'unit.ampere',
      tags: ['current flow', 'flow of electricity', 'electrical flow', 'amperes', 'amperage'],
      to_anchor: 1,
    },
    pA: {
      name: 'unit.picoampere',
      tags: ['picoamperes'],
      to_anchor: 1e-12,
    },
    nA: {
      name: 'unit.nanoampere',
      tags: ['nanoamperes'],
      to_anchor: 1e-9,
    },
    μA: {
      name: 'unit.microampere',
      tags: ['microamperes'],
      to_anchor: 1e-6,
    },
    mA: {
      name: 'unit.milliampere',
      tags: ['milliamperes'],
      to_anchor: 0.001,
    },
    kA: {
      name: 'unit.kiloampere',
      tags: ['kiloamperes'],
      to_anchor: 1000,
    },
    MA: {
      name: 'unit.megaampere',
      tags: ['megaamperes'],
      to_anchor: 1e6,
    },
    GA: {
      name: 'unit.gigaampere',
      tags: ['gigaamperes'],
      to_anchor: 1e9,
    },
  }
};

const measure: TbMeasure<ElectricCurrentUnits> = {
  METRIC,
};

export default measure;
