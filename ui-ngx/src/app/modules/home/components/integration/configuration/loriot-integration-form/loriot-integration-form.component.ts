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
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
import { filter, takeUntil } from 'rxjs/operators';
import { IntegrationCredentialType, IntegrationType, LoriotIntegration } from '@shared/models/integration.models';
import {
  integrationEndPointUrl,
  privateNetworkAddressValidator
} from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { merge } from 'rxjs';

@Component({
  selector: 'tb-loriot-integration-form',
  templateUrl: './loriot-integration-form.component.html',
  styleUrls: ['./loriot-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => LoriotIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => LoriotIntegrationFormComponent),
    multi: true,
  }]
})
export class LoriotIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  loriotIntegrationConfigForm: FormGroup;

  IntegrationCredentialType = IntegrationCredentialType;

  @Input()
  routingKey: string;

  private integrationType = IntegrationType.LORIOT;
  private updatedDownlinkUrl = true;

  private propagateChangePending = false;
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
    this.loriotIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{
        value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey),
        disabled: true
      }],
      enableSecurity: [false],
      headersFilter: [{}],
      replaceNoContentToOk: [false],
      createLoriotOutput: [false],
      sendDownlink: [false],
      server: [{value: 'eu1', disabled: true}, Validators.required],
      domain: [{value: 'loriot.io', disabled: true}],
      appId: [{value: '', disabled: true}, Validators.required],
      token: [{value: '', disabled: true}, Validators.required],
      credentials: [{value: {type: IntegrationCredentialType.Basic}, disabled: true}],
      loriotDownlinkUrl: [{value: 'https://eu1.loriot.io/1/rest', disabled: true}, baseURLValidators]
    });
    this.loriotIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.loriotIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
    this.loriotIntegrationConfigForm.get('server').valueChanges.pipe(
      filter(() => this.updatedDownlinkUrl),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.generateLoriotDownlinkUrl(value, this.loriotIntegrationConfigForm.get('domain').value);
    });
    this.loriotIntegrationConfigForm.get('domain').valueChanges.pipe(
      filter(() => this.updatedDownlinkUrl),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.generateLoriotDownlinkUrl(this.loriotIntegrationConfigForm.get('server').value, value);
    });
    merge(
      this.loriotIntegrationConfigForm.get('sendDownlink').valueChanges,
      this.loriotIntegrationConfigForm.get('createLoriotOutput').valueChanges
    ).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnableFields();
    });
    this.loriotIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.loriotIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: LoriotIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.loriotIntegrationConfigForm.patchValue(value, {emitEvent: false});
      this.updatedDownlinkUrl = !value.sendDownlink;
      if (!this.disabled) {
        this.updateEnableFields();
      }
    } else {
      this.propagateChangePending = true;
      this.updatedDownlinkUrl = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.loriotIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.loriotIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.loriotIntegrationConfigForm.enable({emitEvent: false});
      this.updateEnableFields();
      this.loriotIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.loriotIntegrationConfigForm.valid ? null : {
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

  private generateLoriotDownlinkUrl(server: string, domain: string) {
    if (this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').pristine) {
      this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').setValue(`https://${server}.${domain}/1/rest`);
    }
  }

  private updateEnableFields() {
    const createLoriotOutput = this.loriotIntegrationConfigForm.get('createLoriotOutput').value;
    const sendDownlink = this.loriotIntegrationConfigForm.get('sendDownlink').value;
    if (createLoriotOutput || sendDownlink) {
      this.loriotIntegrationConfigForm.get('appId').enable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('server').enable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('domain').enable({emitEvent: false});
    } else {
      this.loriotIntegrationConfigForm.get('appId').disable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('server').disable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('domain').disable({emitEvent: false});
    }
    if (createLoriotOutput) {
      this.loriotIntegrationConfigForm.get('credentials').enable({emitEvent: false});
    } else {
      this.loriotIntegrationConfigForm.get('credentials').disable({emitEvent: false});
    }
    if (sendDownlink) {
      this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').enable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('token').enable({emitEvent: false});
    } else {
      this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').disable({emitEvent: false});
      this.loriotIntegrationConfigForm.get('token').disable({emitEvent: false});
    }
  }

  updatedValidationPrivateNetwork() {
    if (this.loriotIntegrationConfigForm) {
      if (this.allowLocalNetwork) {
        this.loriotIntegrationConfigForm.get('baseUrl').removeValidators(privateNetworkAddressValidator);
        this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').removeValidators(privateNetworkAddressValidator);
      } else {
        this.loriotIntegrationConfigForm.get('baseUrl').addValidators(privateNetworkAddressValidator);
        this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').addValidators(privateNetworkAddressValidator);
      }
      this.loriotIntegrationConfigForm.get('baseUrl').updateValueAndValidity({emitEvent: false});
      this.loriotIntegrationConfigForm.get('loriotDownlinkUrl').updateValueAndValidity({emitEvent: false});
    }
  }
}
