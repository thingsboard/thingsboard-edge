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

import { AfterViewInit, Directive, EventEmitter, inject, Input, OnDestroy, Output, TemplateRef } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, ValidationErrors, Validator } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Directive()
export abstract class GatewayConnectorBasicConfigDirective<InputBasicConfig, OutputBasicConfig>
  implements AfterViewInit, ControlValueAccessor, Validator, OnDestroy {

  @Input() generalTabContent: TemplateRef<any>;
  @Output() initialized = new EventEmitter<void>();

  basicFormGroup: FormGroup;

  protected fb = inject(FormBuilder);
  protected onChange!: (value: OutputBasicConfig) => void;
  protected onTouched!: () => void;
  protected destroy$ = new Subject<void>();

  constructor() {
    this.basicFormGroup = this.initBasicFormGroup();

    this.basicFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => this.onBasicFormGroupChange(value));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.initialized.emit();
  }

  validate(): ValidationErrors | null {
    return this.basicFormGroup.valid ? null : { basicFormGroup: { valid: false } };
  }

  registerOnChange(fn: (value: OutputBasicConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(config: OutputBasicConfig): void {
    this.basicFormGroup.setValue(this.mapConfigToFormValue(config), { emitEvent: false });
  }

  protected onBasicFormGroupChange(value: InputBasicConfig): void {
    this.onChange(this.getMappedValue(value));
    this.onTouched();
  }

  protected abstract mapConfigToFormValue(config: OutputBasicConfig): InputBasicConfig;
  protected abstract getMappedValue(config: InputBasicConfig): OutputBasicConfig;
  protected abstract initBasicFormGroup(): FormGroup;
}
