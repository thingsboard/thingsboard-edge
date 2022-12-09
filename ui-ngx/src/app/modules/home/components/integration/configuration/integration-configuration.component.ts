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

import { Component, forwardRef, Input, OnDestroy, TemplateRef, ViewEncapsulation } from '@angular/core';
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
import { IntegrationType } from '@shared/models/integration.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-integration-configuration',
  templateUrl: './integration-configuration.component.html',
  styleUrls: ['./integration-configuration.component.scss'],
  encapsulation: ViewEncapsulation.None,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IntegrationConfigurationComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => IntegrationConfigurationComponent),
    multi: true,
  }]
})
export class IntegrationConfigurationComponent implements ControlValueAccessor, Validator, OnDestroy {

  integrationConfigurationForm: FormGroup;
  integrationTypes = IntegrationType;

  @Input() executeRemotelyTemplate: TemplateRef<any>;
  @Input() genericAdditionalInfoTemplate: TemplateRef<any>;

  @Input() isSetDownlink: boolean;
  @Input() routingKey: string;
  @Input() integrationType: IntegrationType;
  @Input() isEdgeTemplate: boolean;
  @Input() allowLocalNetwork = true;

  @Input() disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.integrationConfigurationForm = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.integrationConfigurationForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value.configuration));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.integrationConfigurationForm.disable({emitEvent: false});
    } else {
      this.integrationConfigurationForm.enable({emitEvent: false});
    }
  }

  writeValue(value: any) {
    this.integrationConfigurationForm.get('configuration').reset(value, {emitEvent: false});
  }

  private updateModel(value: any) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.integrationConfigurationForm.valid ? null : {
      integrationConfiguration: {valid: false}
    };
  }
}
