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

import { Component, Input, TemplateRef } from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { mqttCredentialType, mqttCredentialTypes } from '../../integration-forms-templates';
import { changeRequiredCredentialsFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss']
})
export class MqttIntegrationFormComponent extends IntegrationFormComponent {

  @Input() topicFilters: FormArray;
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
    changeRequiredCredentialsFields(form, type);
  }

}
