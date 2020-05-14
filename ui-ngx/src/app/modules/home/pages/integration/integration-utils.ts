///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { FormGroup, Validators } from '@angular/forms';
import { mqttCredentialType } from '@home/pages/integration/integration-forms-templates';

const basic = ['username', 'password'];
const requiredBasic = ['username', 'password'];
const pem = ['caCertFileName', 'caCert', 'certFileName', 'cert', 'privateKeyFileName', 'privateKey', 'privateKeyPassword'];
const requiredPem = ['caCertFileName', 'caCert', 'certFileName', 'cert', 'privateKeyFileName', 'privateKey'];

export function changeRequiredCredentialsFields(form: FormGroup, credentialType: mqttCredentialType) {
    let disabled = [];
    let enabled = [];
    let required = [];
    switch (credentialType) {
        case 'anonymous':
            disabled = [...basic, ...pem];
            break;
        case 'basic':
            disabled = pem;
            enabled = basic;
            required = requiredBasic;
            break;
        case 'cert.PEM':
            disabled = basic;
            enabled = pem;
            required = requiredPem;
            break;
    }

    disableFields(form, disabled);
    enableFields(form, enabled, required);
}

export function disableFields(form: FormGroup, fields: string[], required: string[] = []) {
    fields.forEach(key => {
        if (form.get(key)) {
          form.get(key).setValue(null);
          form.get(key).disable();
          if (required.includes(key)) {
            form.get(key).setValidators([]);
            form.get(key).updateValueAndValidity();
          }
        }
    });
}

export function enableFields(form: FormGroup, fields: string[], required: string[] = []) {
    fields.forEach(key => {
        if (form.get(key)) {
          form.get(key).enable();
          if (required.includes(key)) {
            form.get(key).setValidators(Validators.required);
            form.get(key).updateValueAndValidity();
          }
        }
    });
}
