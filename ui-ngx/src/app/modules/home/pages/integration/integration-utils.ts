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
