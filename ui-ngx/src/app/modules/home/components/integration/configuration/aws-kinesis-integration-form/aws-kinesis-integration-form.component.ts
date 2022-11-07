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
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import {
  AwsKinesisIntegration,
  InitialPositionInStream,
  InitialPositionInStreamTranslation
} from '@shared/models/integration.models';

@Component({
  selector: 'tb-aws-kinesis-integration-form',
  templateUrl: './aws-kinesis-integration-form.component.html',
  styleUrls: ['./aws-kinesis-integration-form.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AwsKinesisIntegrationFormComponent),
    multi: true
  },
  {
    provide: NG_VALIDATORS,
    useExisting: forwardRef(() => AwsKinesisIntegrationFormComponent),
    multi: true,
  }]
})
export class AwsKinesisIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  awsKinesisConfigForm: FormGroup;

  initialPositionInStreams = Object.keys(InitialPositionInStream);
  InitialPositionInStreamTranslation = InitialPositionInStreamTranslation;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    super();
    this.awsKinesisConfigForm = this.fb.group({
      streamName: ['', Validators.required],
      region: ['', Validators.required],
      accessKeyId: ['', Validators.required],
      secretAccessKey: ['', Validators.required],
      useCredentialsFromInstanceMetadata: [false],
      applicationName: [''],
      initialPositionInStream: ['', Validators.required],
      useConsumersWithEnhancedFanOut: [false],
      maxRecords: [10000, [Validators.required, Validators.min(1), Validators.max(10000)]],
      requestTimeout: [30, Validators.required]
    });

    this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateUsedMetadata(value);
    });
    this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateEnhancedFanOut(value);
    });
    this.awsKinesisConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.awsKinesisConfigForm.getRawValue());
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched() { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.awsKinesisConfigForm.disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.enable({emitEvent: false});
      this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').updateValueAndValidity({onlySelf: true});
      this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    return this.awsKinesisConfigForm.valid ? null : {
      awsKinesisIntegrationConfigForm: {valid: false}
    };
  }

  writeValue(value: AwsKinesisIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.awsKinesisConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').updateValueAndValidity({onlySelf: true});
        this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').updateValueAndValidity({onlySelf: true});
      }
    }
  }

  private updateUsedMetadata(value: boolean) {
    if (value) {
      this.awsKinesisConfigForm.get('accessKeyId').disable({emitEvent: false});
      this.awsKinesisConfigForm.get('secretAccessKey').disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.get('accessKeyId').enable({emitEvent: false});
      this.awsKinesisConfigForm.get('secretAccessKey').enable({emitEvent: false});
    }
  }

  private updateEnhancedFanOut(value: boolean) {
    if (value) {
      this.awsKinesisConfigForm.get('maxRecords').disable({emitEvent: false});
      this.awsKinesisConfigForm.get('requestTimeout').disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.get('maxRecords').enable({emitEvent: false});
      this.awsKinesisConfigForm.get('requestTimeout').enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }
}
