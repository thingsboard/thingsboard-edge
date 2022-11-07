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

import { Component, forwardRef } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { CustomIntegration } from '@shared/models/integration.models';

@Component({
  selector: 'tb-custom-integration-form',
  templateUrl: './custom-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CustomIntegrationFormComponent),
    multi: true
  },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CustomIntegrationFormComponent),
      multi: true,
    }]
})
export class CustomIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  customIntegrationConfigForm: FormGroup;

  // @ViewChild('jsonContentComponent', {static: true}) jsonContentComponent: JsonContentComponent;

  contentType = ContentType;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.customIntegrationConfigForm = this.fb.group({
      clazz: ['', Validators.required],
      configuration: ['{}']
    });
    this.customIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.customIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: CustomIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.customIntegrationConfigForm.reset(value, {emitEvent: false});
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.customIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.customIntegrationConfigForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors {
    return this.customIntegrationConfigForm.valid ? null : {
      customIntegrationConfigForm: {valid: false}
    };
  }

  private updateModels(value) {
    this.propagateChange(value);
  }
}
