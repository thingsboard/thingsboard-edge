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
import { isNotEmptyStr } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { MqttVersions, MqttVersionTranslation } from '@shared/models/device.models';

@Component({
  selector: 'tb-external-node-mqtt-config',
  templateUrl: './mqtt-config.component.html',
  styleUrls: ['./mqtt-config.component.scss']
})
export class MqttConfigComponent extends RuleNodeConfigurationComponent {

  mqttConfigForm: UntypedFormGroup;

  mqttVersions = MqttVersions;
  mqttVersionTranslation = MqttVersionTranslation;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.mqttConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.mqttConfigForm = this.fb.group({
      topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
        [Validators.required, Validators.min(1), Validators.max(200)]],
      clientId: [configuration ? configuration.clientId : null, []],
      appendClientIdSuffix: [{
        value: configuration ? configuration.appendClientIdSuffix : false,
        disabled: !(configuration && isNotEmptyStr(configuration.clientId))
      }, []],
      parseToPlainText: [configuration ? configuration.parseToPlainText : false, []],
      cleanSession: [configuration ? configuration.cleanSession : false, []],
      retainedMessage: [configuration ? configuration.retainedMessage : false, []],
      ssl: [configuration ? configuration.ssl : false, []],
      protocolVersion: [configuration ? configuration.protocolVersion : null, []],
      credentials: [configuration ? configuration.credentials : null, []]
    });
  }

  protected updateValidators(emitEvent: boolean) {
    if (isNotEmptyStr(this.mqttConfigForm.get('clientId').value)) {
      this.mqttConfigForm.get('appendClientIdSuffix').enable({emitEvent: false});
    } else {
      this.mqttConfigForm.get('appendClientIdSuffix').disable({emitEvent: false});
    }
    this.mqttConfigForm.get('appendClientIdSuffix').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['clientId'];
  }
}
