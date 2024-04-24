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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { WidgetService } from '@core/http/widget.service';
import {
  echartsAnimationEasings,
  EChartsAnimationSettings
} from '@home/components/widget/lib/chart/echarts-widget.models';

@Component({
  selector: 'tb-echarts-animation-settings',
  templateUrl: './echarts-animation-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EchartsAnimationSettingsComponent),
      multi: true
    }
  ]
})
export class EchartsAnimationSettingsComponent implements OnInit, ControlValueAccessor {

  settingsExpanded = false;

  echartsAnimationEasings = echartsAnimationEasings;

  @Input()
  disabled: boolean;

  private modelValue: EChartsAnimationSettings;

  private propagateChange = null;

  public animationSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,) {
  }

  ngOnInit(): void {
    this.animationSettingsFormGroup = this.fb.group({
      animation: [null, []],
      animationThreshold: [null, [Validators.min(0)]],
      animationDuration: [null, [Validators.min(0)]],
      animationEasing: [null, []],
      animationDelay: [null, [Validators.min(0)]],
      animationDurationUpdate: [null, [Validators.min(0)]],
      animationEasingUpdate: [null, []],
      animationDelayUpdate: [null, [Validators.min(0)]]
    });
    this.animationSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.animationSettingsFormGroup.get('animation').valueChanges
    .subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.animationSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.animationSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: EChartsAnimationSettings): void {
    this.modelValue = value;
    this.animationSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.animationSettingsFormGroup.get('animation').valueChanges.subscribe((animation) => {
      this.settingsExpanded = animation;
    });
  }

  private updateValidators() {
    const animation: boolean = this.animationSettingsFormGroup.get('animation').value;
    if (animation) {
      this.animationSettingsFormGroup.enable({emitEvent: false});
    } else {
      this.animationSettingsFormGroup.disable({emitEvent: false});
      this.animationSettingsFormGroup.get('animation').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.animationSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
