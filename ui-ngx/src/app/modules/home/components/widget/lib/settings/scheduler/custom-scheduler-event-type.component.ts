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
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { CustomSchedulerEventType } from '@home/components/scheduler/scheduler-events.models';

export function customSchedulerEventTypeValidator(control: AbstractControl) {
    const schedulerEventType: CustomSchedulerEventType = control.value;
    if (!schedulerEventType
      || !schedulerEventType.name
      || !schedulerEventType.value
      || !schedulerEventType.template
    ) {
      return {
        customSchedulerEventType: true
      };
    }
    return null;
}

@Component({
  selector: 'tb-custom-scheduler-event-type',
  templateUrl: './custom-scheduler-event-type.component.html',
  styleUrls: ['./custom-scheduler-event-type.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomSchedulerEventTypeComponent),
      multi: true
    }
  ]
})
export class CustomSchedulerEventTypeComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeCustomSchedulerEventType = new EventEmitter();

  private modelValue: CustomSchedulerEventType;

  private propagateChange = null;

  public customSchedulerEventTypeFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.customSchedulerEventTypeFormGroup = this.fb.group({
      name: [null, [Validators.required]],
      value: [null, [Validators.required]],
      originator: [null, []],
      msgType: [null, []],
      metadata: [null, []],
      template: [null, [Validators.required]]
    });
    this.customSchedulerEventTypeFormGroup.valueChanges.subscribe(() => {
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
      this.customSchedulerEventTypeFormGroup.disable({emitEvent: false});
    } else {
      this.customSchedulerEventTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CustomSchedulerEventType): void {
    this.modelValue = value;
    this.customSchedulerEventTypeFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  customSchedulerEventTypeHtml(): SafeHtml {
    const value: CustomSchedulerEventType = this.customSchedulerEventTypeFormGroup.value;
    const name = value.name || 'Undefined';
    const typeName = value.value || 'Undefined';
    return this.domSanitizer.bypassSecurityTrustHtml(`${name} (<small>${typeName}</small>)`);
  }

  private updateModel() {
    const value: CustomSchedulerEventType = this.customSchedulerEventTypeFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
