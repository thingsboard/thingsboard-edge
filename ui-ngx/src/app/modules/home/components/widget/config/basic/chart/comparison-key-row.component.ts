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

import { ChangeDetectorRef, Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup
} from '@angular/forms';
import {
  DataKey,
  DataKeyComparisonSettings,
  DataKeySettingsWithComparison,
  DatasourceType
} from '@shared/models/widget.models';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-comparison-key-row',
  templateUrl: './comparison-key-row.component.html',
  styleUrls: ['./comparison-key-row.component.scss', '../../data-keys.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ComparisonKeyRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ComparisonKeyRowComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  @Input()
  datasourceType: DatasourceType;

  keyFormControl: UntypedFormControl;

  keyRowFormGroup: UntypedFormGroup;

  modelValue: DataKey;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keyFormControl = this.fb.control(null, []);
    this.keyRowFormGroup = this.fb.group({
      showValuesForComparison: [null, []],
      comparisonValuesLabel: [null, []],
      color: [null, []]
    });
    this.keyRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.keyRowFormGroup.get('showValuesForComparison').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateValidators());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keyFormControl.disable({emitEvent: false});
      this.keyRowFormGroup.disable({emitEvent: false});
    } else {
      this.keyFormControl.enable({emitEvent: false});
      this.keyRowFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: DataKey): void {
    this.modelValue = value;
    const comparisonSettings = (value?.settings as DataKeySettingsWithComparison)?.comparisonSettings;
    this.keyRowFormGroup.patchValue(
      comparisonSettings, {emitEvent: false}
    );
    this.keyFormControl.patchValue(deepClone(this.modelValue), {emitEvent: false});
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const showValuesForComparison: boolean = this.keyRowFormGroup.get('showValuesForComparison').value;
    if (showValuesForComparison) {
      this.keyFormControl.enable({emitEvent: false});
      this.keyRowFormGroup.get('comparisonValuesLabel').enable({emitEvent: false});
      this.keyRowFormGroup.get('color').enable({emitEvent: false});
    } else {
      this.keyFormControl.disable({emitEvent: false});
      this.keyRowFormGroup.get('comparisonValuesLabel').disable({emitEvent: false});
      this.keyRowFormGroup.get('color').disable({emitEvent: false});
    }
  }

  private updateModel() {
    const comparisonSettings: DataKeyComparisonSettings = this.keyRowFormGroup.value;
    if (!this.modelValue.settings) {
      this.modelValue.settings = {};
    }
    this.modelValue.settings.comparisonSettings = comparisonSettings;
    this.propagateChange(this.modelValue);
  }

}
