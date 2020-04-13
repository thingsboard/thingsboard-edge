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

import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupInfo,
  EntityGroupParams,
  entityGroupsTitle,
  resolveGroupParams
} from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroupComponent } from '@home/pages/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/pages/group/entity-group-tabs.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, publicGroupTypes, resourceByEntityType } from '@shared/models/security.models';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { isDefinedAndNotNull } from '@core/utils';
import { CustomerService } from '@core/http/customer.service';

@Injectable()
export class EntityGroupsTableConfigResolver implements Resolve<EntityTableConfig<EntityGroupInfo>> {

  private readonly config: EntityTableConfig<EntityGroupInfo> = new EntityTableConfig<EntityGroupInfo>();

  private customerId: string;
  private groupType: EntityType;

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private route: ActivatedRoute,
              private router: Router,
              private homeDialogs: HomeDialogsService) {

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
        (entityGroup) =>
          entityGroup && entityGroup.additionalInfo && isDefinedAndNotNull(entityGroup.additionalInfo.description)
            ? entityGroup.additionalInfo.description : '', entity => ({}), false),
      new EntityTableColumn<EntityGroupInfo>('isPublic', 'entity-group.public', '60px',
        entityGroup => {
          return checkBoxCell(entityGroup && entityGroup.additionalInfo ? entityGroup.additionalInfo.isPublic : false);
        }, () => ({}), false)
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

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EntityGroupInfo>> | EntityTableConfig<EntityGroupInfo> {
    return this.resolveEntityGroupTableConfig(resolveGroupParams(route));
  }

  resolveEntityGroupTableConfig(params: EntityGroupParams, resolveCustomer = true):
    Observable<EntityTableConfig<EntityGroupInfo>> | EntityTableConfig<EntityGroupInfo> {
    this.customerId = params.customerId;
    if (this.customerId && params.childGroupType) {
      this.groupType = params.childGroupType;
    } else {
      this.groupType = params.groupType;
    }
    const resource = resourceByEntityType.get(this.config.entityType);
    if (!this.userPermissionsService.hasGenericPermission(resource, Operation.CREATE)) {
      this.config.addEnabled = false;
    }
    if (!this.userPermissionsService.hasGenericPermission(resource, Operation.DELETE)) {
      this.config.entitiesDeleteEnabled = false;
    }
    this.config.componentsData = {
      isGroupEntitiesView: false
    };
    this.updateActionCellDescriptors();
    if (this.customerId && resolveCustomer) {
      return this.customerService.getShortCustomerInfo(this.customerId).pipe(
        map((info) => {
          this.config.tableTitle = info.title + ': ' + this.translate.instant(entityGroupsTitle(this.groupType));
          return this.config;
        })
      );
    } else {
      this.config.tableTitle = this.translate.instant(entityGroupsTitle(this.groupType));
      return this.config;
    }
  }

  updateActionCellDescriptors() {
    this.config.cellActionDescriptors.splice(0);
    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.open'),
        icon: 'view_list',
        isEnabled: (entity) => true,
        onAction: ($event, entity) => this.open($event, entity)
      }
    );
    if (publicGroupTypes.has(this.groupType)) {
      this.config.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.share'),
          icon: 'share',
          isEnabled: (entity) => entity && publicGroupTypes.has(entity.type)
            && (!entity.additionalInfo || !entity.additionalInfo.isPublic)
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('action.make-private'),
          icon: 'reply',
          isEnabled: (entity) => entity && publicGroupTypes.has(entity.type)
            && entity.additionalInfo && entity.additionalInfo.isPublic
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePrivate($event, entity)
        }
      );
    }
  }

  makePublic($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPublic($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          if (this.config.componentsData.isGroupEntitiesView) {
            this.config.componentsData.reloadEntityGroup();
          } else {
            this.config.table.updateData();
          }
        }
    });
  }

  makePrivate($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPrivate($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          if (this.config.componentsData.isGroupEntitiesView) {
            this.config.componentsData.reloadEntityGroup();
          } else {
            this.config.table.updateData();
          }
        }
    });
  }

  open($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([entityGroup.id.id], {relativeTo: this.config.table.route});
    this.router.navigateByUrl(url);
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
