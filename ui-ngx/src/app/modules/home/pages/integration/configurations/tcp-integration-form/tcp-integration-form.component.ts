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
import {
  handlerConfigurationTypes,
  tcpBinaryByteOrder,
  tcpTextMessageSeparator
} from '../../integration-forms-templates';
import _ from 'lodash';
import { disableFields, enableFields } from '../../integration-utils';
import { IntegrationFormComponent } from '@home/pages/integration/configurations/integration-form.component';

@Component({
  selector: 'tb-tcp-integration-form',
  templateUrl: './tcp-integration-form.component.html',
  styleUrls: ['./tcp-integration-form.component.scss']
})
export class TcpIntegrationFormComponent extends IntegrationFormComponent {

  @Input() isSetDownlink: boolean;

  handlerConfigurationTypes = handlerConfigurationTypes;
  handlerTypes = _.cloneDeep(handlerConfigurationTypes);
  tcpBinaryByteOrder = tcpBinaryByteOrder;
  tcpTextMessageSeparator = tcpTextMessageSeparator;

  defaultHandlerConfigurations = {
    [handlerConfigurationTypes.binary.value]: {
      handlerType: handlerConfigurationTypes.binary.value,
      byteOrder: tcpBinaryByteOrder.littleEndian.value,
      maxFrameLength: 128,
      lengthFieldOffset: 0,
      lengthFieldLength: 2,
      lengthAdjustment: 0,
      initialBytesToStrip: 0,
      failFast: false
    }, [handlerConfigurationTypes.text.value]: {
      handlerType: handlerConfigurationTypes.text.value,
      maxFrameLength: 128,
      stripDelimiter: true,
      messageSeparator: tcpTextMessageSeparator.systemLineSeparator.value
    },
    [handlerConfigurationTypes.json.value]: {
      handlerType: handlerConfigurationTypes.json.value
    }
  };

  constructor() {
    super();
    delete this.handlerTypes.hex;
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
    const handlerConf = this.defaultHandlerConfigurations[type];
    const controls = this.form.get('handlerConfiguration') as FormGroup;
    const fieldsSet = {
      BINARY: [
        'byteOrder',
        'maxFrameLength',
        'lengthFieldOffset',
        'lengthFieldLength',
        'lengthAdjustment',
        'initialBytesToStrip',
        'failFast'
      ],
      TEXT: [
        'maxFrameLength',
        'stripDelimiter',
        'messageSeparator'
      ],
      JSON: []
    };
    disableFields(controls, [...fieldsSet.BINARY, ...fieldsSet.TEXT]);
    enableFields(controls, fieldsSet[type]);
    this.form.get('handlerConfiguration').patchValue(handlerConf, {emitEvent: false});
  }

}
