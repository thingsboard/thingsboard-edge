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
  Validator
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';

export interface UpdateAttributeGeneralSettings {
  widgetTitle: string;
  showLabel: boolean;
  labelValue?: string;
  isRequired: boolean;
  requiredErrorMessage: string;
  showResultMessage: boolean;
}

export function updateAttributeGeneralDefaultSettings(hasLabelValue = true): UpdateAttributeGeneralSettings {
  const updateAttributeGeneralSettings: UpdateAttributeGeneralSettings = {
    widgetTitle: '',
    showLabel: true,
    isRequired: true,
    requiredErrorMessage: '',
    showResultMessage: true
  };
  if (hasLabelValue) {
    updateAttributeGeneralSettings.labelValue = '';
  }
  return updateAttributeGeneralSettings;
}

@Component({
  selector: 'tb-update-attribute-general-settings',
  templateUrl: './update-attribute-general-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UpdateAttributeGeneralSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => UpdateAttributeGeneralSettingsComponent),
      multi: true
    }
  ]
})
export class UpdateAttributeGeneralSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  hasLabelValue = true;

  private modelValue: UpdateAttributeGeneralSettings;

  private propagateChange = null;

  public updateAttributeGeneralSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.updateAttributeGeneralSettingsFormGroup = this.fb.group({
      widgetTitle: ['', []],
      showLabel: [true, []],
      isRequired: [true, []],
      requiredErrorMessage: ['', []],
      showResultMessage: [true, []]
    });
    if (this.hasLabelValue) {
      this.updateAttributeGeneralSettingsFormGroup.addControl('labelValue', this.fb.control('', []));
      this.updateAttributeGeneralSettingsFormGroup.get('showLabel').valueChanges.subscribe(() => {
        this.updateValidators(true);
      });
    }
    this.updateAttributeGeneralSettingsFormGroup.get('isRequired').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.updateAttributeGeneralSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
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
      this.updateAttributeGeneralSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.updateAttributeGeneralSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: UpdateAttributeGeneralSettings): void {
    this.modelValue = value;
    this.updateAttributeGeneralSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.updateAttributeGeneralSettingsFormGroup.valid ? null : {
      updateAttributeGeneralSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: UpdateAttributeGeneralSettings = this.updateAttributeGeneralSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    if (this.hasLabelValue) {
      const showLabel: boolean = this.updateAttributeGeneralSettingsFormGroup.get('showLabel').value;
      if (showLabel) {
        this.updateAttributeGeneralSettingsFormGroup.get('labelValue').enable({emitEvent});
      } else {
        this.updateAttributeGeneralSettingsFormGroup.get('labelValue').disable({emitEvent});
      }
      this.updateAttributeGeneralSettingsFormGroup.get('labelValue').updateValueAndValidity({emitEvent: false});
    }

    const isRequired: boolean = this.updateAttributeGeneralSettingsFormGroup.get('isRequired').value;
    if (isRequired) {
      this.updateAttributeGeneralSettingsFormGroup.get('requiredErrorMessage').enable({emitEvent});
    } else {
      this.updateAttributeGeneralSettingsFormGroup.get('requiredErrorMessage').disable({emitEvent});
    }
    this.updateAttributeGeneralSettingsFormGroup.get('requiredErrorMessage').updateValueAndValidity({emitEvent: false});
  }
}
