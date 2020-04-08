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

import { BaseData, HasId } from '@shared/models/base-data';
import {
  CellActionDescriptor,
  CellContentFunction,
  CellStyleFunction,
  EntityBooleanFunction,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  EntityGroupDetailsMode,
  entityGroupEntityFields,
  EntityGroupInfo,
  EntityGroupParams,
  EntityGroupSortOrder,
  groupSettingsDefaults,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, isDefined, objToBase64 } from '@core/utils';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { GroupEntitiesDataSource } from '@home/models/datasource/group-entity-datasource';
import { EntityGroupService } from '@core/http/entity-group.service';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { MatDialog } from '@angular/material/dialog';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { GroupEntityTableHeaderComponent } from '@home/components/group/group-entity-table-header.component';
import { InjectionToken, Injector } from '@angular/core';
import { Device } from '@shared/models/device.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { WidgetActionDescriptor, WidgetActionType } from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { StateObject, StateParams } from '@core/api/widget-api.models';
import { ServicesMap } from '@home/models/services.map';
import { map } from 'rxjs/operators';

export const DEVICE_GROUP_CONFIG_FACTORY = new InjectionToken<EntityGroupStateConfigFactory<Device>>(EntityType.DEVICE);

export const groupConfigFactoryTokenMap = new Map<EntityType, InjectionToken<EntityGroupStateConfigFactory<BaseData<HasId>>>>(
  [
    [EntityType.DEVICE, DEVICE_GROUP_CONFIG_FACTORY]
  ]
);

export interface EntityGroupStateConfigFactory<T extends BaseData<HasId>> {
  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<T>): Observable<GroupEntityTableConfig<T>>;
}

export interface EntityGroupStateInfo<T extends BaseData<HasId>> extends EntityGroupInfo {
  origEntityGroup?: EntityGroupInfo;
  customerGroupsTitle?: string;
  parentEntityGroup?: EntityGroupInfo;
  entityGroupConfig?: GroupEntityTableConfig<T>;
}

export class GroupEntityTableConfig<T extends BaseData<HasId>> extends EntityTableConfig<T, PageLink, ShortEntityView> {

  customerId: string;

  settings = groupSettingsDefaults(this.entityGroup.type, this.entityGroup.configuration.settings);
  actionDescriptorsBySourceId: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = {};

  onToggleEntityGroupDetails = () => {};
  onToggleEntityDetails = ($event: Event, entity: ShortEntityView) => {};

  assignmentEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableAssignment;
  manageCredentialsEnabled: EntityBooleanFunction<T | ShortEntityView> = () => this.settings.enableCredentialsManagement;

  constructor(public entityGroup: EntityGroupStateInfo<T>,
              public groupParams: EntityGroupParams) {
    super();
    this.customerId = groupParams.customerId;
    this.entityType = entityGroup.type;
    this.entityTranslations = entityTypeTranslations.get(this.entityType);
    this.entityResources = entityTypeResources.get(this.entityType);
    this.entityTabsComponent = GroupEntityTabsComponent;
    this.headerComponent = GroupEntityTableHeaderComponent;
    this.entitiesDeleteEnabled = this.settings.enableDelete;
    this.searchEnabled = this.settings.enableSearch;
    this.selectionEnabled = this.settings.enableSelection;
    this.displayPagination = this.settings.displayPagination;
    this.defaultPageSize = this.settings.defaultPageSize;
    this.detailsPanelEnabled = this.settings.detailsMode !== EntityGroupDetailsMode.disabled;
    this.deleteEnabled = () => this.settings.enableDelete;
  }
}

export abstract class AbstractGroupConfigFactory<T extends BaseData<HasId>> implements EntityGroupStateConfigFactory<T> {

