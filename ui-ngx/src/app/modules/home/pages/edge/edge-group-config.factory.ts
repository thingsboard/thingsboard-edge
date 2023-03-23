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
import { tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Operation, Resource } from '@shared/models/security.models';
import { Edge, EdgeInstallInstructions } from '@shared/models/edge.models';
import { EdgeService } from '@core/http/edge.service';
import { EdgeComponent } from '@home/pages/edge/edge.component';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { WINDOW } from '@core/services/window.service';
import {
  EdgeInstructionsData,
  EdgeInstructionsDialogComponent
} from '@home/pages/edge/edge-instructions-dialog.component';
import { Customer } from '@shared/models/customer.model';

@Injectable()
export class EdgeGroupConfigFactory implements EntityGroupStateConfigFactory<Edge> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<Edge>,
              private userPermissionsService: UserPermissionsService,
              private store: Store<AppState>,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private edgeService: EdgeService,
              private broadcast: BroadcastService,
              private router: Router,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<Edge>): Observable<GroupEntityTableConfig<Edge>> {
    const config = new GroupEntityTableConfig<Edge>(entityGroup, params);

    const authUser: AuthUser = getCurrentAuthUser(this.store);

    config.entityComponent = EdgeComponent;

    config.entityTitle = (edge) => edge ?
      this.utils.customTranslation(edge.name, edge.name) : '';

    config.deleteEntityTitle = edge => this.translate.instant('edge.delete-edge-title', { edgeName: edge.name });
    config.deleteEntityContent = () => this.translate.instant('edge.delete-edge-text');
    config.deleteEntitiesTitle = count => this.translate.instant('edge.delete-edges-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('edge.delete-edges-text');

    config.loadEntity = id => this.edgeService.getEdge(id.id);
    config.saveEntity = edge => {
      return this.edgeService.saveEdge(edge).pipe(
        tap(() => {
          this.broadcast.broadcast('edgeSaved');
        }));
    };
    config.deleteEntity = id => this.edgeService.deleteEdge(id.id);

    config.onEntityAction = action => this.onEdgeAction(action, config, params);

    if (params.hierarchyView) {
      config.entityAdded = edge => {
        params.hierarchyCallbacks.edgeAdded(params.nodeId, edge);
      };
      config.entityUpdated = edge => {
        params.hierarchyCallbacks.edgeUpdated(edge);
      };
      config.entitiesDeleted = edgeIds => {
        params.hierarchyCallbacks.edgesDeleted(edgeIds.map(id => id.id));
      };
    }

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('edge.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importEdges($event, config)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.USER_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-user-groups'),
          icon: 'account_circle',
          isEnabled: config.manageUsersEnabled,
          onAction: ($event, entity) => this.manageUsers($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-asset-groups'),
          icon: 'domain',
          isEnabled: config.manageAssetsEnabled,
          onAction: ($event, entity) => this.manageAssets($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-device-groups'),
          icon: 'devices_other',
          isEnabled: config.manageDevicesEnabled,
          onAction: ($event, entity) => this.manageDevices($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-entity-view-groups'),
          icon: 'view_quilt',
          isEnabled: config.manageEntityViewsEnabled,
          onAction: ($event, entity) => this.manageEntityViews($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD_GROUP, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-dashboard-groups'),
          icon: 'dashboard',
          isEnabled: config.manageDashboardsEnabled,
          onAction: ($event, entity) => this.manageDashboards($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-scheduler-events'),
          icon: 'schedule',
          isEnabled: config.manageSchedulerEventsEnabled,
          onAction: ($event, entity) => this.manageSchedulerEvents($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.READ) &&
      authUser.authority === Authority.TENANT_ADMIN) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-rule-chains'),
            icon: 'settings_ethernet',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageRuleChains($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.INTEGRATION, Operation.READ) &&
      authUser.authority === Authority.TENANT_ADMIN) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.manage-edge-integrations'),
          icon: 'input',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageIntegrations($event, entity, config, params)
        }
      );
    }

    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  importEdges($event: Event, config: GroupEntityTableConfig<Edge>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.EDGE, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('edgeSaved');
        config.updateData();
      }
    });
  }

  onEdgeAction(action: EntityAction<Edge>, config: GroupEntityTableConfig<Edge>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openEdge(action.event, action.entity, config, params);
        return true;
      case 'manageUsers':
        this.manageUsers(action.event, action.entity, config, params);
        return true;
      case 'manageAssets':
        this.manageAssets(action.event, action.entity, config, params);
        return true;
      case 'manageDevices':
        this.manageDevices(action.event, action.entity, config, params);
        return true;
      case 'manageEntityViews':
        this.manageEntityViews(action.event, action.entity, config, params);
        return true;
      case 'manageDashboards':
        this.manageDashboards(action.event, action.entity, config, params);
        return true;
      case 'manageSchedulerEvents':
        this.manageSchedulerEvents(action.event, action.entity, config, params);
        return true;
      case 'manageRuleChains':
        this.manageRuleChains(action.event, action.entity, config, params);
        return true;
      case 'manageIntegrations':
        this.manageIntegrations(action.event, action.entity, config, params);
        return true;
      case 'syncEdge':
        this.syncEdge(action.event, action.entity);
        return true;
      case 'openInstructions':
        this.openInstructions(action.event, action.entity);
        return true;
    }
    return false;
  }

  private openEdge($event: Event, edge: Edge,  config: GroupEntityTableConfig<Edge>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      const url = this.router.createUrlTree(['customerGroups', params.entityGroupId,
          params.customerId, 'edgeGroups', params.childEntityGroupId, edge.id.id]);
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([edge.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  manageUsers($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
              params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.USER);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/userGroups');
    }
  }

  manageAssets($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
               params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.ASSET);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/assetGroups');
    }
  }

  manageDevices($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.DEVICE);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/deviceGroups');
    }
  }

  manageEntityViews($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                    params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.ENTITY_VIEW);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/entityViewGroups');
    }
  }

  manageDashboards($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                   params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.DASHBOARD);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/dashboardGroups');
    }
  }

  manageSchedulerEvents($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                        params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.SCHEDULER_EVENT);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/scheduler');
    }
  }

  manageRuleChains($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                   params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.RULE_CHAIN);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/ruleChains');
    }
  }

  manageIntegrations($event: Event, edge: Edge | ShortEntityView, config: GroupEntityTableConfig<Edge>,
                     params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.edgeGroupsSelected(params.nodeId, edge.id.id, EntityType.INTEGRATION);
    } else  {
      this.navigateToChildEdgePage(config, edge, '/integrations');
    }
  }

  private navigateToChildEdgePage(config: GroupEntityTableConfig<Edge>, edge: Edge | ShortEntityView, page: string) {
    if (this.isCustomerScope(config.groupParams)) {
      if (config.groupParams.childEntityGroupId) {
        const targetGroups = config.groupParams.shared ? 'shared' : 'groups';
        this.router.navigateByUrl(`customers/${targetGroups}/${config.groupParams.entityGroupId}/${config.groupParams.customerId}` +
      `/edgeManagement/instances/groups/${config.groupParams.childEntityGroupId}/${edge.id.id}${page}`);
      } else {
        this.router.navigateByUrl(`customers/all/${config.groupParams.customerId}` +
      `/edgeManagement/instances/groups/${config.groupParams.entityGroupId}/${edge.id.id}${page}`);
      }
    } else {
      const targetGroups = config.groupParams.shared ? 'shared' : 'groups';
      this.router.navigateByUrl(`edgeManagement/instances/${targetGroups}/${config.entityGroup.id.id}/${edge.id.id}${page}`);
    }
  }

  syncEdge($event, edge) {
    if ($event) {
      $event.stopPropagation();
    }
    this.edgeService.syncEdge(edge.id.id).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.translate.instant('edge.sync-process-started-successfully'),
            type: 'success',
            duration: 750,
            verticalPosition: 'bottom',
            horizontalPosition: 'right'
          }));
      }
    );
  }

  openInstructions($event, edge) {
    if ($event) {
      $event.stopPropagation();
    }
    this.edgeService.getEdgeDockerInstallInstructions(edge.id.id).subscribe(
      (edgeInstructionsTemplate: EdgeInstallInstructions) => {
        this.dialog.open<EdgeInstructionsDialogComponent, EdgeInstructionsData>(EdgeInstructionsDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            instructions: edgeInstructionsTemplate.dockerInstallInstructions
          }
        });
      }
    );
  }

  private isCustomerScope(params: EntityGroupParams): boolean {
    return params.childGroupType === EntityType.EDGE && params.groupType === EntityType.CUSTOMER;
  }

}
