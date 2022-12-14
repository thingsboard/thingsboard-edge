///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { MqttQos, MqttQosTranslation, MqttTopicFilter } from '@shared/models/integration.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isNumber } from '@core/utils';

@Component({
  selector: 'tb-mqtt-topic-filters',
  templateUrl: './mqtt-topic-filters.component.html',
  styleUrls: ['./mqtt-topic-filters.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MqttTopicFiltersComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => MqttTopicFiltersComponent),
    multi: true,
  }]
})
export class MqttTopicFiltersComponent implements ControlValueAccessor, Validator, OnDestroy {

  mqttTopicFiltersForm: FormGroup;
  mqttQosTypes = Object.values(MqttQos).filter(v => isNumber(v));
  MqttQosTranslation = MqttQosTranslation;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.mqttTopicFiltersForm = this.fb.group({
      filters: this.fb.array([], Validators.required)
    });
    this.mqttTopicFiltersForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateModel(value.filters);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(value: MqttTopicFilter[]) {
    if (this.mqttFiltersFromArray.length === value?.length) {
      this.mqttTopicFiltersForm.get('filters').patchValue(value, {emitEvent: false});
    } else {
      const filtersControls: Array<AbstractControl> = [];
      if (value) {
        value.forEach((filter) => {
          filtersControls.push(this.fb.group({
            filter: [filter.filter, [Validators.required]],
            qos: [filter.qos, [Validators.required]]
          }));
        });
      }
      this.mqttTopicFiltersForm.setControl('filters', this.fb.array(filtersControls), {emitEvent: false});
      if (this.disabled) {
        this.mqttTopicFiltersForm.disable({emitEvent: false});
      } else {
        this.mqttTopicFiltersForm.enable({emitEvent: false});
      }
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mqttTopicFiltersForm.disable({emitEvent: false});
    } else {
      this.mqttTopicFiltersForm.enable({emitEvent: false});
    }
  }

  get mqttFiltersFromArray(): FormArray {
    return this.mqttTopicFiltersForm.get('filters') as FormArray;
  }

  addTopicFilter() {
    this.mqttFiltersFromArray.push(this.fb.group({
      filter: ['', [Validators.required]],
      qos: [0, [Validators.required]]
    }));
  }

  private updateModel(value: MqttTopicFilter[]) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.mqttTopicFiltersForm.valid ? null : {
      mqttTopicFilters: {valid: false}
    };
  }
}
