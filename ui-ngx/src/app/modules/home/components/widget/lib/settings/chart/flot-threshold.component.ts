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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';
import { TbFlotKeyThreshold } from '@home/components/widget/lib/flot-widget.models';

@Component({
  selector: 'tb-flot-threshold',
  templateUrl: './flot-threshold.component.html',
  styleUrls: ['./flot-threshold.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FlotThresholdComponent),
      multi: true
    }
  ]
})
export class FlotThresholdComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeThreshold = new EventEmitter();

  private modelValue: TbFlotKeyThreshold;

  private propagateChange = null;

  public thresholdFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.thresholdFormGroup = this.fb.group({
      valueSource: [null, []],
      lineWidth: [null, [Validators.min(0)]],
      color: [null, []]
    });
    this.thresholdFormGroup.valueChanges.subscribe(() => {
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
      this.thresholdFormGroup.disable({emitEvent: false});
    } else {
      this.thresholdFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TbFlotKeyThreshold): void {
    this.modelValue = value;
    const valueSource: ValueSourceProperty = {
      valueSource: value?.thresholdValueSource,
      entityAlias: value?.thresholdEntityAlias,
      attribute: value?.thresholdAttribute,
      value: value?.thresholdValue
    };
    this.thresholdFormGroup.patchValue(
      {valueSource, lineWidth: value?.lineWidth, color: value?.color}, {emitEvent: false}
    );
  }

  thresholdText(): string {
    const value: ValueSourceProperty = this.thresholdFormGroup.get('valueSource').value;
    return this.valueSourcePropertyText(value);
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
    const value: {valueSource: ValueSourceProperty, lineWidth: number, color: string} = this.thresholdFormGroup.value;
    this.modelValue = {
      thresholdValueSource: value?.valueSource?.valueSource,
      thresholdEntityAlias: value?.valueSource?.entityAlias,
      thresholdAttribute: value?.valueSource?.attribute,
      thresholdValue: value?.valueSource?.value,
      lineWidth: value?.lineWidth,
      color: value?.color
    };
    this.propagateChange(this.modelValue);
  }
}
