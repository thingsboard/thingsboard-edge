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
import { takeUntil } from 'rxjs/operators';
import { IntegrationType, ThingParkIntegration } from '@shared/models/integration.models';
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
  selector: 'tb-thing-spark-integration-form',
  templateUrl: './thing-park-integration-form.component.html',
  styleUrls: ['./thing-park-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ThingParkIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => ThingParkIntegrationFormComponent),
    multi: true,
  }]
})
export class ThingParkIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  thingParkConfigForm: FormGroup;

  @Input()
  routingKey: string;

  protected integrationType = IntegrationType.THINGPARK;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(protected fb: FormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    const downlinkUrlValidators = [];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
      downlinkUrlValidators.push(privateNetworkAddressValidator);
    }
    this.thingParkConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey), disabled: true}],
      enableSecurity: [false],
      replaceNoContentToOk: [false],
      downlinkUrl: ['https://api.thingpark.com/thingpark/lrc/rest/downlink', downlinkUrlValidators],
      enableSecurityNew: [{value: false, disabled: true}],
      asId: [{value: '', disabled: true}, Validators.required],
      asIdNew: [{value: '', disabled: true}, Validators.required],
      asKey: [{value: '', disabled: true}, Validators.required],
      clientIdNew: [{value: '', disabled: true}, Validators.required],
      clientSecret: [{value: '', disabled: true}, Validators.required],
      maxTimeDiffInSeconds: [{value: 60, disabled: true}, [Validators.required, Validators.min(0)]]
    });
    this.thingParkConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.thingParkConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
    this.thingParkConfigForm.get('enableSecurity').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updatedEnabledSecurity(value);
    });
    this.thingParkConfigForm.get('enableSecurityNew').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updatedEnabledSecurityNew(value);
    });
    this.thingParkConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.thingParkConfigForm.getRawValue());
    });
  }

  writeValue(value: ThingParkIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.thingParkConfigForm.patchValue(value, {emitEvent: false});
      if (!this.disabled) {
        this.thingParkConfigForm.get('enableSecurity').updateValueAndValidity({onlySelf: true});
        this.thingParkConfigForm.get('enableSecurityNew').updateValueAndValidity({onlySelf: true});
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.thingParkConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.thingParkConfigForm.disable({emitEvent: false});
    } else {
      this.thingParkConfigForm.enable({emitEvent: false});
      this.thingParkConfigForm.get('enableSecurity').updateValueAndValidity({onlySelf: true});
      this.thingParkConfigForm.get('enableSecurityNew').updateValueAndValidity({onlySelf: true});
      this.thingParkConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.thingParkConfigForm.valid ? null : {
      baseHttpIntegrationConfigForm: {valid: false}
    };
  }

  private updatedEnabledSecurity(enable: boolean) {
    if (enable) {
      this.thingParkConfigForm.get('enableSecurityNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('asId').enable({emitEvent: false});
      this.thingParkConfigForm.get('asKey').enable({emitEvent: false});
      this.thingParkConfigForm.get('maxTimeDiffInSeconds').enable({emitEvent: false});
    } else {
      this.thingParkConfigForm.get('enableSecurityNew').disable({emitEvent: false});
      this.thingParkConfigForm.get('asId').disable({emitEvent: false});
      this.thingParkConfigForm.get('asKey').disable({emitEvent: false});
      this.thingParkConfigForm.get('maxTimeDiffInSeconds').disable({emitEvent: false});
    }
  }

  private updatedEnabledSecurityNew(enable: boolean) {
    if (enable) {
      this.thingParkConfigForm.get('clientIdNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('clientSecret').enable({emitEvent: false});
      this.thingParkConfigForm.get('asIdNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('asId').disable({emitEvent: false});
    } else {
      this.thingParkConfigForm.get('clientIdNew').disable({emitEvent: false});
      this.thingParkConfigForm.get('clientSecret').disable({emitEvent: false});
      this.thingParkConfigForm.get('asIdNew').disable({emitEvent: false});
      if (this.thingParkConfigForm.get('enableSecurity').value) {
        this.thingParkConfigForm.get('asId').enable({emitEvent: false});
      }
    }
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
    if (this.thingParkConfigForm) {
      if (this.allowLocalNetwork) {
        this.thingParkConfigForm.get('baseUrl').removeValidators(privateNetworkAddressValidator);
        this.thingParkConfigForm.get('downlinkUrl').removeValidators(privateNetworkAddressValidator);
      } else {
        this.thingParkConfigForm.get('baseUrl').addValidators(privateNetworkAddressValidator);
        this.thingParkConfigForm.get('downlinkUrl').addValidators(privateNetworkAddressValidator);
      }
      this.thingParkConfigForm.get('baseUrl').updateValueAndValidity({emitEvent: false});
      this.thingParkConfigForm.get('downlinkUrl').updateValueAndValidity({emitEvent: false});
    }
  }
}
