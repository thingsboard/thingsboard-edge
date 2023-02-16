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

import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupDetailsMode, EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Dashboard, DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { GroupDashboardFormComponent } from '@home/pages/dashboard/group-dashboard-form.component';
import { Operation, Resource } from '@shared/models/security.models';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ActivatedRoute, Router, UrlTree } from '@angular/router';
import {
  PublicDashboardLinkDialogComponent,
  PublicDashboardLinkDialogData
} from '@home/pages/dashboard/public-dashboard-link.dialog.component';
import { WINDOW } from '@core/services/window.service';

// @dynamic
@Injectable()
export class DashboardGroupConfigFactory implements EntityGroupStateConfigFactory<Dashboard> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<Dashboard>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private route: ActivatedRoute,
              private router: Router,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private homeDialogs: HomeDialogsService,
              private dashboardService: DashboardService,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<Dashboard>): Observable<GroupEntityTableConfig<Dashboard>> {
    const config = new GroupEntityTableConfig<Dashboard>(entityGroup, params);

    config.entityComponent = GroupDashboardFormComponent;

    config.entityTitle = (dashboard) => dashboard ?
      this.utils.customTranslation(dashboard.title, dashboard.title) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = dashboard => this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    config.loadEntity = id => this.dashboardService.getDashboard(id.id);
    config.saveEntity = dashboard => this.dashboardService.saveDashboard(dashboard);
    config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);

    config.onEntityAction = action => this.onDashboardAction(action, config, params);

    if (config.entityGroup.additionalInfo && config.entityGroup.additionalInfo.isPublic) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('dashboard.public-dashboard-link'),
          icon: 'link',
          isEnabled: () => true,
          onAction: ($event, entity) => {
            this.openPublicDashboardLinkDialog($event, entity, config);
          }
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.WIDGETS_BUNDLE, Operation.READ) &&
      this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.READ)) {
      config.onGroupEntityRowClick = ($event, dashboard) => {
        if (config.isDetailsOpen()) {
          config.onToggleEntityDetails($event, dashboard);
        } else {
          this.openDashboard($event, dashboard, config, params);
        }
      };
    }

    config.cellActionDescriptors.push(
      {
        name: this.translate.instant('dashboard.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => {
          this.exportDashboard($event, entity);
        }
      }
    );

    if (config.settings.detailsMode === EntityGroupDetailsMode.onRowClick &&
        this.userPermissionsService.hasGroupEntityPermission(Operation.READ, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('dashboard.dashboard-details'),
            icon: 'edit',
            isEnabled: () => true,
            onAction: ($event, entity) => config.onToggleEntityDetails($event, entity)
          }
        );
    }

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('dashboard.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDashboard($event, config)
        }
      );
    }
    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  openDashboard($event: Event, dashboard: ShortEntityView | Dashboard, config: GroupEntityTableConfig<Dashboard>,
                params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'dashboardGroups', params.edgeEntitiesGroupId, dashboard.id.id]);
      } else {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId,
          params.customerId, 'dashboardGroups', params.childEntityGroupId, dashboard.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([dashboard.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  exportDashboard($event: Event, dashboard: ShortEntityView | Dashboard) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportDashboard(dashboard.id.id);
  }

  importDashboard($event: Event, config: GroupEntityTableConfig<Dashboard>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.importExport.importDashboard(customerId, entityGroupId).subscribe((res) => {
      if (res) {
        config.updateData();
      }
    });
  }

  openPublicDashboardLinkDialog($event: Event, dashboard: ShortEntityView | DashboardInfo, config: GroupEntityTableConfig<Dashboard>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<PublicDashboardLinkDialogComponent, PublicDashboardLinkDialogData>(
      PublicDashboardLinkDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        dashboard,
        entityGroup: config.entityGroup
      }
    });
  }

  onDashboardAction(action: EntityAction<Dashboard>, config: GroupEntityTableConfig<Dashboard>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity, config, params);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
    }
    return false;
  }

}
