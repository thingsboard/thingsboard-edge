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

import {BaseData, HasId} from '@shared/models/base-data';
import {EntitiesFetchFunction} from '@home/models/datasource/entity-datasource';
import {Observable, of} from 'rxjs';
import {emptyPageData} from '@shared/models/page/page-data';
import {DatePipe} from '@angular/common';
import {Direction, SortOrder} from '@shared/models/page/sort-order';
import {EntityType, EntityTypeResource, EntityTypeTranslation} from '@shared/models/entity-type.models';
import {EntityComponent} from '@home/components/entity/entity.component';
import {Type} from '@angular/core';
import {EntityAction} from './entity-component.models';
import {HasUUID} from '@shared/models/id/has-uuid';
import {PageLink} from '@shared/models/page/page-link';
import {EntitiesTableComponent} from '@home/components/entity/entities-table.component';
import {EntityTableHeaderComponent} from '@home/components/entity/entity-table-header.component';
import {ActivatedRoute} from '@angular/router';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';

export type EntityBooleanFunction<T extends BaseData<HasId>> = (entity: T) => boolean;
export type EntityStringFunction<T extends BaseData<HasId>> = (entity: T) => string;
export type EntityCountStringFunction = (count: number) => string;
export type EntityTwoWayOperation<T extends BaseData<HasId>> = (entity: T) => Observable<T>;
export type EntityByIdOperation<T extends BaseData<HasId>> = (id: HasUUID) => Observable<T>;
export type EntityIdOneWayOperation = (id: HasUUID) => Observable<any>;
export type EntityActionFunction<T extends BaseData<HasId>> = (action: EntityAction<T>) => boolean;
export type CreateEntityOperation<T extends BaseData<HasId>> = () => Observable<T>;

export type CellContentFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string;
export type CellTooltipFunction<T extends BaseData<HasId>> = (entity: T, key: string) => string | undefined;
export type HeaderCellStyleFunction<T extends BaseData<HasId>> = (key: string) => object;
export type CellStyleFunction<T extends BaseData<HasId>> = (entity: T, key: string) => object;

export interface CellActionDescriptor<T extends BaseData<HasId>> {
  name: string;
  nameFunction?: (entity: T) => string;
  icon?: string;
  mdiIcon?: string;
  style?: any;
  isEnabled: (entity: T) => boolean;
  onAction: ($event: MouseEvent, entity: T) => void;
}

export interface GroupActionDescriptor<T extends BaseData<HasId>> {
  name: string;
  icon: string;
  isEnabled: boolean;
  onAction: ($event: MouseEvent, entities: T[]) => void;
}

export interface HeaderActionDescriptor {
  name: string;
  icon: string;
  isEnabled: () => boolean;
  onAction: ($event: MouseEvent) => void;
}

export type EntityTableColumnType = 'content' | 'action';

export class BaseEntityTableColumn<T extends BaseData<HasId>> {
  constructor(public type: EntityTableColumnType,
              public key: string,
              public title: string,
              public width: string = '0px',
              public sortable: boolean = true) {
  }
}

export class EntityTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public width: string = '0px',
              public cellContentFunction: CellContentFunction<T> = (entity, property) => entity[property] ? entity[property] : '',
              public cellStyleFunction: CellStyleFunction<T> = () => ({}),
              public sortable: boolean = true,
              public headerCellStyleFunction: HeaderCellStyleFunction<T> = () => ({}),
              public cellTooltipFunction: CellTooltipFunction<T> = () => undefined,
              public isNumberColumn: boolean = false) {
    super('content', key, title, width, sortable);
  }
}

export class EntityActionTableColumn<T extends BaseData<HasId>> extends BaseEntityTableColumn<T> {
  constructor(public key: string,
              public title: string,
              public actionDescriptor: CellActionDescriptor<T>,
              public width: string = '0px') {
    super('action', key, title, width, false);
  }
}

export class DateEntityTableColumn<T extends BaseData<HasId>> extends EntityTableColumn<T> {
  constructor(key: string,
              title: string,
              datePipe: DatePipe,
              width: string = '0px',
              dateFormat: string = 'yyyy-MM-dd HH:mm:ss',
              cellStyleFunction: CellStyleFunction<T> = () => ({})) {
    super(key,
          title,
          width,
          (entity, property) => datePipe.transform(entity[property], dateFormat),
          cellStyleFunction);
  }
}

export type EntityColumn<T extends BaseData<HasId>> = EntityTableColumn<T> | EntityActionTableColumn<T>;

export class EntityTableConfig<T extends BaseData<HasId>, P extends PageLink = PageLink> {

  constructor() {}

  componentsData: any = null;

  loadDataOnInit = true;
  onLoadAction: (route: ActivatedRoute) => void = null;
  table: EntitiesTableComponent = null;
  useTimePageLink = false;
  entityType: EntityType = null;
  tableTitle = '';
  selectionEnabled = true;
  searchEnabled = true;
  addEnabled = true;
  entitiesDeleteEnabled = true;
  detailsPanelEnabled = true;
  hideDetailsTabsOnEdit = true;
  actionsColumnTitle = null;
  entityTranslations: EntityTypeTranslation;
  entityResources: EntityTypeResource<T>;
  entityComponent: Type<EntityComponent<T>>;
  entityTabsComponent: Type<EntityTabsComponent<T>>;
  addDialogStyle = {};
  defaultSortOrder: SortOrder = {property: 'createdTime', direction: Direction.ASC};
  columns: Array<EntityColumn<T>> = [];
  cellActionDescriptors: Array<CellActionDescriptor<T>> = [];
  groupActionDescriptors: Array<GroupActionDescriptor<T>> = [];
  headerActionDescriptors: Array<HeaderActionDescriptor> = [];
  addActionDescriptors: Array<HeaderActionDescriptor> = [];
  headerComponent: Type<EntityTableHeaderComponent<T>>;
  addEntity: CreateEntityOperation<T> = null;
  detailsReadonly: EntityBooleanFunction<T> = () => false;
  entitySelectionEnabled: EntityBooleanFunction<T> = () => true;
  deleteEnabled: EntityBooleanFunction<T> = () => true;
  deleteEntityTitle: EntityStringFunction<T> = () => '';
  deleteEntityContent: EntityStringFunction<T> = () => '';
  deleteEntitiesTitle: EntityCountStringFunction = () => '';
  deleteEntitiesContent: EntityCountStringFunction = () => '';
  loadEntity: EntityByIdOperation<T> = () => of();
  saveEntity: EntityTwoWayOperation<T> = (entity) => of(entity);
  deleteEntity: EntityIdOneWayOperation = () => of();
  entitiesFetchFunction: EntitiesFetchFunction<T, P> = () => of(emptyPageData<T>());
  onEntityAction: EntityActionFunction<T> = () => false;
  entityTitle: EntityStringFunction<T> = (entity) => entity?.name;
}

export function checkBoxCell(value: boolean): string {
  return `<mat-icon class="material-icons mat-icon">${value ? 'check_box' : 'check_box_outline_blank'}</mat-icon>`;
}
