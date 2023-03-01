///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { Pipe, PipeTransform } from '@angular/core';

type unit = 'bytes' | 'KB' | 'MB' | 'GB' | 'TB' | 'PB';
type unitPrecisionMap = {
  [u in unit]: number;
};

const defaultPrecisionMap: unitPrecisionMap = {
  bytes: 0,
  KB: 1,
  MB: 1,
  GB: 1,
  TB: 2,
  PB: 2
};

@Pipe({ name: 'fileSize' })
export class FileSizePipe implements PipeTransform {
  private readonly units: unit[] = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

  transform(bytes: number = 0, precision: number | unitPrecisionMap = defaultPrecisionMap): string {
    if (isNaN(parseFloat(String(bytes))) || !isFinite(bytes)) {
      return '?';
    }

    let unitIndex = 0;

    while (bytes >= 1024) {
      bytes /= 1024;
      unitIndex++;
    }

    const unitSymbol = this.units[unitIndex];

    if (typeof precision === 'number') {
      return `${bytes.toFixed(+precision)} ${unitSymbol}`;
    }
    return `${bytes.toFixed(precision[unitSymbol])} ${unitSymbol}`;
  }
}
