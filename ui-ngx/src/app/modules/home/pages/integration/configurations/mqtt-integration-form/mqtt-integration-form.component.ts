///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { mqttCredentialType, mqttCredentialTypes } from '../../integration-forms-templates';
import { changeRequiredCredentialsFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss']
})
export class MqttIntegrationFormComponent extends IntegrationFormComponent {

  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;

  mqttCredentialTypes = mqttCredentialTypes;

  constructor() {
    super();
  }

  onIntegrationFormSet() {
    const form = this.form.get('credentials') as FormGroup;
    form.get('type').valueChanges.subscribe(() => {
      this.mqttCredentialsTypeChanged();
    });
    this.mqttCredentialsTypeChanged();
  }

  mqttCredentialsTypeChanged() {
    const form = this.form.get('credentials') as FormGroup;
    const type: mqttCredentialType = form.get('type').value;
    changeRequiredCredentialsFields(form, type)
  }

}
