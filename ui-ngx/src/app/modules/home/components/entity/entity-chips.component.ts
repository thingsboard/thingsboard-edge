///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import { BaseData, GroupEntityInfo } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { baseDetailsPageByEntityType, EntityType, groupUrlPrefixByEntityType } from '@app/shared/public-api';
import { UserPermissionsService } from '@core/http/user-permissions.service';

const entityTypeEntitiesPropertyKeyMap = new Map<EntityType, string>([
  [EntityType.DOMAIN, 'oauth2ClientInfos'],
  [EntityType.MOBILE_APP, 'oauth2ClientInfos']
]);

@Component({
  selector: 'tb-entity-chips',
  templateUrl: './entity-chips.component.html',
  styleUrls: ['./entity-chips.component.scss']
})
export class EntityChipsComponent {

  @Input()
  set entity(value: BaseData<EntityId> | GroupEntityInfo<EntityId>) {
    this.entityValue = value;
    this.update();
  }

  get entity(): BaseData<EntityId> | GroupEntityInfo<EntityId> {
    return this.entityValue;
  }

  entityDetailsPrefixUrl: string;

  subEntities: Array<BaseData<EntityId>>;

  private entityValue?: BaseData<EntityId> | GroupEntityInfo<EntityId>;

  private subEntitiesKey: string;

  constructor(private userPermissionsService: UserPermissionsService) {
  }

  update(): void {
    if (this.entity && this.entity.id) {
      const entityType = this.entity.id.entityType as EntityType;
      if (entityTypeEntitiesPropertyKeyMap.has(entityType)) {
        this.subEntitiesKey = entityTypeEntitiesPropertyKeyMap.get(entityType);
        this.subEntities = this.entity?.[this.subEntitiesKey];
        if (this.subEntities.length) {
          this.entityDetailsPrefixUrl = baseDetailsPageByEntityType.get(this.subEntities[0].id.entityType as EntityType);
        }
      } else if (groupUrlPrefixByEntityType.has(entityType)) {
        this.entityDetailsPrefixUrl = groupUrlPrefixByEntityType.get(entityType);
        if (this.entity.ownerId && !this.userPermissionsService.isDirectOwner(this.entity.ownerId)) {
          this.entityDetailsPrefixUrl = `/customers/all/${this.entity.ownerId.id}${this.entityDetailsPrefixUrl}`;
        }
        this.subEntities = (this.entity as GroupEntityInfo<EntityId>)?.groups;
      }
    }
  }

}
