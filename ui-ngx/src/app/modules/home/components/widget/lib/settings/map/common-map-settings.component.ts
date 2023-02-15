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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { CommonMapSettings, MapProviders } from '@home/components/widget/lib/maps/map-models';
import { Widget } from '@shared/models/widget.models';

@Component({
  selector: 'tb-common-map-settings',
  templateUrl: './common-map-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CommonMapSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CommonMapSettingsComponent),
      multi: true
    }
  ]
})
export class CommonMapSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator, OnChanges {

  @Input()
  disabled: boolean;

  @Input()
  provider: MapProviders;

  @Input()
  widget: Widget;

  mapProvider = MapProviders;

  private modelValue: CommonMapSettings;

  private propagateChange = null;

  public commonMapSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.commonMapSettingsFormGroup = this.fb.group({
      latKeyName: [null, [Validators.required]],
      lngKeyName: [null, [Validators.required]],
      xPosKeyName: [null, [Validators.required]],
      yPosKeyName: [null, [Validators.required]],
      defaultZoomLevel: [null, [Validators.min(0), Validators.max(20)]],
      defaultCenterPosition: [null, []],
      disableScrollZooming: [null, []],
      disableDoubleClickZooming: [null, []],
      disableZoomControl: [null, []],
      fitMapBounds: [null, []],
      useDefaultCenterPosition: [null, []],
      mapPageSize: [null, [Validators.min(1), Validators.required]]
    });
    this.commonMapSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.updateValidators(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'provider') {
          this.updateValidators(false);
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.commonMapSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.commonMapSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CommonMapSettings): void {
    this.modelValue = value;
    this.commonMapSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.commonMapSettingsFormGroup.valid ? null : {
      commonMapSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: CommonMapSettings = this.commonMapSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    if (this.provider === MapProviders.image) {
      this.commonMapSettingsFormGroup.get('latKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('lngKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultZoomLevel').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultCenterPosition').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('fitMapBounds').disable({emitEvent});

      this.commonMapSettingsFormGroup.get('xPosKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('yPosKeyName').enable({emitEvent});
    } else {
      this.commonMapSettingsFormGroup.get('latKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('lngKeyName').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultZoomLevel').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('defaultCenterPosition').enable({emitEvent});
      this.commonMapSettingsFormGroup.get('fitMapBounds').enable({emitEvent});


      this.commonMapSettingsFormGroup.get('xPosKeyName').disable({emitEvent});
      this.commonMapSettingsFormGroup.get('yPosKeyName').disable({emitEvent});
    }
    this.commonMapSettingsFormGroup.get('latKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('lngKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('xPosKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('yPosKeyName').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('defaultZoomLevel').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('useDefaultCenterPosition').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('defaultCenterPosition').updateValueAndValidity({emitEvent: false});
    this.commonMapSettingsFormGroup.get('fitMapBounds').updateValueAndValidity({emitEvent: false});
  }
}