  protected constructor(protected entityGroupService: EntityGroupService,
              protected userPermissionsService: UserPermissionsService,
              protected translate: TranslateService,
              protected utils: UtilsService,
              protected datePipe: DatePipe,
              protected dialog: MatDialog,
              protected router: Router,
              protected injector: Injector) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<T>): Observable<GroupEntityTableConfig<T>> {
    const config = new GroupEntityTableConfig<T>(entityGroup, params);
    return this.configure(params, config).pipe(
      map((configured) => this.prepareConfiguration(params, configured)
      ));
  }

  protected configure(params: EntityGroupParams, config: GroupEntityTableConfig<T>): Observable<GroupEntityTableConfig<T>> {
    return of(config);
  }

  private prepareConfiguration(params: EntityGroupParams, config: GroupEntityTableConfig<T>): GroupEntityTableConfig<T> {
    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.addEnabled = config.settings.enableAdd;
    } else {
      config.addEnabled = false;
    }
    config.addEntity = () => {
      return this.addGroupEntity(config);
    };
    config.handleRowClick = (event, entity) => {
      return this.onRowClick(config, event, entity);
    };

    const columns = config.entityGroup.configuration.columns.filter((column) => {
      if (column.type === EntityGroupColumnType.TIMESERIES) {
        return this.userPermissionsService.hasGroupEntityPermission(Operation.READ_TELEMETRY, config.entityGroup);
      } else if (column.type === EntityGroupColumnType.CLIENT_ATTRIBUTE ||
        column.type === EntityGroupColumnType.SHARED_ATTRIBUTE ||
        column.type === EntityGroupColumnType.SERVER_ATTRIBUTE) {
        return this.userPermissionsService.hasGroupEntityPermission(Operation.READ_ATTRIBUTES, config.entityGroup);
      } else {
        return true;
      }
    });

    const columnsMap = new Map<string, EntityGroupColumn>();
    columns.forEach((column, index) => {
      column.property = this.getColumnProperty(column);
      column.columnKey = `column${index}`;
      columnsMap.set(column.columnKey, column);
    });

    const entityColumns = columns.map((column) => this.toEntityColumn(column));
    if (entityColumns.length) {
      const width = `${(100 / entityColumns.length).toFixed(2)}%`;
      entityColumns.forEach(entityColumn => entityColumn.width = width);
    }

    config.columns = entityColumns;

    const sortOrderColumn = columns.find((column) => column.sortOrder !== EntityGroupSortOrder.NONE);
    if (sortOrderColumn) {
      config.defaultSortOrder = {property: sortOrderColumn.columnKey,
        direction: sortOrderColumn.sortOrder === EntityGroupSortOrder.ASC ? Direction.ASC : Direction.DESC};
    } else {
      config.defaultSortOrder = null;
    }

    config.dataSource = dataLoadedFunction => {
      return new GroupEntitiesDataSource(
        columnsMap,
        config.entityGroup.id.id,
        this.entityGroupService,
        config.entitySelectionEnabled,
        dataLoadedFunction
      );
    };

    if (config.entityGroup.configuration.actions) {
      for (const actionSourceId of Object.keys(config.entityGroup.configuration.actions)) {
        const descriptors = config.entityGroup.configuration.actions[actionSourceId];
        const actionDescriptors: Array<WidgetActionDescriptor> = [];
        descriptors.forEach((descriptor) => {
          const actionDescriptor: WidgetActionDescriptor = deepClone(descriptor);
          actionDescriptor.displayName = this.utils.customTranslation(descriptor.name, descriptor.name);
          actionDescriptors.push(actionDescriptor);
        });
        config.actionDescriptorsBySourceId[actionSourceId] = actionDescriptors;
      }
    }

    const actionCellButtonDescriptors = config.actionDescriptorsBySourceId.actionCellButton;
    const cellActionDescriptors: CellActionDescriptor<ShortEntityView>[] = [];
    if (actionCellButtonDescriptors) {
      actionCellButtonDescriptors.forEach((descriptor) => {
        cellActionDescriptors.push(
          {
            name: descriptor.displayName,
            icon: descriptor.icon,
            isEnabled: entity => true,
            onAction: ($event, entity) => {
              this.handleDescriptorAction($event, entity, descriptor)
            }
          }
        );
      });
    }
    cellActionDescriptors.push(...config.cellActionDescriptors);
    config.cellActionDescriptors = cellActionDescriptors;
    if (config.settings.detailsMode === EntityGroupDetailsMode.onActionButtonClick) {
      if (this.userPermissionsService.hasGroupEntityPermission(Operation.READ, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant(config.entityTranslations.details),
            icon: 'edit',
            isEnabled: entity => true,
            onAction: ($event, entity) => {
              config.onToggleEntityDetails($event, entity);
            }
          }
        );
      }
    }
    if (config.entitiesDeleteEnabled) {
      config.entitiesDeleteEnabled = this.userPermissionsService.hasGroupEntityPermission(Operation.DELETE, config.entityGroup);
    }
    if (this.userPermissionsService.hasGenericEntityGroupPermission(Operation.CHANGE_OWNER, config.entityGroup) &&
        this.userPermissionsService.isOwnedGroup(config.entityGroup)) {
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.change-owner'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => {
            this.changeEntitiesOwner($event, entities, config);
          }
        }
      );
    }
    if (this.userPermissionsService.hasGenericEntityGroupPermission(Operation.ADD_TO_GROUP, config.entityGroup) &&
      this.userPermissionsService.isOwnedGroup(config.entityGroup) &&
      this.userPermissionsService.hasGenericEntityGroupPermission(Operation.READ, config.entityGroup)) {
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.add-to-group'),
          icon: 'add_circle',
          isEnabled: config.settings.enableGroupTransfer,
          onAction: ($event, entities) => {
            this.addEntitiesToEntityGroup($event, entities);
          }
        }
      );
    }

    if (this.userPermissionsService.hasEntityGroupPermission(Operation.REMOVE_FROM_GROUP, config.entityGroup) &&
      this.userPermissionsService.isOwnedGroup(config.entityGroup)) {
      if (this.userPermissionsService.hasGenericEntityGroupPermission(Operation.ADD_TO_GROUP, config.entityGroup)) {
        config.groupActionDescriptors.push(
          {
            name: this.translate.instant('entity-group.move-to-group'),
            icon: 'swap_vertical_circle',
            isEnabled: config.settings.enableGroupTransfer && !config.entityGroup.groupAll,
            onAction: ($event, entities) => {
              this.moveEntitiesToEntityGroup($event, entities);
            }
          }
        );
      }
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.remove-from-group'),
          icon: 'remove_circle',
          isEnabled: config.settings.enableGroupTransfer && !config.entityGroup.groupAll,
          onAction: ($event, entities) => {
            this.removeEntitiesFromEntityGroup($event, entities);
          }
        }
      );
    }

    return config;
  }

  private changeEntitiesOwner($event: MouseEvent, entities: ShortEntityView[], config: GroupEntityTableConfig<T>) {
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (config.customerId) {
      ownerId = {
        id: config.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    // TODO:
  }

  private addEntitiesToEntityGroup($event: MouseEvent, entities: ShortEntityView[]) {
    // TODO:
  }

  private moveEntitiesToEntityGroup($event: MouseEvent, entities: ShortEntityView[]) {
    // TODO:
  }

  private removeEntitiesFromEntityGroup($event: MouseEvent, entities: ShortEntityView[]) {
    // TODO:
  }

  private onRowClick(config: GroupEntityTableConfig<T>, event: Event, entity: ShortEntityView): boolean {
    if (config.settings.detailsMode === EntityGroupDetailsMode.onRowClick) {
      if (this.userPermissionsService.hasGroupEntityPermission(Operation.READ, config.entityGroup)) {
        return false;
      }
    } else {
      const descriptors = config.actionDescriptorsBySourceId.rowClick;
      if (descriptors && descriptors.length) {
        const descriptor = descriptors[0];
        this.handleDescriptorAction(event, entity, descriptor);
      }
      return true;
    }
  }

  private handleDescriptorAction(event: Event, entity: ShortEntityView, descriptor: WidgetActionDescriptor) {
    if (event) {
      event.stopPropagation();
    }
    const entityId = entity.id as EntityId;
    const entityName = entity.name;
    const entityLabel = entity.label;
    const type = descriptor.type;
    const targetEntityParamName = descriptor.stateEntityParamName;
    let targetEntityId: EntityId;
    if (descriptor.setEntityId) {
      targetEntityId = entityId;
    }
    switch (type) {
      case WidgetActionType.openDashboard:
        const targetDashboardId = descriptor.targetDashboardId;
        const targetDashboardStateId = descriptor.targetDashboardStateId;
        const stateObject: StateObject = {};
        stateObject.params = {};
        this.updateEntityParams(stateObject.params, targetEntityParamName, targetEntityId, entityName, entityLabel);
        if (targetDashboardStateId) {
          stateObject.id = targetDashboardStateId;
        }
        const state = objToBase64([ stateObject ]);
        const url = `/dashboards/${targetDashboardId}?state=${state}`;
        this.router.navigateByUrl(url);
        break;
      case WidgetActionType.custom:
        const customFunction = descriptor.customFunction;
        if (customFunction && customFunction.length > 0) {
          try {
            const customActionFunction = new Function('$event', '$injector', 'entityId',
              'entityName', 'servicesMap', customFunction);
            customActionFunction(event, this.injector, entityId, entityName, ServicesMap);
          } catch (e) {
            console.error(e);
          }
        }
        break;
    }
  }

  private updateEntityParams(params: StateParams, targetEntityParamName?: string, targetEntityId?: EntityId,
                             entityName?: string, entityLabel?: string) {
    if (targetEntityId) {
      let targetEntityParams: StateParams;
      if (targetEntityParamName && targetEntityParamName.length) {
        targetEntityParams = params[targetEntityParamName];
        if (!targetEntityParams) {
          targetEntityParams = {};
          params[targetEntityParamName] = targetEntityParams;
          params.targetEntityParamName = targetEntityParamName;
        }
      } else {
        targetEntityParams = params;
      }
      targetEntityParams.entityId = targetEntityId;
      if (entityName) {
        targetEntityParams.entityName = entityName;
      }
      if (entityLabel) {
        targetEntityParams.entityLabel = entityLabel;
      }
    }
  }

  private addGroupEntity(config: GroupEntityTableConfig<T>): Observable<T> {
    return this.dialog.open<AddGroupEntityDialogComponent, AddGroupEntityDialogData<T>,
      T>(AddGroupEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  private getColumnProperty(column: EntityGroupColumn) {
    switch (column.type) {
      case EntityGroupColumnType.CLIENT_ATTRIBUTE:
        return `client_${column.key}`;
      case EntityGroupColumnType.SHARED_ATTRIBUTE:
        return `shared_${column.key}`;
      case EntityGroupColumnType.SERVER_ATTRIBUTE:
        return `server_${column.key}`;
      case EntityGroupColumnType.TIMESERIES:
        return `timeseries_${column.key}`;
      case EntityGroupColumnType.ENTITY_FIELD:
        return column.key;
    }
  }

  private toEntityColumn(entityGroupColumn: EntityGroupColumn): EntityTableColumn<ShortEntityView> {
    let title: string;
    if (entityGroupColumn.title && entityGroupColumn.title.length) {
      title = entityGroupColumn.title;
    } else {
      if (entityGroupColumn.type === EntityGroupColumnType.ENTITY_FIELD) {
        const entityField = entityGroupEntityFields[entityGroupColumn.key];
        title = this.translate.instant(entityField.name);
      } else {
        title = entityGroupColumn.key;
      }
    }

    let columnCellContentFunction: (...args: any[]) => string = null;
    if (entityGroupColumn.useCellContentFunction && entityGroupColumn.cellContentFunction && entityGroupColumn.cellContentFunction.length) {
      try {
        columnCellContentFunction =
          new Function('value, entity, datePipe', entityGroupColumn.cellContentFunction) as (...args: any[]) => string;
      } catch (e) {}
    }
    const cellContentFunction: CellContentFunction<ShortEntityView> = (entity, property) =>
      this.cellContent(entity, entityGroupColumn, columnCellContentFunction);

    let columnCellStyleFunction: (...args: any[]) => object = null;
    if (entityGroupColumn.useCellStyleFunction && entityGroupColumn.cellStyleFunction && entityGroupColumn.cellStyleFunction.length) {
      try {
        columnCellStyleFunction = new Function('value, entity', entityGroupColumn.cellStyleFunction) as (...args: any[]) => object;
      } catch (e) {}
    }
    const cellStyleFunction: CellStyleFunction<ShortEntityView> = (entity, property) =>
      this.cellStyle(entity, entityGroupColumn, columnCellStyleFunction);

    const column = new EntityTableColumn<ShortEntityView>(
      entityGroupColumn.columnKey,
      title,
      '0px',
      cellContentFunction,
      cellStyleFunction,
      entityGroupColumn.type !== EntityGroupColumnType.TIMESERIES // TODO: Timeseries sort
    );
    column.ignoreTranslate = true;
    column.mobileHide = entityGroupColumn.mobileHide;
    return column;
  }

  private cellStyle(entity: ShortEntityView, column: EntityGroupColumn,
                    cellFunction: (...args: any[]) => object): object {
    let style: object;
    if (cellFunction) {
      const value = entity[column.property];
      try {
        style = cellFunction(value, entity);
      } catch (e) {
        style = {};
      }
    } else {
      style = {};
    }
    return style;
  }

  private cellContent(entity: ShortEntityView,
                      column: EntityGroupColumn, cellFunction: (...args: any[]) => string): string {
    let content: string;
    let value = entity[column.property];
    value = this.utils.customTranslation(value, value);
    if (cellFunction) {
      let strContent = '';
      if (isDefined(value)) {
        strContent = '' + value;
      }
      content = strContent;
      try {
        content = cellFunction(value, entity, this.datePipe);
      } catch (e) {
        content = strContent;
      }
    } else {
      content = this.defaultContent(column, value);
    }
    return content;
  }

  private defaultContent(column: EntityGroupColumn, value: any): string {
    if (isDefined(value)) {
      if (column.type === EntityGroupColumnType.ENTITY_FIELD) {
        const entityField = entityGroupEntityFields[column.key];
        if (entityField.time) {
          return this.datePipe.transform(value, 'yyyy-MM-dd HH:mm:ss');
        }
      }
      return value;
    } else {
      return '';
    }
  }

}
