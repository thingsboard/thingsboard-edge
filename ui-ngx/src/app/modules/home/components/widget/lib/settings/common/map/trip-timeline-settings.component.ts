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
  TripTimelineSettings
} from '@shared/models/widget/maps/map.models';

@Component({
  selector: 'tb-trip-timeline-settings',
  templateUrl: './trip-timeline-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripTimelineSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripTimelineSettingsComponent),
      multi: true
    }
  ]
})
export class TripTimelineSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  settingsExpanded = false;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  @Input()
  disabled: boolean;

  private modelValue: TripTimelineSettings;

  private propagateChange = null;

  public tripTimelineSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {

    this.tripTimelineSettingsFormGroup = this.fb.group({
      showTimelineControl: [null],
      timeStep: [null, [Validators.required, Validators.min(1)]],
      speedOptions: [null, [Validators.required]],
      showTimestamp: [null],
      timestampFormat: [null, [Validators.required]],
      snapToRealLocation: [null],
      locationSnapFilter: [null, [Validators.required]]
    });

    this.tripTimelineSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.tripTimelineSettingsFormGroup.get('showTimelineControl').valueChanges,
          this.tripTimelineSettingsFormGroup.get('showTimestamp').valueChanges,
          this.tripTimelineSettingsFormGroup.get('snapToRealLocation').valueChanges
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
      this.tripTimelineSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripTimelineSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TripTimelineSettings): void {
    this.modelValue = value;
    this.tripTimelineSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.settingsExpanded = this.tripTimelineSettingsFormGroup.get('showTimelineControl').value;
    this.tripTimelineSettingsFormGroup.get('showTimelineControl').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((show) => {
      this.settingsExpanded = show;
    });
  }

  public validate(c: UntypedFormControl) {
    const valid = this.tripTimelineSettingsFormGroup.valid;
    return valid ? null : {
      tripTimelineSettings: {
        valid: false,
      },
    };
  }

  private updateValidators() {
    const showTimelineControl: boolean = this.tripTimelineSettingsFormGroup.get('showTimelineControl').value;
    const showTimestamp: boolean = this.tripTimelineSettingsFormGroup.get('showTimestamp').value;
    const snapToRealLocation: boolean = this.tripTimelineSettingsFormGroup.get('snapToRealLocation').value;
    if (showTimelineControl) {
      this.tripTimelineSettingsFormGroup.enable({emitEvent: false});
      if (!showTimestamp) {
        this.tripTimelineSettingsFormGroup.get('timestampFormat').disable({emitEvent: false});
      }
      if (!snapToRealLocation) {
        this.tripTimelineSettingsFormGroup.get('locationSnapFilter').disable({emitEvent: false});
      }
    } else {
      this.tripTimelineSettingsFormGroup.disable({emitEvent: false});
      this.tripTimelineSettingsFormGroup.get('showTimelineControl').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.tripTimelineSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
