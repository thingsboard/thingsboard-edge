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

import { FormGroup } from '@angular/forms';
import {
  apachePulsarCredentialsType,
  azureIotHubCredentialsType, loriotCredentialType,
  mqttCredentialType
} from '@home/pages/integration/integration-forms-templates';

const basic = ['username', 'password'];
const pem = ['caCertFileName', 'caCert', 'certFileName', 'cert', 'privateKeyFileName', 'privateKey', 'privateKeyPassword'];
const sas = ['sasKey', 'caCertFileName', 'caCert'];
const token = ['token'];

export function changeRequiredCredentialsFields(form: FormGroup, credentialType: mqttCredentialType) {
    let disabled = [];
    let enabled = [];
    switch (credentialType) {
        case 'anonymous':
            disabled = [...basic, ...pem];
            break;
        case 'basic':
            disabled = pem;
            enabled = basic;
            break;
        case 'cert.PEM':
            disabled = basic;
            enabled = pem;
            break;
    }

    disableFields(form, disabled);
    enableFields(form, enabled);
}

export function changeRequiredAzureCredentialsFields(form: FormGroup, credentialType: azureIotHubCredentialsType) {
  let disabled = [];
  let enabled = [];
  switch (credentialType) {
    case 'sas':
      disabled = pem;
      enabled = sas;
      break;
    case 'cert.PEM':
      disabled = basic;
      enabled = pem;
      break;
  }

  disableFields(form, disabled);
  enableFields(form, enabled);
}

export function changeRequiredApachePulsarCredentialsFields(form: FormGroup, credentialType: apachePulsarCredentialsType) {
  let disabled = [];
  let enabled = [];
  switch (credentialType) {
    case 'anonymous':
      disabled = [...token];
      break;
    case 'token':
      enabled = [...token];
      break;
  }

  disableFields(form, disabled);
  enableFields(form, enabled);
}

export function disableFields(form: FormGroup, fields: string[], clear = true) {
    fields.forEach(key => {
        const field = form.get(key);
        if (field) {
          if (clear) {
            field.setValue(null);
          }
          field.disable();
        }
    });
    form.updateValueAndValidity();
}

export function enableFields(form: FormGroup, fields: string[]) {
    fields.forEach(key => {
        const field = form.get(key);
        if (field) {
          field.enable();
        }
    });
    form.updateValueAndValidity();
}
