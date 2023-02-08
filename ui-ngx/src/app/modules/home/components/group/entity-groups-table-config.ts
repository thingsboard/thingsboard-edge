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

import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityGroup, EntityGroupInfo, EntityGroupParams, entityGroupsTitle } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { ActivatedRoute, Router } from '@angular/router';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getDescendantProp, isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Operation, publicGroupTypes, Resource, sharableGroupTypes } from '@shared/models/security.models';
import { AddEntityDialogData, EntityAction } from '@home/models/entity/entity-component.models';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityGroupWizardDialogComponent,
  EntityGroupWizardDialogResult
} from '@home/components/wizard/entity-group-wizard-dialog.component';
import { AddEntityGroupsToEdgeDialogComponent } from '@home/dialogs/add-entity-groups-to-edge-dialog.component';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';
import { PageLinkSearchFunction } from '@shared/models/page/page-link';

export class EntityGroupsTableConfig extends EntityTableConfig<EntityGroupInfo> {

  customerId: string;
  edgeId: string;
  groupType: EntityType;

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private route: ActivatedRoute,
              private router: Router,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private params: EntityGroupParams) {
    super();

    if (params.hierarchyView) {
      this.pageMode = false;
    }
    this.customerId = params.customerId;
    this.edgeId = params.edgeId;
    if ((this.customerId || this.edgeId) && params.edgeEntitiesType) {
      this.groupType = params.edgeEntitiesType;
    } else if ((this.customerId || this.edgeId) && params.childGroupType) {
      this.groupType = params.childGroupType;
    } else {
      this.groupType = params.groupType;
    }

    this.entityType = EntityType.ENTITY_GROUP;
    this.entityComponent = EntityGroupComponent;
    this.entityTabsComponent = EntityGroupTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
    this.entityResources = entityTypeResources.get(EntityType.ENTITY_GROUP);

    this.rowPointer = true;

    this.hideDetailsTabsOnEdit = false;

    this.entityTitle = (entityGroup) => entityGroup ?
      this.utils.customTranslation(entityGroup.name, entityGroup.name) : '';

    this.columns.push(
      new DateEntityTableColumn<EntityGroupInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityGroupInfo>('name', 'entity-group.name', '33%', this.entityTitle),
      new EntityTableColumn<EntityGroupInfo>('description', 'entity-group.description', '40%',
        (entityGroup) =>
          entityGroup && entityGroup.additionalInfo && isDefinedAndNotNull(entityGroup.additionalInfo.description)
            ? entityGroup.additionalInfo.description : '', entity => ({}), false)
    );
    if (publicGroupTypes.has(this.groupType)) {
      this.columns.push(
        new EntityTableColumn<EntityGroupInfo>('isPublic', 'entity-group.public', '60px',
          entityGroup => {
            return checkBoxCell(entityGroup && entityGroup.additionalInfo ? entityGroup.additionalInfo.isPublic : false);
          }, () => ({}), false)
      );
    }

    this.deleteEntityTitle = entityGroup =>
      this.translate.instant('entity-group.delete-entity-group-title', { entityGroupName: entityGroup.name });
    this.deleteEntityContent = () => this.translate.instant('entity-group.delete-entity-group-text');
    this.deleteEntitiesTitle = count => this.translate.instant('entity-group.delete-entity-groups-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('entity-group.delete-entity-groups-text');

    this.entitiesFetchFunction = pageLink => {
      let fetchObservable: Observable<Array<EntityGroupInfo>>;
      if (this.customerId && !this.isEdgeGroup()) {
        fetchObservable = this.entityGroupService.getEntityGroupsByOwnerId(EntityType.CUSTOMER, this.customerId, this.groupType);
      } else if (this.isEdgeGroup()) {
        fetchObservable = this.entityGroupService.getEdgeEntityGroups(this.edgeId, this.groupType);
      } else {
        fetchObservable = this.entityGroupService.getEntityGroups(this.groupType);
      }
      return fetchObservable.pipe(
        map((entityGroups) => pageLink.filterData(entityGroups, this.groupPageLinkSearchFunction())
        )
      );
    };

    this.loadEntity = id => this.entityGroupService.getEntityGroup(id.id);

    this.saveEntity = (entityGroup, originalEntityGroup) => {
      entityGroup.type = this.groupType;
      if (this.customerId) {
        entityGroup.ownerId = {
          entityType: EntityType.CUSTOMER,
          id: this.customerId
        };
      }
      let saveEntity$: Observable<EntityGroupInfo>;
      if (entityGroup.type === EntityType.DEVICE) {
        saveEntity$ = this.entityGroupService.saveDeviceEntityGroup(entityGroup, originalEntityGroup);
      } else {
        saveEntity$ = this.entityGroupService.saveEntityGroup(entityGroup);
      }
      return saveEntity$.pipe(
        tap((savedEntityGroup) => {
            this.notifyEntityGroupUpdated();
          }
        ));
    };

    this.deleteEntity = id => {
      return this.entityGroupService.deleteEntityGroup(id.id).pipe(
        tap(() => {
            this.notifyEntityGroupUpdated();
          }
        ));
    };

    this.onEntityAction = action => this.onEntityGroupAction(action);

    this.handleRowClick = ($event, entityGroup) => {
      if (this.isDetailsOpen()) {
        this.toggleEntityDetails($event, entityGroup);
      } else {
        this.open($event, entityGroup);
      }
      return true;
    };

    this.deleteEnabled = (entityGroup) => entityGroup && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);
    this.detailsReadonly = (entityGroup) =>
      !this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
    this.entitySelectionEnabled = (entityGroup) => entityGroup && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);

    if (!this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.CREATE, this.groupType)) {
      this.addEnabled = false;
    }
    if (!this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.DELETE, this.groupType)) {
      this.entitiesDeleteEnabled = false;
    }
    this.componentsData = {
      isGroupEntitiesView: false
    };
    this.updateActionCellDescriptors();
    this.tableTitle = this.translate.instant(entityGroupsTitle(this.groupType));
    if (sharableGroupTypes.has(this.groupType) &&
      this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE)) {
        this.addEntity = () => this.entityGroupWizard();
    }
    if (this.isEdgeGroup()) {
      this.deleteEnabled = () => false;
      this.entitiesDeleteEnabled = false;
      this.addEnabled = false;
      this.componentsData.isEdgeGroup = true;
      if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE)) {
        this.entitySelectionEnabled = () => true;
        this.componentsData.isUnassignEnabled = true;
        this.groupActionDescriptors.push(
          {
            name: this.translate.instant('edge.unassign-entity-groups-from-edge'),
            icon: 'assignment_return',
            isEnabled: true,
            onAction: ($event, entities) => {
              this.unassignEntityGroupsFromEdge($event, entities);
            }
          }
        );
        if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER, Operation.READ)) {
          this.headerActionDescriptors.push({
              name: this.translate.instant('edge.assign-to-edge'),
              icon: 'add',
              isEnabled: () => true,
              onAction: ($event) => {
                this.assignEntityGroupsToEdge($event);
              }
            }
          );
        }
      }
    }
  }

  private updateActionCellDescriptors() {
    this.cellActionDescriptors.splice(0);
    if (sharableGroupTypes.has(this.groupType) &&
      this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE)) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.share'),
          icon: 'assignment_ind',
          isEnabled: (entity) => entity && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.share($event, entity)
        }
      );
    }
    if (publicGroupTypes.has(this.groupType)) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.make-public'),
          icon: 'share',
          isEnabled: (entity) => entity
            && (!entity.additionalInfo || !entity.additionalInfo.isPublic)
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('action.make-private'),
          icon: 'reply',
          isEnabled: (entity) => entity
            && entity.additionalInfo && entity.additionalInfo.isPublic
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePrivate($event, entity)
        }
      );
    }
    if (this.isEdgeGroup()) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.unassign-entity-group-from-edge'),
          icon: 'assignment_return',
          isEnabled: (entity) => this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entity) => this.unassignEntityGroupFromEdge($event, entity)
        }
      );
    }
    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('entity-group.entity-group-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.toggleEntityDetails($event, entity)
      }
    );
  }

  private entityGroupWizard(): Observable<EntityGroupInfo> {
    return this.dialog.open<EntityGroupWizardDialogComponent, AddEntityDialogData<EntityGroupInfo>,
      EntityGroupWizardDialogResult>(EntityGroupWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: this
      }
    }).afterClosed().pipe(
      map((result) => {
        if (result && result.shared) {
          this.notifyEntityGroupUpdated();
        }
        return result?.entityGroup;
      }
    ));
  }

  private assignEntityGroupsToEdge($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (this.params.customerId) {
      ownerId = {
        id: this.params.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    this.dialog.open<AddEntityGroupsToEdgeDialogComponent,
      AddEntityGroupsToEdgeDialogData,
      EntityGroupWizardDialogResult>(AddEntityGroupsToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        ownerId,
        groupType: this.groupType,
        edgeId: this.params.edgeId,
        addEntityGroupsToEdgeTitle: 'edge.assign-to-edge-title',
        confirmSelectTitle: 'action.assign',
        notFoundText: 'entity-group.no-entity-groups-matching',
        requiredText: 'entity-group.target-entity-group-required'
      }
    }).afterClosed().subscribe(
      (result) => {
          if (result) {
            this.notifyEntityGroupUpdated();
            this.updateData();
          }
        }
    );
  }

  private share($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.shareEntityGroup($event, entityGroup).subscribe((res) => {
      if (res) {
        this.onGroupUpdated();
      }
    });
  }

  private makePublic($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPublic($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          this.onGroupUpdated();
        }
      });
  }

  private makePrivate($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPrivate($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          this.onGroupUpdated();
        }
      });
  }

  onGroupUpdated(closeDetails = false) {
    this.notifyEntityGroupUpdated();
    if (this.componentsData.isGroupEntitiesView) {
      this.componentsData.reloadEntityGroup();
    } else {
      this.updateData(closeDetails);
    }
  }

  private notifyEntityGroupUpdated() {
    if (!this.customerId) {
      this.broadcast.broadcast(this.groupType + 'changed');
    }
    if (!this.componentsData.isGroupEntitiesView && this.params.hierarchyView) {
      this.params.hierarchyCallbacks.refreshEntityGroups(this.params.internalId);
    }
  }

  private open($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.params.hierarchyView) {
      this.params.hierarchyCallbacks.groupSelected(this.params.nodeId, entityGroup.id.id);
    } else {
      const url = this.router.createUrlTree([entityGroup.id.id], {relativeTo: this.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  private unassignEntityGroupFromEdge($event: Event, entityGroup: EntityGroup) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.unassignEntityGroupFromEdge($event, entityGroup, this.edgeId).subscribe(
      (res) => {
        if (res) {
          this.onGroupUpdated(true);
        }
      }
    );
  }

  private unassignEntityGroupsFromEdge($event: Event, entityGroups: Array<EntityGroup>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.unassignEntityGroupsFromEdge($event, entityGroups, this.edgeId).subscribe(
      (res) => {
        if (res) {
          this.onGroupUpdated();
        }
      }
    );
  }

  private onEntityGroupAction(action: EntityAction<EntityGroupInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.open(action.event, action.entity);
        return true;
      case 'share':
        this.share(action.event, action.entity);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'makePrivate':
        this.makePrivate(action.event, action.entity);
        return true;
      case 'unassign':
        this.unassignEntityGroupFromEdge(action.event, action.entity);
        return true;
    }
    return false;
  }

  private isEdgeGroup(): boolean {
    return isDefinedAndNotNull(this.params.edgeId) && (this.params.groupType === EntityType.EDGE ||
      (this.params.groupType === EntityType.CUSTOMER && this.params.childGroupType === EntityType.EDGE));
  }

  private groupPageLinkSearchFunction(): PageLinkSearchFunction<any> {
    return (entity, textSearch) => this.groupPageLinkSearch(entity, textSearch, ['name', 'additionalInfo.description']);
  }

  private groupPageLinkSearch(entity: any, textSearch: string, searchProperties: string[]): boolean {
    if (textSearch === null || !textSearch.length) {
      return true;
    }
    const expected = ('' + textSearch).toLowerCase();
    for (const searchProperty of searchProperties) {
      const val = getDescendantProp(entity, searchProperty);
      if (isDefinedAndNotNull(val)) {
        const actual = ('' + val).toLowerCase();
        if (actual.indexOf(expected) !== -1) {
          return true;
        }
      }
    }
    return false;
  }
}
