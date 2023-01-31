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

import { ValueSourceProperty } from '@home/components/widget/lib/settings/common/value-source.component';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, ValidationErrors,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';

export interface FixedColorLevel {
  from?: ValueSourceProperty;
  to?: ValueSourceProperty;
  color: string;
}

export function fixedColorLevelValidator(control: AbstractControl): ValidationErrors | null {
  const fixedColorLevel: FixedColorLevel = control.value;
  if (!fixedColorLevel || !fixedColorLevel.color) {
    return {
      fixedColorLevel: true
    };
  }
  return null;
}

@Component({
  selector: 'tb-fixed-color-level',
  templateUrl: './fixed-color-level.component.html',
  styleUrls: ['./fixed-color-level.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FixedColorLevelComponent),
      multi: true
    }
  ]
})
export class FixedColorLevelComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeFixedColorLevel = new EventEmitter();

  private modelValue: FixedColorLevel;

  private propagateChange = null;

  public fixedColorLevelFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.fixedColorLevelFormGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      color: [null, [Validators.required]]
    });
    this.fixedColorLevelFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.fixedColorLevelFormGroup.disable({emitEvent: false});
    } else {
      this.fixedColorLevelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FixedColorLevel): void {
    this.modelValue = value;
    this.fixedColorLevelFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  fixedColorLevelRangeText(): string {
    const value: FixedColorLevel = this.fixedColorLevelFormGroup.value;
    const from = this.valueSourcePropertyText(value?.from);
    const to = this.valueSourcePropertyText(value?.to);
    return `${from} - ${to}`;
  }

  private valueSourcePropertyText(source?: ValueSourceProperty): string {
    if (source) {
      if (source.valueSource === 'predefinedValue') {
        return `${isNumber(source.value) ? source.value : 0}`;
      } else if (source.valueSource === 'entityAttribute') {
        const alias = source.entityAlias || 'Undefined';
        const key = source.attribute || 'Undefined';
        return `${alias}.${key}`;
      }
    }
    return 'Undefined';
  }

  private updateModel() {
    const value: FixedColorLevel = this.fixedColorLevelFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
