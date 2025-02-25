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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  WidgetButtonAppearance,
  widgetButtonStates, widgetButtonStatesTranslations,
  widgetButtonTypeImages,
  widgetButtonTypes,
  widgetButtonTypeTranslations
} from '@shared/components/button/widget-button.models';
import { merge } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-widget-button-appearance',
  templateUrl: './widget-button-appearance.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetButtonAppearanceComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class WidgetButtonAppearanceComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled = false;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  @coerceBoolean()
  withAutoScale = true;

  @Input()
  @coerceBoolean()
  withBorderRadius = false;

  widgetButtonTypes = widgetButtonTypes;

  widgetButtonTypeTranslationMap = widgetButtonTypeTranslations;
  widgetButtonTypeImageMap = widgetButtonTypeImages;

  widgetButtonStates = widgetButtonStates;
  widgetButtonStateTranslationMap = widgetButtonStatesTranslations;

  modelValue: WidgetButtonAppearance;

  appearanceFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.appearanceFormGroup = this.fb.group({
      type: [null, []],
      showLabel: [null, []],
      label: [null, []],
      showIcon: [null, []],
      icon: [null, []],
      iconSize: [null, []],
      iconSizeUnit: [null, []],
      mainColor: [null, []],
      backgroundColor: [null, []]
    });
    if (this.withAutoScale) {
      this.appearanceFormGroup.addControl('autoScale', this.fb.control(null, []));
    }
    if (this.withBorderRadius) {
      this.appearanceFormGroup.addControl('borderRadius', this.fb.control(null, []));
    }
    const customStyle = this.fb.group({});
    for (const state of widgetButtonStates) {
      customStyle.addControl(state, this.fb.control(null, []));
    }
    this.appearanceFormGroup.addControl('customStyle', customStyle);
    this.appearanceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.appearanceFormGroup.get('showLabel').valueChanges,
          this.appearanceFormGroup.get('showIcon').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.appearanceFormGroup.disable({emitEvent: false});
    } else {
      this.appearanceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: WidgetButtonAppearance): void {
    this.modelValue = value;
    this.appearanceFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateModel() {
    this.modelValue = this.appearanceFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private updateValidators(): void {
    const showLabel: boolean = this.appearanceFormGroup.get('showLabel').value;
    const showIcon: boolean = this.appearanceFormGroup.get('showIcon').value;
    if (showLabel) {
      this.appearanceFormGroup.get('label').enable();
    } else {
      this.appearanceFormGroup.get('label').disable();
    }
    if (showIcon) {
      this.appearanceFormGroup.get('icon').enable();
      this.appearanceFormGroup.get('iconSize').enable();
      this.appearanceFormGroup.get('iconSizeUnit').enable();
    } else {
      this.appearanceFormGroup.get('icon').disable();
      this.appearanceFormGroup.get('iconSize').disable();
      this.appearanceFormGroup.get('iconSizeUnit').disable();
    }
  }
}
