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
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  extensionKeystoreType,
  identityType,
  opcSecurityTypes,
  opcUaMappingType
} from '../../integration-forms-templates';
import { disableFields, enableFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-opc-ua-integration-form',
  templateUrl: './opc-ua-integration-form.component.html',
  styleUrls: ['./opc-ua-integration-form.component.scss']
})
export class OpcUaIntegrationFormComponent extends IntegrationFormComponent {

  identityType = identityType;
  opcUaMappingType = opcUaMappingType;
  extensionKeystoreType = extensionKeystoreType;
  opcSecurityTypes = opcSecurityTypes;
  showIdentityForm: boolean;

  constructor(private fb: FormBuilder) {
    super();
  }

  onIntegrationFormSet() {
    this.form.get('security').valueChanges.subscribe(() => {
      this.securityChanged();
    });
    this.form.get('identity').get('type').valueChanges.subscribe(() => {
      this.identityTypeChanged();
    });
    this.securityChanged();
    this.identityTypeChanged();
  }

  identityTypeChanged() {
    const type: string = this.form.get('identity').get('type').value;
    if (type === 'username') {
      this.showIdentityForm = true;
      enableFields(this.form.get('identity') as FormGroup, ['username', 'password']);
    } else {
      this.showIdentityForm = false;
      disableFields(this.form.get('identity') as FormGroup, ['username', 'password']);
    }
  }

  securityChanged() {
    if (this.form.get('security').value === 'None')
      this.form.get('keystore').disable();
    else
      this.form.get('keystore').enable();
  }

  addMap() {
    (this.form.get('mapping') as FormArray).push(
      this.fb.group({
        deviceNodePattern: ['Channel1\\.Device\\d+$'],
        mappingType: ['FQN', Validators.required],
        subscriptionTags: this.fb.array([], [Validators.required]),
        namespace: [Validators.min(0)]
      })
    );
  }
}
