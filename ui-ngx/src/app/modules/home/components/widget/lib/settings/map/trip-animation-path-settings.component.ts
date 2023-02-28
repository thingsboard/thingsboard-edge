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
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator, Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  PolylineDecoratorSymbol,
  polylineDecoratorSymbolTranslationMap,
  PolylineSettings
} from '@home/components/widget/lib/maps/map-models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-trip-animation-path-settings',
  templateUrl: './trip-animation-path-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripAnimationPathSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripAnimationPathSettingsComponent),
      multi: true
    }
  ]
})
export class TripAnimationPathSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: PolylineSettings;

  private propagateChange = null;

  public tripAnimationPathSettingsFormGroup: FormGroup;

  polylineDecoratorSymbols = Object.values(PolylineDecoratorSymbol);

  polylineDecoratorSymbolTranslations = polylineDecoratorSymbolTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationPathSettingsFormGroup = this.fb.group({
      color: [null, []],
      strokeWeight: [null, [Validators.min(0)]],
      strokeOpacity: [null, [Validators.min(0), Validators.max(1)]],
      useColorFunction: [null, []],
      colorFunction: [null, []],
      usePolylineDecorator: [null, []],
      decoratorSymbol: [null, []],
      decoratorSymbolSize: [null, [Validators.min(1)]],
      useDecoratorCustomColor: [null, []],
      decoratorCustomColor: [null, []],
      decoratorOffset: [null, []],
      endDecoratorOffset: [null, []],
      decoratorRepeat: [null, []],
    });
    this.tripAnimationPathSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationPathSettingsFormGroup.get('useColorFunction').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPathSettingsFormGroup.get('usePolylineDecorator').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationPathSettingsFormGroup.get('useDecoratorCustomColor').valueChanges.subscribe(() => {
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
      this.tripAnimationPathSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationPathSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: PolylineSettings): void {
    this.modelValue = value;
    this.tripAnimationPathSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.tripAnimationPathSettingsFormGroup.valid ? null : {
      tripAnimationPathSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: PolylineSettings = this.tripAnimationPathSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const useColorFunction: boolean = this.tripAnimationPathSettingsFormGroup.get('useColorFunction').value;
    const usePolylineDecorator: boolean = this.tripAnimationPathSettingsFormGroup.get('usePolylineDecorator').value;
    const useDecoratorCustomColor: boolean = this.tripAnimationPathSettingsFormGroup.get('useDecoratorCustomColor').value;
    if (useColorFunction) {
      this.tripAnimationPathSettingsFormGroup.get('colorFunction').enable({emitEvent});
    } else {
      this.tripAnimationPathSettingsFormGroup.get('colorFunction').disable({emitEvent});
    }
    if (usePolylineDecorator) {
      this.tripAnimationPathSettingsFormGroup.get('decoratorSymbol').enable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('decoratorSymbolSize').enable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('useDecoratorCustomColor').enable({emitEvent: false});
      if (useDecoratorCustomColor) {
        this.tripAnimationPathSettingsFormGroup.get('decoratorCustomColor').enable({emitEvent});
      } else {
        this.tripAnimationPathSettingsFormGroup.get('decoratorCustomColor').disable({emitEvent});
      }
      this.tripAnimationPathSettingsFormGroup.get('decoratorOffset').enable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('endDecoratorOffset').enable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('decoratorRepeat').enable({emitEvent});
    } else {
      this.tripAnimationPathSettingsFormGroup.get('decoratorSymbol').disable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('decoratorSymbolSize').disable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('useDecoratorCustomColor').disable({emitEvent: false});
      this.tripAnimationPathSettingsFormGroup.get('decoratorCustomColor').disable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('decoratorOffset').disable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('endDecoratorOffset').disable({emitEvent});
      this.tripAnimationPathSettingsFormGroup.get('decoratorRepeat').disable({emitEvent});
    }
    this.tripAnimationPathSettingsFormGroup.get('colorFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('decoratorSymbol').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('decoratorSymbolSize').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('useDecoratorCustomColor').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('decoratorCustomColor').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('decoratorOffset').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('endDecoratorOffset').updateValueAndValidity({emitEvent: false});
    this.tripAnimationPathSettingsFormGroup.get('decoratorRepeat').updateValueAndValidity({emitEvent: false});
  }
}
