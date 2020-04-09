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

import { Injectable, Injector, NgZone } from '@angular/core';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { Router } from '@angular/router';
import {
  EntityGroupColumn,
  EntityGroupColumnType, EntityGroupDetailsMode, entityGroupEntityFields,
  EntityGroupParams,
  EntityGroupSortOrder,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { Operation } from '@shared/models/security.models';
import { Direction } from '@shared/models/page/sort-order';
import { GroupEntitiesDataSource } from '@home/models/datasource/group-entity-datasource';
import { WidgetActionDescriptor, WidgetActionType } from '@shared/models/widget.models';
import { deepClone, isDefined, objToBase64 } from '@core/utils';
import {
  CellActionDescriptor,
  CellContentFunction, CellStyleFunction,
  EntityTableColumn
} from '@home/models/entity/entities-table-config.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityId } from '@shared/models/id/entity-id';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { forkJoin, Observable, of } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { SelectEntityGroupDialogResult } from '@home/dialogs/select-entity-group-dialog.component';
import { StateObject, StateParams } from '@core/api/widget-api.models';
import { ServicesMap } from '@home/models/services.map';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';

@Injectable()
export class GroupConfigTableConfigService<T extends BaseData<HasId>> {

  constructor(protected entityGroupService: EntityGroupService,
              protected userPermissionsService: UserPermissionsService,
              protected telemetryWsService: TelemetryWebsocketService,
              protected zone: NgZone,
              protected translate: TranslateService,
              protected utils: UtilsService,
              protected datePipe: DatePipe,
              protected dialog: MatDialog,
              protected homeDialogs: HomeDialogsService,
              protected router: Router,
              protected injector: Injector) {
  }

  prepareConfiguration(params: EntityGroupParams, config: GroupEntityTableConfig<T>): GroupEntityTableConfig<T> {
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

    columns.forEach((column, index) => {
      column.property = this.getColumnProperty(column);
      column.columnKey = `column${index}`;
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
        columns,
        config.entityGroup.id.id,
        this.entityGroupService,
        this.telemetryWsService,
        this.zone,
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
            this.addEntitiesToEntityGroup($event, entities, config);
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
              this.moveEntitiesToEntityGroup($event, entities, config);
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
            this.removeEntitiesFromEntityGroup($event, entities, config);
          }
        }
      );
    }

    return config;
  }

  private changeEntitiesOwner($event: MouseEvent, entities: ShortEntityView[], config: GroupEntityTableConfig<T>) {
    const ignoreErrors = entities.length > 1;
    const onOwnerSelected = (targetOwnerId: EntityId) => {
      return this.homeDialogs.confirm(
        this.translate.instant('entity-group.confirm-change-owner-title', {count: entities.length}),
        this.translate.instant('entity-group.confirm-change-owner-text')).pipe(
        mergeMap((res) => {
            if (res) {
              const changeOwnerObservables: Observable<any>[] = [];
              entities.forEach((entity) => {
                changeOwnerObservables.push(
                  this.entityGroupService.changeEntityOwner(targetOwnerId, entity.id, {ignoreErrors}).pipe(
                    catchError((err) => {
                      if (ignoreErrors) {
                        return of(null);
                      } else {
                        throw err;
                      }
                    })
                  )
                );
              });
              return forkJoin(changeOwnerObservables).pipe(
                map(() => {
                    return true;
                  },
                  catchError((err) => {
                    if (ignoreErrors) {
                      return of(true);
                    } else {
                      throw err;
                    }
                  })
                ));
            } else {
              return of(false);
            }
          }
        ));
    };
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (config.customerId) {
      ownerId = {
        id: config.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    this.homeDialogs.selectOwner($event, 'entity-group.change-owner', 'entity-group.change-owner',
      'entity-group.select-target-owner',
      'entity-group.no-owners-matching',
      'entity-group.target-owner-required', onOwnerSelected,
      [ownerId.id]).subscribe(
      (targetOwnerId) => {
        if (targetOwnerId) {
          config.table.updateData();
        }
      }
    );
  }

  private addEntitiesToEntityGroup($event: MouseEvent, entities: ShortEntityView[], config: GroupEntityTableConfig<T>) {
    const onEntityGroupSelected = (result: SelectEntityGroupDialogResult) => {
      const entityIds = entities.map((entity) => entity.id.id);
      return this.entityGroupService.addEntitiesToEntityGroup(result.groupId, entityIds).pipe(
        map(() => true)
      );
    };
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (config.customerId) {
      ownerId = {
        id: config.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    this.homeDialogs.selectEntityGroup($event, ownerId, config.entityType,
      'entity-group.add-to-group', 'action.add',
      config.entityTranslations.selectGroupToAdd,
      'entity-group.no-entity-groups-matching',
      'entity-group.target-entity-group-required', onEntityGroupSelected,
      [config.entityGroup.id.id]).subscribe(
      (result) => {
        if (result) {
          config.table.clearSelection();
        }
      }
    );
  }

  private moveEntitiesToEntityGroup($event: MouseEvent, entities: ShortEntityView[], config: GroupEntityTableConfig<T>) {
    const entityIds = entities.map((entity) => entity.id.id);
    const onEntityGroupSelected = (result: SelectEntityGroupDialogResult) => {
      return forkJoin([
        this.entityGroupService.removeEntitiesFromEntityGroup(config.entityGroup.id.id, entityIds),
        this.entityGroupService.addEntitiesToEntityGroup(result.groupId, entityIds)
      ]).pipe(
        map(() => true)
      );
    };
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (config.customerId) {
      ownerId = {
        id: config.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    this.homeDialogs.selectEntityGroup($event, ownerId, config.entityType,
      'entity-group.move-to-group', 'action.move',
      config.entityTranslations.selectGroupToMove,
      'entity-group.no-entity-groups-matching',
      'entity-group.target-entity-group-required', onEntityGroupSelected,
      [config.entityGroup.id.id]).subscribe(
      (result) => {
        if (result) {
          config.table.updateData();
        }
      }
    );
  }

  private removeEntitiesFromEntityGroup($event: MouseEvent, entities: ShortEntityView[], config: GroupEntityTableConfig<T>) {
    const title = this.translate.instant('entity-group.remove-from-group');
    const content = this.translate.instant(config.entityTranslations.removeFromGroup,
      {count: entities.length, entityGroup: config.entityGroup.name});
    this.homeDialogs.confirm(title, content).subscribe(
      (res) => {
        if (res) {
          const entityIds = entities.map((entity) => entity.id.id);
          this.entityGroupService.removeEntitiesFromEntityGroup(config.entityGroup.id.id, entityIds).subscribe(
            () => {
              config.table.updateData();
            }
          );
        }
      }
    );
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
