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
