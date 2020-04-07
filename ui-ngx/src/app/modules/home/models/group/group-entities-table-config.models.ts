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
  CellContentFunction,
  CellStyleFunction,
  EntityBooleanFunction,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  EntityGroupColumn,
  EntityGroupColumnType,
  entityGroupEntityFields,
  EntityGroupInfo,
  EntityGroupSettings,
  EntityGroupSortOrder,
  groupSettingsDefaults,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { isDefined } from '@core/utils';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { GroupEntitiesDataSource } from '@home/models/datasource/group-entity-datasource';
import { EntityGroupService } from '@core/http/entity-group.service';
import { Params } from '@angular/router';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { User } from '@shared/models/user.model';
import { AddUserDialogComponent, AddUserDialogData } from '@home/pages/user/add-user-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';

/*export abstract class GroupEntitiesTableConfigResolver<T extends BaseData<HasId>> implements Resolve<GroupEntityTableConfig<T>> {

  entityGroupId: string;

  constructor(private store: Store<AppState>,
              private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe) {

  }

  resolve(route: ActivatedRouteSnapshot): Observable<GroupEntityTableConfig<BaseData<HasId>>> {
    const routeParams = route.params;
    this.entityGroupId = routeParams.entityGroupId;
    this.entityGroupService.
  }
}*/

export interface EntityGroupStateConfigFactory<T extends BaseData<HasId>> {
  createConfig(params: Params, entityGroup: EntityGroupStateInfo<T>): Observable<GroupEntityTableConfig<T>>;
}

export interface EntityGroupStateInfo<T extends BaseData<HasId>> extends EntityGroupInfo {
  origEntityGroup?: EntityGroupInfo;
  customerGroupsTitle?: string;
  parentEntityGroup?: EntityGroupInfo;
  entityGroupConfig?: GroupEntityTableConfig<T>;
}

export class GroupEntityTableConfig<T extends BaseData<HasId>> extends EntityTableConfig<T, PageLink, ShortEntityView> {

  public settings: EntityGroupSettings;

  assignmentEnabled: EntityBooleanFunction<T> = () => this.settings.enableAssignment;
  manageCredentialsEnabled: EntityBooleanFunction<T> = () => this.settings.enableCredentialsManagement;

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              public entityGroup: EntityGroupInfo) {
    super();
    this.settings = groupSettingsDefaults(entityGroup.type, entityGroup.configuration.settings);

    this.entityTabsComponent = GroupEntityTabsComponent;

    this.addEntity = () => this.addGroupEntity();

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, this.entityGroup)) {
      this.addEnabled = this.settings.enableAdd;
    } else {
      this.addEnabled = false;
    }
    this.deleteEnabled = () => {
      return this.settings.enableDelete;
    };
    this.entitiesDeleteEnabled = this.settings.enableDelete;

    const columns = this.entityGroup.configuration.columns.filter((column) => {
      if (column.type === EntityGroupColumnType.TIMESERIES) {
        return this.userPermissionsService.hasGroupEntityPermission(Operation.READ_TELEMETRY, this.entityGroup);
      } else if (column.type === EntityGroupColumnType.CLIENT_ATTRIBUTE ||
        column.type === EntityGroupColumnType.SHARED_ATTRIBUTE ||
        column.type === EntityGroupColumnType.SERVER_ATTRIBUTE) {
        return this.userPermissionsService.hasGroupEntityPermission(Operation.READ_ATTRIBUTES, this.entityGroup);
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

    this.columns = entityColumns;

    const sortOrderColumn = columns.find((column) => column.sortOrder !== EntityGroupSortOrder.NONE);
    if (sortOrderColumn) {
      this.defaultSortOrder = {property: sortOrderColumn.columnKey,
        direction: sortOrderColumn.sortOrder === EntityGroupSortOrder.ASC ? Direction.ASC : Direction.DESC};
    } else {
      this.defaultSortOrder = null;
    }

    this.dataSource = dataLoadedFunction => {
      return new GroupEntitiesDataSource(
        columnsMap,
        this.entityGroup.id.id,
        this.entityGroupService,
        this.entitySelectionEnabled,
        dataLoadedFunction
      );
    };

  }

  private addGroupEntity(): Observable<T> {
    return this.dialog.open<AddGroupEntityDialogComponent, AddGroupEntityDialogData<T>,
      T>(AddGroupEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: this
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
