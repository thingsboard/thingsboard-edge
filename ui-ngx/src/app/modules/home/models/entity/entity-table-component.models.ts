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

import { BaseData, HasId } from '@shared/models/base-data';
import { EntityTypeTranslation } from '@shared/models/entity-type.models';
import { SafeHtml } from '@angular/platform-browser';
import { PageLink } from '@shared/models/page/page-link';
import { Timewindow } from '@shared/models/time/time.models';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { ElementRef, EventEmitter, Renderer2, ViewContainerRef } from '@angular/core';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  CellActionDescriptor, EntityActionTableColumn, EntityColumn, EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRoute } from '@angular/router';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';

export type EntitiesTableAction = 'add';

export interface IEntitiesTableComponent {
  entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;
  translations: EntityTypeTranslation;
  headerActionDescriptors: Array<HeaderActionDescriptor>;
  groupActionDescriptors: Array<GroupActionDescriptor<BaseData<HasId>>>;
  cellActionDescriptors: Array<CellActionDescriptor<BaseData<HasId>>>;
  actionColumns: Array<EntityActionTableColumn<BaseData<HasId>>>;
  entityColumns: Array<EntityTableColumn<BaseData<HasId>>>;
  displayedColumns: string[];
  headerCellStyleCache: Array<any>;
  cellContentCache: Array<SafeHtml>;
  cellTooltipCache: Array<string>;
  cellStyleCache: Array<any>;
  selectionEnabled: boolean;
  defaultPageSize: number;
  displayPagination: boolean;
  pageSizeOptions: number[];
  pageLink: PageLink;
  pageMode: boolean;
  textSearchMode: boolean;
  timewindow: Timewindow;
  dataSource: EntitiesDataSource<BaseData<HasId>>;
  isDetailsOpen: boolean;
  detailsPanelOpened: EventEmitter<boolean>;
  entityTableHeaderAnchor: TbAnchorComponent;
  searchInputField: ElementRef;
  paginator: MatPaginator;
  sort: MatSort;
  route: ActivatedRoute;
  entityDetailsPanel: EntityDetailsPanelComponent;
  viewContainerRef: ViewContainerRef;
  renderer: Renderer2;

  addEnabled(): boolean;
  clearSelection(): void;
  updateData(closeDetails?: boolean): void;
  onRowClick($event: Event, entity): void;
  toggleEntityDetails($event: Event, entity);
  addEntity($event: Event): void;
  onEntityUpdated(entity: BaseData<HasId>): void;
  onEntityAction(action: EntityAction<BaseData<HasId>>): void;
  deleteEntity($event: Event, entity: BaseData<HasId>): void;
  deleteEntities($event: Event, entities: BaseData<HasId>[]): void;
  onTimewindowChange(): void;
  enterFilterMode(): void;
  exitFilterMode(): void;
  resetSortAndFilter(update?: boolean, preserveTimewindow?: boolean): void;
  columnsUpdated(resetData?: boolean): void;
  headerCellStyle(column: EntityColumn<BaseData<HasId>>): any;
  clearCellCache(col: number, row: number): void;
  cellContent(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): any;
  cellTooltip(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): string;
  cellStyle(entity: BaseData<HasId>, column: EntityColumn<BaseData<HasId>>, row: number): any;
  trackByColumnKey(index, column: EntityTableColumn<BaseData<HasId>>): string;
  trackByEntityId(index: number, entity: BaseData<HasId>): string;
  detectChanges(): void;
}
