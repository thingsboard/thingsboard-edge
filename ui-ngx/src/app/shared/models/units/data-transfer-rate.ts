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

export type DataTransferRateUnits = 'bps' | 'kbps' | 'Mbps' | 'Gbps' | 'Tbps' | 'B/s' | 'KB/s' | 'MB/s' | 'GB/s';

const METRIC: TbMeasureUnits<DataTransferRateUnits> = {
  units: {
    bps: {
      name: 'unit.bit-per-second',
      to_anchor: 1,
    },
    kbps: {
      name: 'unit.kilobit-per-second',
      to_anchor: 1e3,
    },
    Mbps: {
      name: 'unit.megabit-per-second',
      to_anchor: 1e6,
    },
    Gbps: {
      name: 'unit.gigabit-per-second',
      to_anchor: 1e9,
    },
    Tbps: {
      name: 'unit.terabit-per-second',
      to_anchor: 1e12,
    },
    'B/s': {
      name: 'unit.byte-per-second',
      to_anchor: 8,
    },
    'KB/s': {
      name: 'unit.kilobyte-per-second',
      to_anchor: 8e3,
    },
    'MB/s': {
      name: 'unit.megabyte-per-second',
      to_anchor: 8e6,
    },
    'GB/s': {
      name: 'unit.gigabyte-per-second',
      to_anchor: 8e9,
    },
  },
};

const measure: TbMeasure<DataTransferRateUnits> = {
  METRIC,
};

export default measure;
