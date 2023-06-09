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

import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { HasUUID } from '@shared/models/id/has-uuid';
import { isDefinedAndNotNull } from '@core/utils';

export interface EntityId extends HasUUID {
  entityType: EntityType | AliasEntityType;
}

export const entityIdEquals = (entityId1: EntityId, entityId2: EntityId): boolean => {
  if (isDefinedAndNotNull(entityId1) && isDefinedAndNotNull(entityId2)) {
    return entityId1.id === entityId2.id && entityId1.entityType === entityId2.entityType;
  } else {
    return entityId1 === entityId2;
  }
};

const entityIdsSort = (entityId1: EntityId, entityId2: EntityId): number => {
  if (entityId1.entityType === entityId2.entityType) {
    if (isDefinedAndNotNull(entityId1.id) && isDefinedAndNotNull(entityId2.id)) {
      return entityId1.id.localeCompare(entityId2.id);
    } else if (!isDefinedAndNotNull(entityId1.id) && !isDefinedAndNotNull(entityId2.id)) {
      return 0;
    } else {
      return isDefinedAndNotNull(entityId1.id) ? 1 : -1;
    }
  } else {
    if (isDefinedAndNotNull(entityId1.entityType) && isDefinedAndNotNull(entityId2.entityType)) {
      return entityId1.entityType.localeCompare(entityId2.entityType);
    } else if (!isDefinedAndNotNull(entityId1.entityType) && !isDefinedAndNotNull(entityId2.entityType)) {
      return 0;
    } else {
      return isDefinedAndNotNull(entityId1.entityType) ? 1 : -1;
    }
  }
};

export const entityIdsEquals = (entityIds1: EntityId[], entityIds2: EntityId[]): boolean => {
  if (isDefinedAndNotNull(entityIds1) && isDefinedAndNotNull(entityIds2)) {
    if (entityIds1.length === entityIds2.length) {
      entityIds1 = [...entityIds1].sort(entityIdsSort);
      entityIds2 = [...entityIds2].sort(entityIdsSort);
      for (let index = 0; index < entityIds1.length; index++) {
        if (!entityIdEquals(entityIds1[index], entityIds2[index])) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  } else {
    return entityIds1 === entityIds2;
  }
};

export const entityIdsContains = (entityIds: EntityId[], checkEntityId: EntityId): boolean => {
  for (const entityId of entityIds) {
    if (entityIdEquals(entityId, checkEntityId)) {
      return true;
    }
  }
  return false;
}
