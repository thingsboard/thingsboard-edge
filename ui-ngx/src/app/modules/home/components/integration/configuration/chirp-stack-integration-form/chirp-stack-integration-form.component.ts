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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { ChipStackIntegration, IntegrationType } from '@shared/models/integration.models';
import {
  integrationEndPointUrl,
  privateNetworkAddressValidator
} from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';

@Component({
  selector: 'tb-chirp-stack-integration-form',
  templateUrl: './chirp-stack-integration-form.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ChirpStackIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => ChirpStackIntegrationFormComponent),
    multi: true,
  }]
})
export class ChirpStackIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  chirpStackIntegrationConfigForm: FormGroup;

  @Input()
  routingKey: string;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private store: Store<AppState>,
              private translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
    }
    this.chirpStackIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{value: this.endPointUrl(baseUrl()), disabled: true}],
      applicationServerUrl: [null, Validators.required],
      applicationServerAPIToken: [null, Validators.required]
    });
    this.chirpStackIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.chirpStackIntegrationConfigForm.get('httpEndpoint').patchValue(this.endPointUrl(value));
    });
    this.chirpStackIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.chirpStackIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: ChipStackIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.chirpStackIntegrationConfigForm.reset(value.clientConfiguration, {emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.chirpStackIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.chirpStackIntegrationConfigForm.enable({emitEvent: false});
      this.chirpStackIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  private endPointUrl(url: string): string {
    return integrationEndPointUrl(IntegrationType.CHIRPSTACK, url, this.routingKey);
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  validate(): ValidationErrors | null {
    return this.chirpStackIntegrationConfigForm.valid ? null : {
      chirpStackIntegrationConfigForm: {valid: false}
    };
  }

  onHttpEndpointCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.http-endpoint-url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left',
        target: 'integrationRoot'
      }));
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.chirpStackIntegrationConfigForm?.get('baseUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.chirpStackIntegrationConfigForm?.get('baseUrl').addValidators(privateNetworkAddressValidator);
    }
    this.chirpStackIntegrationConfigForm?.get('baseUrl').updateValueAndValidity({emitEvent: false});
  }
}
