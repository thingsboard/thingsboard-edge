///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@app/core/http/widget.service';
import { WidgetsBundleComponent } from '@modules/home/pages/widget/widgets-bundle.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { Direction } from '@shared/models/page/sort-order';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { WidgetsBundleTabsComponent } from '@home/pages/widget/widgets-bundle-tabs.component';

@Injectable()
export class WidgetsBundlesTableConfigResolver implements Resolve<EntityTableConfig<WidgetsBundle>> {

  private readonly config: EntityTableConfig<WidgetsBundle> = new EntityTableConfig<WidgetsBundle>();

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private widgetsService: WidgetService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService) {

    this.config.entityType = EntityType.WIDGETS_BUNDLE;
    this.config.entityComponent = WidgetsBundleComponent;
    this.config.entityTabsComponent = WidgetsBundleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.WIDGETS_BUNDLE);
    this.config.entityResources = entityTypeResources.get(EntityType.WIDGETS_BUNDLE);
    this.config.defaultSortOrder = {property: 'title', direction: Direction.ASC};

    this.config.rowPointer = true;

    this.config.entityTitle = (widgetsBundle) => widgetsBundle ?
      this.utils.customTranslation(widgetsBundle.title, widgetsBundle.title) : '';

    this.config.columns.push(
      new DateEntityTableColumn<WidgetsBundle>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<WidgetsBundle>('title', 'widgets-bundle.title', '100%', this.config.entityTitle),
      new EntityTableColumn<WidgetsBundle>('tenantId', 'widgets-bundle.system', '60px',
        entity => {
          return checkBoxCell(entity.tenantId.id === NULL_UUID);
        }),
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('widgets-bundle.create-new-widgets-bundle'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('widgets-bundle.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importWidgetsBundle($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('widgets-bundle.export'),
        icon: 'file_download',
        isEnabled: () => userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ),
        onAction: ($event, entity) => this.exportWidgetsBundle($event, entity)
      },
      {
        name: this.translate.instant('widgets-bundle.widgets-bundle-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );

    this.config.deleteEntityTitle = widgetsBundle => this.translate.instant('widgets-bundle.delete-widgets-bundle-title',
      { widgetsBundleTitle: widgetsBundle.title });
    this.config.deleteEntityContent = () => this.translate.instant('widgets-bundle.delete-widgets-bundle-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('widgets-bundle.delete-widgets-bundles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('widgets-bundle.delete-widgets-bundles-text');

    this.config.loadEntity = id => this.widgetsService.getWidgetsBundle(id.id);
    this.config.saveEntity = widgetsBundle => this.widgetsService.saveWidgetsBundle(widgetsBundle);
    this.config.deleteEntity = id => this.widgetsService.deleteWidgetsBundle(id.id);
    this.config.onEntityAction = action => this.onWidgetsBundleAction(action);

  }

  resolve(): EntityTableConfig<WidgetsBundle> {
    this.config.tableTitle = this.translate.instant('widgets-bundle.widgets-bundles');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (widgetsBundle) =>
      this.isWidgetsBundleEditable(widgetsBundle, authUser.authority) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.DELETE);
    this.config.entitySelectionEnabled = (widgetsBundle) =>
      this.isWidgetsBundleEditable(widgetsBundle, authUser.authority) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.DELETE);
    this.config.detailsReadonly = (widgetsBundle) => !this.isWidgetsBundleEditable(widgetsBundle, authUser.authority);
    this.config.entitiesFetchFunction = pageLink => this.widgetsService.getWidgetBundles(pageLink);
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    if (this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ)) {
      this.config.handleRowClick = ($event, widgetsBundle) => {
        if (this.config.isDetailsOpen()) {
          this.config.toggleEntityDetails($event, widgetsBundle);
        } else {
          this.openWidgetsBundle($event, widgetsBundle);
        }
        return true;
      };
    } else {
      this.config.handleRowClick = () => false;
    }
    return this.config;
  }

  isWidgetsBundleEditable(widgetsBundle: WidgetsBundle, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return widgetsBundle && widgetsBundle.tenantId && widgetsBundle.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }

  importWidgetsBundle($event: Event) {
    this.importExport.importWidgetsBundle().subscribe(
      (widgetsBundle) => {
        if (widgetsBundle) {
          this.config.updateData();
        }
      }
    );
  }

  openWidgetsBundle($event: Event, widgetsBundle: WidgetsBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`widgets-bundles/${widgetsBundle.id.id}/widgetTypes`);
  }

  exportWidgetsBundle($event: Event, widgetsBundle: WidgetsBundle) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetsBundle(widgetsBundle.id.id);
  }

  onWidgetsBundleAction(action: EntityAction<WidgetsBundle>): boolean {
    switch (action.action) {
      case 'open':
        this.openWidgetsBundle(action.event, action.entity);
        return true;
      case 'export':
        this.exportWidgetsBundle(action.event, action.entity);
        return true;
    }
    return false;
  }

}
