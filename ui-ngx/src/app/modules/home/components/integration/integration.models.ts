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

import { Validators } from '@angular/forms';

export type mqttCredentialType = 'anonymous' | 'basic' | 'cert.PEM';
export const mqttCredentialTypes = {
  anonymous: {
    value: 'anonymous',
    name: 'extension.anonymous'
  },
  basic: {
    value: 'basic',
    name: 'extension.basic'
  },
  'cert.PEM': {
    value: 'cert.PEM',
    name: 'extension.pem'
  }
};

enum MqttQos {
  AT_MOST_ONE = 0,
  AT_LEAST_ONCE = 1,
  EXACTLY_ONCE = 2
}

export const mqttQoSTypes = [
  {
    value: MqttQos.AT_MOST_ONE,
    name: 'integration.mqtt-qos-at-most-once'
  },
  {
    value: MqttQos.AT_LEAST_ONCE,
    name: 'integration.mqtt-qos-at-least-once'
  },
  {
    value: MqttQos.EXACTLY_ONCE,
    name: 'integration.mqtt-qos-exactly-once'
  }
];

export interface MqttTopicFilter {
  filter: string;
  qos: MqttQos;
}

export const mqttClientIdPatternValidator = Validators.pattern('[a-zA-Z0-9]*');
export const mqttClientIdMaxLengthValidator = Validators.maxLength(23);

