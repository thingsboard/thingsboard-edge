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

import { FormControl, Validators } from '@angular/forms';
import { IntegrationType, MqttQos, MqttTopicFilter } from '@shared/models/integration.models';

export enum ThingsStartHostType {
  Region = 0,
  Custom = 1
}

export const ThingsStartHostTypeTranslation = new Map<ThingsStartHostType, string> ([
  [ThingsStartHostType.Region, 'Region'],
  [ThingsStartHostType.Custom, 'Custom'],
]);

export const mqttClientIdPatternValidator = Validators.pattern('[a-zA-Z0-9]*');
export const mqttClientIdMaxLengthValidator = Validators.maxLength(23);

const PRIVATE_NETWORK_REGEXP = /^((http|https|pulsar):\/\/)?(127\.|(10\.)|(172\.1[6-9]\.)|(172\.2[0-9]\.)|(172\.3[0-1]\.)|(192\.168\.)|localhost(:[0-9]+)?$)/;

export enum ttnVersion {
  v2,
  v3
}

export interface TtnVersionParameter {
  downlinkPattern: string;
  uplinkTopic: MqttTopicFilter[];
}

export const ttnVersionMap = new Map<ttnVersion, TtnVersionParameter>([
  [
    ttnVersion.v2, {
      downlinkPattern: '${applicationId}/devices/${devId}/down',
      uplinkTopic: [{
        filter: '+/devices/+/up',
        qos: MqttQos.AT_MOST_ONE
      }]
    }
  ],
  [
    ttnVersion.v3, {
    downlinkPattern: 'v3/${applicationId}/devices/${devId}/down/push',
    uplinkTopic: [{
      filter: 'v3/+/devices/+/up',
      qos: MqttQos.AT_MOST_ONE
    }]
  }
  ]
]);

export function integrationEndPointUrl(type: IntegrationType, baseUrl: string, key = ''): string {
  return `${baseUrl}/api/v1/integrations/${type.toLowerCase()}/${key}`;
}

export function privateNetworkAddressValidator(control: FormControl): { [key: string]: any } | null {
  if (control.value) {
    const host = control.value.trim();
    return !PRIVATE_NETWORK_REGEXP.test(host) ? null : {
      privateNetwork: {
        valid: false
      }
    };
  }
  return null;
}
