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

import { Injectable } from '@angular/core';

import { Resolve, Router } from '@angular/router';
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
import { WidgetService } from '@app/core/http/widget.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { Direction } from '@shared/models/page/sort-order';
import {
  BaseWidgetType,
  WidgetTypeDetails,
  WidgetTypeInfo,
  widgetType as WidgetDataType,
  widgetTypesData
} from '@shared/models/widget.models';
import { WidgetTypeComponent } from '@home/pages/widget/widget-type.component';
import { WidgetTypeTabsComponent } from '@home/pages/widget/widget-type-tabs.component';
import { SelectWidgetTypeDialogComponent } from '@home/pages/widget/select-widget-type-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class WidgetTypesTableConfigResolver implements Resolve<EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails>> {

  private readonly config: EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails> =
    new EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails>();

  constructor(private store: Store<AppState>,
              private dialog: MatDialog,
              private widgetsService: WidgetService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.WIDGETS_BUNDLE;
    this.config.entityComponent = WidgetTypeComponent;
    this.config.entityTabsComponent = WidgetTypeTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.WIDGET_TYPE);
    this.config.entityResources = entityTypeResources.get(EntityType.WIDGET_TYPE);
    this.config.defaultSortOrder = {property: 'name', direction: Direction.ASC};

    this.config.rowPointer = true;

    this.config.entityTitle = (widgetType) => widgetType ?
      widgetType.name : '';

    this.config.columns.push(
      new DateEntityTableColumn<WidgetTypeInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<WidgetTypeInfo>('name', 'widget.title', '100%'),
      new EntityTableColumn<WidgetTypeInfo>('widgetType', 'widget.type', '150px', entity =>
        entity?.widgetType ? this.translate.instant(widgetTypesData.get(entity.widgetType).name) : '', undefined, false),
      new EntityTableColumn<WidgetTypeInfo>('tenantId', 'widget.system', '60px',
        entity => checkBoxCell(entity.tenantId.id === NULL_UUID)),
      new EntityTableColumn<WidgetTypeInfo>('deprecated', 'widget.deprecated', '60px',
        entity => checkBoxCell(entity.deprecated))
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('widget-type.create-new-widget-type'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.addWidgetType($event)
      },
      {
        name: this.translate.instant('widget-type.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importWidgetType($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('widget-type.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportWidgetType($event, entity)
      },
      {
        name: this.translate.instant('widget.widget-type-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );

    this.config.groupActionDescriptors.push(
      {
        name: this.translate.instant('widget-type.export-widget-types'),
        icon: 'file_download',
        isEnabled: true,
        onAction: ($event, entities) => this.exportWidgetTypes($event, entities)
      }
    );

    this.config.deleteEntityTitle = widgetType => this.translate.instant('widget.delete-widget-type-title',
      { widgetTypeName: widgetType.name });
    this.config.deleteEntityContent = () => this.translate.instant('widget.delete-widget-type-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('widget.delete-widget-types-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('widget.delete-widget-types-text');


    this.config.loadEntity = id => this.widgetsService.getWidgetTypeById(id.id);
    this.config.saveEntity = widgetType => this.widgetsService.saveWidgetType(widgetType as WidgetTypeDetails);
    this.config.deleteEntity = id => this.widgetsService.deleteWidgetType(id.id);
    this.config.onEntityAction = action => this.onWidgetTypeAction(action);

    this.config.handleRowClick = ($event, widgetType) => {
      if (this.config.isDetailsOpen()) {
        this.config.toggleEntityDetails($event, widgetType);
      } else {
        this.openWidgetEditor($event, widgetType);
      }
      return true;
    };
  }

  resolve(): EntityTableConfig<WidgetTypeInfo | WidgetTypeDetails> {
    this.config.tableTitle = this.translate.instant('widget.widget-types');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (widgetType) =>
      this.isWidgetTypeEditable(widgetType, authUser.authority) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.DELETE);
    this.config.entitySelectionEnabled = (widgetType) =>
      this.isWidgetTypeEditable(widgetType, authUser.authority) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.DELETE);
    this.config.detailsReadonly = (widgetType) => !this.isWidgetTypeEditable(widgetType, authUser.authority);
    this.config.entitiesFetchFunction = pageLink => this.widgetsService.getWidgetTypes(pageLink);

    // @voba - edge read-only
    this.config.detailsReadonly = () => true;
    this.config.deleteEnabled = () => false;
    this.config.addEnabled = false;
    this.config.entitiesDeleteEnabled = false;

    return this.config;
  }

  isWidgetTypeEditable(widgetType: BaseWidgetType, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return widgetType && widgetType.tenantId && widgetType.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }

  addWidgetType($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SelectWidgetTypeDialogComponent, any,
      WidgetDataType>(SelectWidgetTypeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (type) => {
        if (type) {
          this.router.navigateByUrl(`resources/widgets-library/widget-types/${type}`);
        }
      }
    );
  }

  importWidgetType($event: Event) {
    this.importExport.importWidgetType().subscribe(
      (widgetType) => {
        if (widgetType) {
          this.config.updateData();
        }
      }
    );
  }

  openWidgetEditor($event: Event, widgetType: BaseWidgetType) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`resources/widgets-library/widget-types/${widgetType.id.id}`);
  }

  exportWidgetType($event: Event, widgetType: BaseWidgetType) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetType(widgetType.id.id);
  }

  exportWidgetTypes($event: Event, widgetTypes: BaseWidgetType[]) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetTypes(widgetTypes.map(w => w.id.id)).subscribe(
        () => {
          this.config.getTable().clearSelection();
        }
    );
  }

  onWidgetTypeAction(action: EntityAction<BaseWidgetType>): boolean {
    switch (action.action) {
      case 'edit':
        this.openWidgetEditor(action.event, action.entity);
        return true;
      case 'export':
        this.exportWidgetType(action.event, action.entity);
        return true;
    }
    return false;
  }

}
