///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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


import { MessageType } from '@shared/models/rule-node.models';
import { isArray, isDefinedAndNotNull, isObject } from '@core/utils';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';

export interface EmailConfig {
  from: string;
  to: string;
  cc?: string;
  bcc?: string;
  subject: string;
  body: string;
}

export const sendRPCRequestDefaults: SchedulerEventConfiguration = {
  msgType: MessageType.RPC_CALL_FROM_SERVER_TO_DEVICE,
  originatorId: null,
  msgBody: {
    method: null,
    params: null
  },
  metadata: {
    oneway: true,
    timeout: 5000,
    persistent: false
  }
};

export const updateAttributesDefaults: SchedulerEventConfiguration = {
  msgType: MessageType.POST_ATTRIBUTES_REQUEST,
  originatorId:  null,
  msgBody: {},
  metadata: {
    scope: null
  }
};

export const defaultEmailConfig: EmailConfig =  {
  from: null,
  to: null,
  subject: 'Report generated on %d{yyyy-MM-dd HH:mm:ss}',
  body: 'Report was successfully generated on %d{yyyy-MM-dd HH:mm:ss}.\nSee attached report file.'
};

export const safeMerge = <T>(defaults: T | { [key: string]: any },
                          value: Partial<T> | { [key: string]: any } | null): T => {
  const result = {...defaults};

  if (value) {
    for (const key in value) {
      if (value.hasOwnProperty(key)) {
        const valueToUpdate = value[key];
        if (isDefinedAndNotNull(valueToUpdate)) {
          if (isObject(valueToUpdate) && !isArray(valueToUpdate)) {
            result[key] = safeMerge(result[key], valueToUpdate);
          } else {
            result[key] = valueToUpdate;
          }
        }
      }
    }
  }

  return result as T;
};
