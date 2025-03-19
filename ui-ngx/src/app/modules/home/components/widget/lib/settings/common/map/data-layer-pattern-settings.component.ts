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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { merge } from 'rxjs';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  DataLayerPatternSettings,
  DataLayerPatternType,
  DataLayerTooltipSettings, dataLayerTooltipTriggers, dataLayerTooltipTriggerTranslationMap
} from '@shared/models/widget/maps/map.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';

@Component({
  selector: 'tb-data-layer-pattern-settings',
  templateUrl: './data-layer-pattern-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataLayerPatternSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DataLayerPatternSettingsComponent),
      multi: true
    }
  ]
})
export class DataLayerPatternSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  DataLayerPatternType = DataLayerPatternType;

  dataLayerTooltipTriggers = dataLayerTooltipTriggers;

  dataLayerTooltipTriggerTranslationMap = dataLayerTooltipTriggerTranslationMap;

  settingsExpanded = false;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  @Input()
  disabled: boolean;

  @Input()
  patternType: 'label' | 'tooltip' = 'label';

  @Input()
  helpId = 'widget/lib/map/label_fn';

  @Input()
  patternTitle: string;

  @Input()
  @coerceBoolean()
  hasTooltipOffset = false;

  @Input()
  @coerceBoolean()
  expand = true;

  @Input()
  context: MapSettingsContext;

  private modelValue: DataLayerPatternSettings | DataLayerTooltipSettings;

  private propagateChange = null;

  public patternSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.patternSettingsFormGroup = this.fb.group({
      show: [null, []],
      type: [null, []],
      pattern: [null, [Validators.required]],
      patternFunction: [null, [Validators.required]]
    });
    if (this.patternType === 'tooltip') {
      this.patternSettingsFormGroup.addControl('trigger', this.fb.control(null, []));
      this.patternSettingsFormGroup.addControl('autoclose', this.fb.control(null, []));
      if (this.hasTooltipOffset) {
        this.patternSettingsFormGroup.addControl('offsetX', this.fb.control(null, []));
        this.patternSettingsFormGroup.addControl('offsetY', this.fb.control(null, []));
      }
      this.patternSettingsFormGroup.addControl('tagActions', this.fb.control(null, []));
    }
    this.patternSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.patternSettingsFormGroup.get('show').valueChanges,
      this.patternSettingsFormGroup.get('type').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
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
      this.patternSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.patternSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: DataLayerPatternSettings | DataLayerTooltipSettings): void {
    this.modelValue = value;
    this.patternSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.settingsExpanded = this.patternSettingsFormGroup.get('show').value && this.expand;
    this.patternSettingsFormGroup.get('show').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  public validate(c: UntypedFormControl) {
    const valid = this.patternSettingsFormGroup.valid;
    return valid ? null : {
      [this.patternType]: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const show: boolean = this.patternSettingsFormGroup.get('show').value;
    const type: DataLayerPatternType = this.patternSettingsFormGroup.get('type').value;
    if (show) {
      this.patternSettingsFormGroup.enable({emitEvent: false});
      if (type === DataLayerPatternType.pattern) {
        this.patternSettingsFormGroup.get('pattern').enable({emitEvent: false});
        this.patternSettingsFormGroup.get('patternFunction').disable({emitEvent: false});
      } else {
        this.patternSettingsFormGroup.get('pattern').disable({emitEvent: false});
        this.patternSettingsFormGroup.get('patternFunction').enable({emitEvent: false});
      }
    } else {
      this.patternSettingsFormGroup.disable({emitEvent: false});
      this.patternSettingsFormGroup.get('show').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.patternSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
