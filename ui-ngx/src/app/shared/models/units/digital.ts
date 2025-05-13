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

export type DigitalUnits = 'bit' | 'B' | 'KB' | 'MB' | 'GB' | 'TB' | 'PB' | 'EB' | 'ZB' | 'YB';

const METRIC: TbMeasureUnits<DigitalUnits> = {
  units: {
    bit: {
      name: 'unit.bit',
      tags: ['data', 'binary digit', 'information'],
      to_anchor: 1.25e-1,
    },
    B: {
      name: 'unit.byte',
      tags: ['data', 'information', 'storage', 'memory'],
      to_anchor: 1
    },
    KB: {
      name: 'unit.kilobyte',
      tags: ['data'],
      to_anchor: 1024,
    },
    MB: {
      name: 'unit.megabyte',
      tags: ['data'],
      to_anchor: 1024 ** 2,
    },
    GB: {
      name: 'unit.gigabyte',
      tags: ['data'],
      to_anchor: 1024 ** 3,
    },
    TB: {
      name: 'unit.terabyte',
      tags: ['data'],
      to_anchor: 1024 ** 4,
    },
    PB: {
      name: 'unit.petabyte',
      tags: ['data'],
      to_anchor: 1024 ** 5,
    },
    EB: {
      name: 'unit.exabyte',
      tags: ['data'],
      to_anchor: 1024 ** 6,
    },
    ZB: {
      name: 'unit.zettabyte',
      tags: ['data'],
      to_anchor: 1024 ** 7,
    },
    YB: {
      name: 'unit.yottabyte',
      tags: ['data'],
      to_anchor: 1024 ** 8,
    },
  }
};

const measure: TbMeasure<DigitalUnits> = {
  METRIC,
};

export default measure;
