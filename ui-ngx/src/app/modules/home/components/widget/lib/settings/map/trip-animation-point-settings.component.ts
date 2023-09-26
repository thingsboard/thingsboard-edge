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
  Validator, Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  PointsSettings,
  PolylineDecoratorSymbol,
  polylineDecoratorSymbolTranslationMap,
  PolylineSettings
} from '@home/components/widget/lib/maps/map-models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-trip-animation-point-settings',
  templateUrl: './trip-animation-point-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripAnimationPointSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripAnimationPointSettingsComponent),
      multi: true
    }
  ]
})
export class TripAnimationPointSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: PointsSettings;

  private propagateChange = null;

  public tripAnimationPointSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationPointSettingsFormGroup = this.fb.group({
      showPoints: [null, []],
      pointColor: [null, []],
      pointSize: [null, [Validators.min(1)]],
      useColorPointFunction: [null, []],
      colorPointFunction: [null, []],
      usePointAsAnchor: [null, []],
      pointAsAnchorFunction: [null, []],
      pointTooltipOnRightPanel: [null, []]
    });
    this.tripAnimationPointSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationPointSettingsFormGroup.get('showPoints').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').valueChanges.subscribe(() => {
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
      this.tripAnimationPointSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationPointSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: PointsSettings): void {
    this.modelValue = value;
    this.tripAnimationPointSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.tripAnimationPointSettingsFormGroup.valid ? null : {
      tripAnimationPointSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: PointsSettings = this.tripAnimationPointSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showPoints: boolean = this.tripAnimationPointSettingsFormGroup.get('showPoints').value;
    const useColorPointFunction: boolean = this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').value;
    const usePointAsAnchor: boolean = this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').value;

    this.tripAnimationPointSettingsFormGroup.disable({emitEvent: false});
    this.tripAnimationPointSettingsFormGroup.get('showPoints').enable({emitEvent: false});

    if (showPoints) {
      this.tripAnimationPointSettingsFormGroup.get('pointColor').enable({emitEvent: false});
      this.tripAnimationPointSettingsFormGroup.get('pointSize').enable({emitEvent: false});
      this.tripAnimationPointSettingsFormGroup.get('useColorPointFunction').enable({emitEvent: false});
      if (useColorPointFunction) {
        this.tripAnimationPointSettingsFormGroup.get('colorPointFunction').enable({emitEvent: false});
      }
      this.tripAnimationPointSettingsFormGroup.get('usePointAsAnchor').enable({emitEvent: false});
      if (usePointAsAnchor) {
        this.tripAnimationPointSettingsFormGroup.get('pointAsAnchorFunction').enable({emitEvent: false});
      }
      this.tripAnimationPointSettingsFormGroup.get('pointTooltipOnRightPanel').enable({emitEvent: false});
    }
    this.tripAnimationPointSettingsFormGroup.updateValueAndValidity({emitEvent: false});
  }
}
