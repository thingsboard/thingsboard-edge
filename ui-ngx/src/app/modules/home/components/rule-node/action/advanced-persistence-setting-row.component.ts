///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  AdvancedPersistenceConfig,
  defaultAdvancedPersistenceConfig,
  maxDeduplicateTimeSecs,
  PersistenceType,
  PersistenceTypeTranslationMap
} from '@home/components/rule-node/action/timeseries-config.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-advanced-persistence-setting-row',
  templateUrl: './advanced-persistence-setting-row.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AdvancedPersistenceSettingRowComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AdvancedPersistenceSettingRowComponent),
    multi: true
  }]
})
export class AdvancedPersistenceSettingRowComponent implements ControlValueAccessor, Validator {

  @Input()
  title: string;

  persistenceSettingRowForm = this.fb.group({
    type: [defaultAdvancedPersistenceConfig.type],
    deduplicationIntervalSecs: [{value: 60, disabled: true}]
  });

  PersistenceType = PersistenceType;
  persistenceStrategies = [PersistenceType.ON_EVERY_MESSAGE, PersistenceType.DEDUPLICATE, PersistenceType.SKIP];
  PersistenceTypeTranslationMap = PersistenceTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder) {
    this.persistenceSettingRowForm.get('type').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updatedValidation());

    this.persistenceSettingRowForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => this.propagateChange(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.persistenceSettingRowForm.disable({emitEvent: false});
    } else {
      this.persistenceSettingRowForm.enable({emitEvent: false});
      this.updatedValidation();
    }
  }

  validate(): ValidationErrors | null {
    return this.persistenceSettingRowForm.valid ? null : {
      persistenceSettingRow: false
    };
  }

  writeValue(value: AdvancedPersistenceConfig) {
    if (isDefinedAndNotNull(value)) {
      this.persistenceSettingRowForm.patchValue(value, {emitEvent: false});
    } else {
      this.persistenceSettingRowForm.patchValue(defaultAdvancedPersistenceConfig);
    }
  }

  private updatedValidation() {
    if (this.persistenceSettingRowForm.get('type').value === PersistenceType.DEDUPLICATE) {
      this.persistenceSettingRowForm.get('deduplicationIntervalSecs').enable({emitEvent: false});
    } else {
      this.persistenceSettingRowForm.get('deduplicationIntervalSecs').disable({emitEvent: false})
    }
  }
}
