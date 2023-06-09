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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR, ValidationErrors,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

export interface LabelDataKey {
  name: string;
  type: DataKeyType;
}

export function labelDataKeyValidator(control: AbstractControl): ValidationErrors | null {
  const labelDataKey: LabelDataKey = control.value;
  if (!labelDataKey || !labelDataKey.name) {
    return {
      labelDataKey: true
    };
  }
  return null;
}

@Component({
  selector: 'tb-label-data-key',
  templateUrl: './label-data-key.component.html',
  styleUrls: ['./label-data-key.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LabelDataKeyComponent),
      multi: true
    }
  ]
})
export class LabelDataKeyComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeLabelDataKey = new EventEmitter();

  private modelValue: LabelDataKey;

  private propagateChange = null;

  public labelDataKeyFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.labelDataKeyFormGroup = this.fb.group({
      name: [null, [Validators.required]],
      type: [DataKeyType.attribute, [Validators.required]]
    });
    this.labelDataKeyFormGroup.valueChanges.subscribe(() => {
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
      this.labelDataKeyFormGroup.disable({emitEvent: false});
    } else {
      this.labelDataKeyFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: LabelDataKey): void {
    this.modelValue = value;
    this.labelDataKeyFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  labelDataKeyText(): string {
    const name: string = this.labelDataKeyFormGroup.get('name').value || '';
    const type: DataKeyType = this.labelDataKeyFormGroup.get('type').value;
    let typeStr: string;
    if (type === DataKeyType.attribute) {
      typeStr = this.translate.instant('widgets.chart.key-type-attribute');
    } else if (type === DataKeyType.timeseries) {
      typeStr = this.translate.instant('widgets.chart.key-type-timeseries');
    }
    return `${name} (${typeStr})`;
  }

  private updateModel() {
    const value: LabelDataKey = this.labelDataKeyFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
