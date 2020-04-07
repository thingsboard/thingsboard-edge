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

import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroupComponent } from '@home/pages/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/pages/group/entity-group-tabs.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, publicGroupTypes } from '@shared/models/security.models';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';

@Injectable()
export class EntityGroupsTableConfigResolver implements Resolve<EntityTableConfig<EntityGroupInfo>> {

  private readonly config: EntityTableConfig<EntityGroupInfo> = new EntityTableConfig<EntityGroupInfo>();

  private customerId: string;
  private groupType: EntityType;

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService) {

    this.config.entityType = EntityType.ENTITY_GROUP;
    this.config.entityComponent = EntityGroupComponent;
    this.config.entityTabsComponent = EntityGroupTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
    this.config.entityResources = entityTypeResources.get(EntityType.ENTITY_GROUP);

    this.config.hideDetailsTabsOnEdit = false;

    this.config.entityTitle = (entityGroup) => entityGroup ?
      this.utils.customTranslation(entityGroup.name, entityGroup.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<EntityGroupInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityGroupInfo>('name', 'entity-group.name', '33%', this.config.entityTitle),
      new EntityTableColumn<EntityGroupInfo>('description', 'entity-group.description', '40%',
        (entityGroup) => entityGroup && entityGroup.additionalInfo ? entityGroup.additionalInfo.description : '', entity => ({}), false),
      new EntityTableColumn<EntityGroupInfo>('isPublic', 'entity-group.public', '60px',
        entityGroup => {
          return checkBoxCell(entityGroup && entityGroup.additionalInfo ? entityGroup.additionalInfo.isPublic : false);
        }, () => ({}), false)
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.open'),
        icon: 'view_list',
        isEnabled: (entity) => true,
        onAction: ($event, entity) => this.open($event, entity)
      },
      {
        name: this.translate.instant('action.share'),
        icon: 'share',
        isEnabled: (entity) => entity && publicGroupTypes.has(entity.type)
          && (!entity.additionalInfo || !entity.additionalInfo.isPublic)
          && this.userPermissionsService.isDirectlyOwnedGroup(entity)
          && userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
        onAction: ($event, entity) => this.makePublic($event, entity)
      },
      {
        name: this.translate.instant('action.make-private'),
        icon: 'reply',
        isEnabled: (entity) => entity && publicGroupTypes.has(entity.type)
          && entity.additionalInfo && entity.additionalInfo.isPublic
          && this.userPermissionsService.isDirectlyOwnedGroup(entity)
          && userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
        onAction: ($event, entity) => this.makePrivate($event, entity)
      }
    );

    this.config.deleteEntityTitle = entityGroup =>
      this.translate.instant('entity-group.delete-entity-group-title', { entityGroupName: entityGroup.name });
    this.config.deleteEntityContent = () => this.translate.instant('entity-group.delete-entity-group-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('entity-group.delete-entity-groups-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('entity-group.delete-entity-groups-text');

    this.config.entitiesFetchFunction = pageLink => {
      let fetchObservable: Observable<Array<EntityGroupInfo>>;
      if (this.customerId) {
        fetchObservable = this.entityGroupService.getEntityGroupsByOwnerId(EntityType.CUSTOMER, this.customerId, this.groupType);
      } else {
        fetchObservable = this.entityGroupService.getEntityGroups(this.groupType);
      }
      return fetchObservable.pipe(
        map((entityGroups) => pageLink.filterData(entityGroups))
      );
    };

    this.config.loadEntity = id => this.entityGroupService.getEntityGroup(id.id);

    this.config.saveEntity = entityGroup => {
      entityGroup.type = this.groupType;
      if (this.customerId) {
        entityGroup.ownerId = {
          entityType: EntityType.CUSTOMER,
          id: this.customerId
        }
      }
      return this.entityGroupService.saveEntityGroup(entityGroup).pipe(
        tap((savedEntityGroup) => {
          if (!this.customerId) {
            this.broadcast.broadcast(this.groupType + 'changed');
          }
        }
      ));
    };

    this.config.deleteEntity = id => {
      return this.entityGroupService.deleteEntityGroup(id.id).pipe(
        tap(() => {
          if (!this.customerId) {
            this.broadcast.broadcast(this.groupType + 'changed');
          }
        }
      ));
    };

    this.config.onEntityAction = action => this.onEntityGroupAction(action);

    this.config.deleteEnabled = (entityGroup) => entityGroup && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);
    this.config.detailsReadonly = (entityGroup) =>
      !this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
    this.config.entitySelectionEnabled = (entityGroup) => entityGroup && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<EntityGroupInfo> {
    const routeParams = route.params;
    const routeData = route.data;
    this.customerId = routeParams.customerId;
    if (this.customerId && routeData.childGroupType) {
      this.groupType = routeData.childGroupType;
    } else {
      this.groupType = routeData.groupType;
    }

    let title;
    switch (this.groupType) {
      case EntityType.CUSTOMER:
        title = 'entity-group.customer-groups';
        break;
      case EntityType.ASSET:
        title = 'entity-group.asset-groups';
        break;
      case EntityType.DEVICE:
        title = 'entity-group.device-groups';
        break;
      case EntityType.ENTITY_VIEW:
        title = 'entity-group.entity-view-groups';
        break;
      case EntityType.USER:
        title = 'entity-group.user-groups';
        break;
      case EntityType.DASHBOARD:
        title = 'entity-group.dashboard-groups';
        break;
    }
    this.config.tableTitle = this.translate.instant(title);
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  makePublic($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }

  }

  makePrivate($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
  }

  open($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }

  }

  onEntityGroupAction(action: EntityAction<EntityGroupInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.open(action.event, action.entity);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'makePrivate':
        this.makePrivate(action.event, action.entity);
        return true;
    }
    return false;
  }

}
