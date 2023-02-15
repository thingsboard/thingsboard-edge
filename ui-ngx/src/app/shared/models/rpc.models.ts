///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { TenantId } from '@shared/models/id/tenant-id';
import { RpcId } from '@shared/models/id/rpc-id';
import { DeviceId } from '@shared/models/id/device-id';
import { TableCellButtonActionDescriptor } from '@home/components/widget/lib/table-widget.models';

export enum RpcStatus {
  QUEUED = 'QUEUED',
  DELIVERED = 'DELIVERED',
  SUCCESSFUL = 'SUCCESSFUL',
  TIMEOUT = 'TIMEOUT',
  FAILED = 'FAILED',
  SENT = 'SENT',
  EXPIRED = 'EXPIRED'
}

export const rpcStatusColors = new Map<RpcStatus, string>(
  [
    [RpcStatus.QUEUED, 'black'],
    [RpcStatus.DELIVERED, 'green'],
    [RpcStatus.SUCCESSFUL, 'green'],
    [RpcStatus.TIMEOUT, 'orange'],
    [RpcStatus.FAILED, 'red'],
    [RpcStatus.SENT, 'green'],
    [RpcStatus.EXPIRED, 'red']
  ]
);

export const rpcStatusTranslation = new Map<RpcStatus, string>(
  [
    [RpcStatus.QUEUED, 'widgets.persistent-table.rpc-status.QUEUED'],
    [RpcStatus.DELIVERED, 'widgets.persistent-table.rpc-status.DELIVERED'],
    [RpcStatus.SUCCESSFUL, 'widgets.persistent-table.rpc-status.SUCCESSFUL'],
    [RpcStatus.TIMEOUT, 'widgets.persistent-table.rpc-status.TIMEOUT'],
    [RpcStatus.FAILED, 'widgets.persistent-table.rpc-status.FAILED'],
    [RpcStatus.SENT, 'widgets.persistent-table.rpc-status.SENT'],
    [RpcStatus.EXPIRED, 'widgets.persistent-table.rpc-status.EXPIRED']
  ]
);

export interface PersistentRpc {
  id: RpcId;
  createdTime: number;
  expirationTime: number;
  status: RpcStatus;
  response: any;
  request: {
    id: string;
    oneway: boolean;
    body: {
      method: string;
      params: string;
    };
    retries: null | number;
  };
  deviceId: DeviceId;
  tenantId: TenantId;
  additionalInfo?: string;
}

export interface PersistentRpcData extends PersistentRpc {
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
}

export interface RequestData {
  method?: string;
  oneWayElseTwoWay?: boolean;
  persistentPollingInterval?: number;
  retries?: number;
  params?: object;
  additionalInfo?: object;
}
