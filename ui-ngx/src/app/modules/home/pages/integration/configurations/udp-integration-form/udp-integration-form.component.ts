///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { handlerConfigurationTypes } from '../../integration-forms-templates';
import { disableFields, enableFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-udp-integration-form',
  templateUrl: './udp-integration-form.component.html',
  styleUrls: ['./udp-integration-form.component.scss']
})
export class UdpIntegrationFormComponent extends IntegrationFormComponent {

  handlerConfigurationTypes = handlerConfigurationTypes;

  defaultHandlerConfigurations = {
    [handlerConfigurationTypes.binary.value]: {
      handlerType: handlerConfigurationTypes.binary.value
    },
    [handlerConfigurationTypes.text.value]: {
      handlerType: handlerConfigurationTypes.text.value,
      charsetName: 'UTF-8'
    },
    [handlerConfigurationTypes.json.value]: {
      handlerType: handlerConfigurationTypes.json.value
    },
    [handlerConfigurationTypes.hex.value]: {
      handlerType: handlerConfigurationTypes.hex.value,
      maxFrameLength: 128
    },
  }

  constructor() {
    super();
  }

  onIntegrationFormSet() {
    if (this.form.enabled) {
      this.form.get('handlerConfiguration').get('handlerType').valueChanges.subscribe(() => {
        this.handlerConfigurationTypeChanged();
      });
      this.handlerConfigurationTypeChanged();
    }
  }

  handlerConfigurationTypeChanged() {
    const type: string = this.form.get('handlerConfiguration').get('handlerType').value;
    disableFields(this.form.get('handlerConfiguration') as FormGroup, ['charsetName', 'maxFrameLength']);
    if (type === handlerConfigurationTypes.hex.value) {
      enableFields(this.form.get('handlerConfiguration') as FormGroup, ['maxFrameLength']);
    }
    if (type === handlerConfigurationTypes.text.value) {
      enableFields(this.form.get('handlerConfiguration') as FormGroup, ['charsetName']);
    }
    this.form.get('handlerConfiguration').patchValue(this.defaultHandlerConfigurations[type], {emitEvent: false});
  };

}
