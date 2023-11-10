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
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import {
  IntegrationCredentialType,
  IntegrationType,
  KpnIntegration,
} from '@shared/models/integration.models';
import { integrationEndPointUrl, privateNetworkAddressValidator } from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';

@Component({
  selector: 'tb-kpn-integration-form',
  templateUrl: './kpn-integration-form.component.html',
  styleUrls: ['./kpn-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => KpnIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => KpnIntegrationFormComponent),
    multi: true,
  }]
})
export class KpnIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  kpnIntegrationConfigForm: UntypedFormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  @Input()
  routingKey: string;

  private integrationType = IntegrationType.KPN;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private store: Store<AppState>,
              private translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
    }
    this.kpnIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      preSharedKey: ['', Validators.required],
      httpEndpoint: [{
        value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey),
        disabled: true
      }],
      enableSecurity: [false],
      headersFilter: [{}],
      allowDownlink: [false],
      customerId:[],
      gripTenantId:[],
      apiId:[],
      apiKey:[]
    });
    this.kpnIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.kpnIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
      this.kpnIntegrationConfigForm.get('allowDownlink').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnableFields();
    });
    this.kpnIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.kpnIntegrationConfigForm.getRawValue());
    });
    this.kpnIntegrationConfigForm.removeControl('replaceNoContentToOk', {emitEvent: false});
  }

  writeValue(value: KpnIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.kpnIntegrationConfigForm.patchValue(value, {emitEvent: false});
      if (!this.disabled) {
        this.updateEnableFields();
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.kpnIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.kpnIntegrationConfigForm.enable({emitEvent: false});
      this.updateEnableFields();
      this.kpnIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.kpnIntegrationConfigForm.valid ? null : {
      baseHttpIntegrationConfigForm: {valid: false}
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

  private updateModels(value) {
    this.propagateChange(value);
  }

  private updateEnableFields() {
    const allowDownlink = this.kpnIntegrationConfigForm.get('allowDownlink').value;
    if (allowDownlink) {
      this.kpnIntegrationConfigForm.get('customerId').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('gripTenantId').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiId').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiKey').enable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('customerId').setValidators(Validators.required);
      this.kpnIntegrationConfigForm.get('gripTenantId').setValidators(Validators.required);
      this.kpnIntegrationConfigForm.get('apiId').setValidators(Validators.required);
      this.kpnIntegrationConfigForm.get('apiKey').setValidators(Validators.required);
    } else {
      this.kpnIntegrationConfigForm.get('customerId').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('gripTenantId').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiId').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('apiKey').disable({emitEvent: false});
      this.kpnIntegrationConfigForm.get('customerId').clearValidators();
      this.kpnIntegrationConfigForm.get('gripTenantId').clearValidators();
      this.kpnIntegrationConfigForm.get('apiId').clearValidators();
      this.kpnIntegrationConfigForm.get('apiKey').clearValidators();

    }
    this.kpnIntegrationConfigForm.get('customerId').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('gripTenantId').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('apiId').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('apiKey').updateValueAndValidity({emitEvent: false});
    this.kpnIntegrationConfigForm.get('preSharedKey').updateValueAndValidity({emitEvent: false});
  }
}
