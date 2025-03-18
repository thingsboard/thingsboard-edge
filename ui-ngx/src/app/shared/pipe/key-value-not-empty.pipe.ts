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

import {
  inject,
  KeyValueChangeRecord,
  KeyValueChanges,
  KeyValueDiffer,
  KeyValueDiffers,
  Pipe,
  PipeTransform
} from '@angular/core';
import { KeyValue } from '@angular/common';
import { isDefinedAndNotNull } from '@core/utils';

@Pipe({
  name: 'keyValueIsNotEmpty',
  pure: false,
  standalone: true,
})
export class KeyValueIsNotEmptyPipe implements PipeTransform {
  private differs: KeyValueDiffers = inject(KeyValueDiffers);
  private differ!: KeyValueDiffer<string, unknown>;
  private keyValues: Array<KeyValue<string, unknown>> = [];

  // This is a custom implementation of angular keyvalue pipe
  // https://github.com/angular/angular/blob/main/packages/common/src/pipes/keyvalue_pipe.ts
  transform(
    input: Record<string, unknown>,
  ): Array<KeyValue<string, unknown>> {
    if (!input || (!(input instanceof Map) && typeof input !== 'object')) {
      return null;
    }

    this.differ ??= this.differs.find(input).create();

    const differChanges: KeyValueChanges<string, unknown> | null = this.differ.diff(input);

    if (differChanges) {
      this.keyValues = [];
      differChanges.forEachItem((r: KeyValueChangeRecord<string, unknown>) => {
        if (isDefinedAndNotNull(r.currentValue)) {
          this.keyValues.push(this.makeKeyValuePair(r.key, r.currentValue!));
        }
      });
    }

    return this.keyValues;
  }

  private makeKeyValuePair(key: string, value: unknown): KeyValue<string, unknown> {
    return {key, value};
  }
}
