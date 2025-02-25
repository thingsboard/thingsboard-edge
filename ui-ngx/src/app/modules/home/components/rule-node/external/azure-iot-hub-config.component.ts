///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  AzureIotHubCredentialsType,
  azureIotHubCredentialsTypes,
  azureIotHubCredentialsTypeTranslations
} from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-external-node-azure-iot-hub-config',
  templateUrl: './azure-iot-hub-config.component.html',
  styleUrls: ['./mqtt-config.component.scss']
})
export class AzureIotHubConfigComponent extends RuleNodeConfigurationComponent {

  azureIotHubConfigForm: UntypedFormGroup;

  allAzureIotHubCredentialsTypes = azureIotHubCredentialsTypes;
  azureIotHubCredentialsTypeTranslationsMap = azureIotHubCredentialsTypeTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.azureIotHubConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.azureIotHubConfigForm = this.fb.group({
      topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
        [Validators.required, Validators.min(1), Validators.max(200)]],
      clientId: [configuration ? configuration.clientId : null, [Validators.required]],
      cleanSession: [configuration ? configuration.cleanSession : false, []],
      ssl: [configuration ? configuration.ssl : false, []],
      credentials: this.fb.group(
        {
          type: [configuration && configuration.credentials ? configuration.credentials.type : null, [Validators.required]],
          sasKey: [configuration && configuration.credentials ? configuration.credentials.sasKey : null, []],
          caCert: [configuration && configuration.credentials ? configuration.credentials.caCert : null, []],
          caCertFileName: [configuration && configuration.credentials ? configuration.credentials.caCertFileName : null, []],
          privateKey: [configuration && configuration.credentials ? configuration.credentials.privateKey : null, []],
          privateKeyFileName: [configuration && configuration.credentials ? configuration.credentials.privateKeyFileName : null, []],
          cert: [configuration && configuration.credentials ? configuration.credentials.cert : null, []],
          certFileName: [configuration && configuration.credentials ? configuration.credentials.certFileName : null, []],
          password: [configuration && configuration.credentials ? configuration.credentials.password : null, []],
        }
      )
    });
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    const credentialsType: AzureIotHubCredentialsType = configuration.credentials.type;
      if (credentialsType === 'sas') {
        configuration.credentials = {
          type: credentialsType,
          sasKey: configuration.credentials.sasKey,
          caCert: configuration.credentials.caCert,
          caCertFileName: configuration.credentials.caCertFileName
        };
    }
    return configuration;
  }

  protected validatorTriggers(): string[] {
    return ['credentials.type'];
  }

  protected updateValidators(emitEvent: boolean) {
    const credentialsControl = this.azureIotHubConfigForm.get('credentials');
    const credentialsType: AzureIotHubCredentialsType = credentialsControl.get('type').value;
    if (emitEvent) {
      credentialsControl.reset({ type: credentialsType }, {emitEvent: false});
    }
    credentialsControl.get('sasKey').setValidators([]);
    credentialsControl.get('privateKey').setValidators([]);
    credentialsControl.get('privateKeyFileName').setValidators([]);
    credentialsControl.get('cert').setValidators([]);
    credentialsControl.get('certFileName').setValidators([]);
    switch (credentialsType) {
      case 'sas':
        credentialsControl.get('sasKey').setValidators([Validators.required]);
        break;
      case 'cert.PEM':
        credentialsControl.get('privateKey').setValidators([Validators.required]);
        credentialsControl.get('privateKeyFileName').setValidators([Validators.required]);
        credentialsControl.get('cert').setValidators([Validators.required]);
        credentialsControl.get('certFileName').setValidators([Validators.required]);
        break;
    }
    credentialsControl.get('sasKey').updateValueAndValidity({emitEvent});
    credentialsControl.get('privateKey').updateValueAndValidity({emitEvent});
    credentialsControl.get('privateKeyFileName').updateValueAndValidity({emitEvent});
    credentialsControl.get('cert').updateValueAndValidity({emitEvent});
    credentialsControl.get('certFileName').updateValueAndValidity({emitEvent});
  }
}
