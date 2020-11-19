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

import {BaseData} from "@shared/models/base-data";
import {AuditLogId} from "@shared/models/id/audit-log-id";
import {TenantId} from "@shared/models/id/tenant-id";
import {CustomerId} from "@shared/models/id/customer-id";
import {EntityId} from "@shared/models/id/entity-id";
import {UserId} from "@shared/models/id/user-id";
import {ActionStatus, ActionType} from "@shared/models/audit-log.models";
import {HasUUID} from "@shared/models/id/has-uuid";

export interface EdgeSettings {
  edgeId: string;
  tenantId: string
  name: string;
  type: string;
  routingKey: string;
  cloudType: CloudType.PE | CloudType.CE;
}

export interface CloudEvent extends BaseData<CloudEventId> {
  cloudEventAction: string;
  cloudEventType: any; //TODO check exact type
  entityBody: any;
  entityGroupId: any; //TODO check exact type
  entityId: any; //TODO check exact type
  tenantId: TenantId;
}

export class CloudEventId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}

export enum CloudType {
  PE = "PE",
  CE = "CE"
}

export enum cloudConnectionStatus {
  true = 'edge.connected',
  false = 'edge.disconnected'
}

export interface CloudStatus {
  label: string,
  isActive: boolean
}

export enum CloudEventType {
  DASHBOARD,
  ASSET,
  DEVICE,
  ENTITY_VIEW,
  ALARM,
  RULE_CHAIN,
  RULE_CHAIN_METADATA,
  USER,
  CUSTOMER,
  RELATION,
  ENTITY_GROUP
}

