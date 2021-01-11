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

import { Component } from '@angular/core';
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
  }

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
  };

}
