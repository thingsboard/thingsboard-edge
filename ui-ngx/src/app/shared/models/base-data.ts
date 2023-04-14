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

import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { isDefinedAndNotNull } from '@core/utils';
import { EntityInfoData } from '@shared/models/entity.models';

export declare type HasId = EntityId | HasUUID;

export interface BaseData<T extends HasId> {
  createdTime?: number;
  id?: T;
  name?: string;
  label?: string;
  ownerId?: EntityId;
  tenantId?: TenantId;
  customerId?: CustomerId;
}

export function sortEntitiesByIds<I extends HasId, T extends BaseData<I>>(entities: T[], entityIds: string[]): T[] {
  entities.sort((entity1, entity2) => {
    const id1 = entity1.id.id;
    const id2 = entity2.id.id;
    const index1 = entityIds.indexOf(id1);
    const index2 = entityIds.indexOf(id2);
    return index1 - index2;
  });
  return entities;
}

export interface ExportableEntity<T extends EntityId> {
  createdTime?: number;
  id?: T;
  externalId?: T;
}

export interface GroupEntityInfo<T extends EntityId> extends BaseData<T> {
  ownerId?: EntityId;
  ownerName?: string;
  groups?: EntityInfoData[];
}

export function hasIdEquals(id1: HasId, id2: HasId): boolean {
  if (isDefinedAndNotNull(id1) && isDefinedAndNotNull(id2)) {
    return id1.id === id2.id;
  } else {
    return id1 === id2;
  }
}
