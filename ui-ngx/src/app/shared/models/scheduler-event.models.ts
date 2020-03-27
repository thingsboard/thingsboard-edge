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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { SchedulerEventId } from '@shared/models/id/scheduler-event-id';
import { RoleType } from '@shared/models/security.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface SchedulerEventInfo extends BaseData<SchedulerEventId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  schedule: any;
  additionalInfo?: any;
}

export interface SchedulerEventWithCustomerInfo extends SchedulerEventInfo {
  customerTitle: string;
  customerIsPublic: boolean;
  typeName?: string;
}

export interface SchedulerEventConfiguration {
  originatorId?: EntityId;
  msgType?: string;
  msgBody?: any;
  metadata?: any;
}

export interface SchedulerEvent extends SchedulerEventInfo {
  configuration: SchedulerEventConfiguration;
}

export interface SchedulerEventConfigType {
  name: string;
  selector: string;
  template?: string;
  originator: boolean;
  msgType: boolean;
  metadata: boolean;
}

export const defaultSchedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType} = {
  generateReport: {
    name: 'Generate Report',
    selector: 'tb-generate-report-event-config',
    originator: false,
    msgType: false,
    metadata: false
  },
  updateAttributes: {
    name: 'Update Attributes',
    selector: 'tb-update-attributes-event-config',
    originator: false,
    msgType: false,
    metadata: false
  },
  sendRpcRequest: {
    name: 'Send RPC Request to Device',
    selector: 'tb-send-rpc-request-event-config',
    originator: false,
    msgType: false,
    metadata: false
  }
};

