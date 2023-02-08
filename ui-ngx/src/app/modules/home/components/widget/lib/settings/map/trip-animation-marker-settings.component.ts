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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { TripAnimationMarkerSettings } from '@home/components/widget/lib/maps/map-models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-trip-animation-marker-settings',
  templateUrl: './trip-animation-marker-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripAnimationMarkerSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripAnimationMarkerSettingsComponent),
      multi: true
    }
  ]
})
export class TripAnimationMarkerSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TripAnimationMarkerSettings;

  private propagateChange = null;

  public tripAnimationMarkerSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationMarkerSettingsFormGroup = this.fb.group({
      rotationAngle: [null, [Validators.min(0), Validators.max(360)]],
      showLabel: [null, []],
      useLabelFunction: [null, []],
      label: [null, []],
      labelFunction: [null, []],
      useMarkerImageFunction: [null, []],
      markerImage: [null, []],
      markerImageSize: [null, [Validators.min(1)]],
      markerImageFunction: [null, []],
      markerImages: [null, []]
    });
    this.tripAnimationMarkerSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationMarkerSettingsFormGroup.get('showLabel').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationMarkerSettingsFormGroup.get('useMarkerImageFunction').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tripAnimationMarkerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationMarkerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TripAnimationMarkerSettings): void {
    this.modelValue = value;
    this.tripAnimationMarkerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.tripAnimationMarkerSettingsFormGroup.valid ? null : {
      tripAnimationMarkerSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TripAnimationMarkerSettings = this.tripAnimationMarkerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showLabel: boolean = this.tripAnimationMarkerSettingsFormGroup.get('showLabel').value;
    const useLabelFunction: boolean = this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').value;
    const useMarkerImageFunction: boolean = this.tripAnimationMarkerSettingsFormGroup.get('useMarkerImageFunction').value;
    if (showLabel) {
      this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').enable({emitEvent: false});
      if (useLabelFunction) {
        this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').enable({emitEvent});
        this.tripAnimationMarkerSettingsFormGroup.get('label').disable({emitEvent});
      } else {
        this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').disable({emitEvent});
        this.tripAnimationMarkerSettingsFormGroup.get('label').enable({emitEvent});
      }
    } else {
      this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').disable({emitEvent: false});
      this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('label').disable({emitEvent});
    }
    if (useMarkerImageFunction) {
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImages').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImage').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').disable({emitEvent});
    } else {
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImages').disable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImage').enable({emitEvent});
      this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').enable({emitEvent});
    }
    this.tripAnimationMarkerSettingsFormGroup.get('useLabelFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('labelFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('label').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImageFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImages').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImage').updateValueAndValidity({emitEvent: false});
    this.tripAnimationMarkerSettingsFormGroup.get('markerImageSize').updateValueAndValidity({emitEvent: false});
  }
}
