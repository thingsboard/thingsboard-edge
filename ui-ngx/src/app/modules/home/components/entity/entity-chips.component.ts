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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { BaseData, GroupEntityInfo } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { baseDetailsPageByEntityType, EntityType, groupUrlPrefixByEntityType } from '@app/shared/public-api';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { isEqual, isNotEmptyStr, isObject } from '@core/utils';

@Component({
  selector: 'tb-entity-chips',
  templateUrl: './entity-chips.component.html',
  styleUrls: ['./entity-chips.component.scss']
})
export class EntityChipsComponent implements OnChanges {

  @Input()
  entity: BaseData<EntityId> | GroupEntityInfo<EntityId>;

  @Input()
  key: string;

  @Input()
  detailsPagePrefixUrl: string;

  entityDetailsPrefixUrl: string;

  subEntities: Array<BaseData<EntityId>> = [];

  constructor(private userPermissionsService: UserPermissionsService) {
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (propName === 'entity' && change.currentValue !== change.previousValue) {
        this.update();
      }
    }
  }

  private update(): void {
    if (this.entity && this.entity.id && this.key) {
      let entitiesList = this.entity?.[this.key];
      const entityType = this.entity.id.entityType as EntityType;
      if (isObject(entitiesList) && !Array.isArray(entitiesList)) {
        entitiesList = [entitiesList];
      }
      if (isNotEmptyStr(this.detailsPagePrefixUrl)) {
        this.entityDetailsPrefixUrl = this.detailsPagePrefixUrl;
      } else if (this.key === 'groups' && groupUrlPrefixByEntityType.has(entityType)) {
        this.entityDetailsPrefixUrl = groupUrlPrefixByEntityType.get(entityType);
        if (this.entity.ownerId && !this.userPermissionsService.isDirectOwner(this.entity.ownerId)) {
          this.entityDetailsPrefixUrl = `/customers/all/${this.entity.ownerId.id}${this.entityDetailsPrefixUrl}`;
        }
      } else if (Array.isArray(entitiesList)) {
        if (entitiesList.length) {
          this.entityDetailsPrefixUrl = baseDetailsPageByEntityType.get(entitiesList[0].id.entityType as EntityType);
        }
      } else {
        entitiesList = [];
      }
      if (!isEqual(entitiesList, this.subEntities)) {
        this.subEntities = entitiesList;
      }
    }
  }

}
