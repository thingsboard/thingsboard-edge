///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Input } from '@angular/core';
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

  @Input() isSetDownlink: boolean;
  handlerConfigurationTypes = handlerConfigurationTypes;

  private defaultHandlerConfigurations = {
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
  };

  private fieldsSet = {
    BINARY: [],
    TEXT: [
      'charsetName'
    ],
    JSON: [],
    HEX: [
      'maxFrameLength'
    ]
  };

  constructor() {
    super();
  }

  onIntegrationFormSet() {
    if (this.form.enabled) {
      this.form.get('handlerConfiguration').get('handlerType').valueChanges.subscribe((handlerType) => {
        this.handlerConfigurationTypeChanged();
        this.form.get('handlerConfiguration').patchValue(this.defaultHandlerConfigurations[handlerType], {emitEvent: false});
      });
      this.handlerConfigurationTypeChanged();
    }
  }

  handlerConfigurationTypeChanged() {
    const type: string = this.form.get('handlerConfiguration').get('handlerType').value;
    const controls = this.form.get('handlerConfiguration') as FormGroup;
    const enableField = this.fieldsSet[type];
    const disableField  = Object.values(this.fieldsSet).flat().filter(item => !enableField.includes(item));
    enableFields(controls, enableField);
    disableFields(controls, disableField);
  }
}
