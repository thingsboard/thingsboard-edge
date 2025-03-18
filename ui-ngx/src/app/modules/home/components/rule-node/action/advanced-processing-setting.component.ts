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
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdvancedProcessingStrategy } from '@home/components/rule-node/action/timeseries-config.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { AttributeAdvancedProcessingStrategy } from '@home/components/rule-node/action/attributes-config.model';

@Component({
  selector: 'tb-advanced-processing-settings',
  templateUrl: './advanced-processing-setting.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AdvancedProcessingSettingComponent),
    multi: true
  },{
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AdvancedProcessingSettingComponent),
    multi: true
  }]
})
export class AdvancedProcessingSettingComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  @coerceBoolean()
  timeseries = false;

  @Input()
  @coerceBoolean()
  attributes = false;

  @Input()
  @coerceBoolean()
  latest = false;

  @Input()
  @coerceBoolean()
  webSockets = false;

  @Input()
  @coerceBoolean()
  calculatedFields = false;

  processingForm: UntypedFormGroup;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.processingForm = this.fb.group({});
    if (this.timeseries) {
      this.processingForm.addControl('timeseries', this.fb.control(null, []));
    }
    if (this.attributes) {
      this.processingForm.addControl('attributes', this.fb.control(null, []));
    }
    if (this.latest) {
      this.processingForm.addControl('latest', this.fb.control(null, []));
    }
    if (this.webSockets) {
      this.processingForm.addControl('webSockets', this.fb.control(null, []));
    }
    if (this.calculatedFields) {
      this.processingForm.addControl('calculatedFields', this.fb.control(null, []));
    }
    this.processingForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.propagateChange(value));
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.processingForm.disable({emitEvent: false});
    } else {
      this.processingForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.processingForm.valid ? null : {
      processingForm: false
    };
  }

  writeValue(value: AdvancedProcessingStrategy | AttributeAdvancedProcessingStrategy) {
    this.processingForm.patchValue(value, {emitEvent: false});
  }
}
