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

import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import {
  CellActionDescriptor, checkBoxCell,
  DateEntityTableColumn,
  defaultEntityTablePermissions, EntityColumn,
  EntityTableColumn,
  EntityTableConfig, GroupActionDescriptor, HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  getIntegrationHelpLink,
  Integration,
  IntegrationParams,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { IntegrationService } from '@core/http/integration.service';
import { IntegrationComponent } from '@home/pages/integration/integration.component';
import { IntegrationTabsComponent } from '@home/pages/integration/integration-tabs.component';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { forkJoin, Observable } from 'rxjs';
import { isUndefined } from '@core/utils';
import { PageData } from '@shared/models/page/page-data';
import { Edge } from '@shared/models/edge.models';
import { Operation, Resource } from '@shared/models/security.models';
import { EdgeService } from '@core/http/edge.service';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';

export class IntegrationsTableConfig extends EntityTableConfig<Integration> {

  constructor(private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private params: ActivatedRouteSnapshot | IntegrationParams) {
    super();

    this.entityType = EntityType.INTEGRATION;
    this.entityComponent = IntegrationComponent;
    this.entityTabsComponent = IntegrationTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.INTEGRATION);

    this.componentsData = this.setComponentsData(this.params);
    this.columns = this.configureEntityTableColumns();
    this.groupActionDescriptors = this.configureGroupActions(this.componentsData.integrationScope);
    this.addActionDescriptors = this.configureAddActions(this.componentsData.integrationScope);
    this.cellActionDescriptors = this.configureCellActions(this.componentsData);

    this.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Integration): string {
        return getIntegrationHelpLink(entity);
      }
    };
    this.addDialogStyle = {width: '800px'};


    this.deleteEntityTitle = integration =>
      this.translate.instant('integration.delete-integration-title', { integrationName: integration.name });
    this.deleteEntityContent = () => this.translate.instant('integration.delete-integration-text');
    this.deleteEntitiesTitle = count => this.translate.instant('integration.delete-integrations-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('integration.delete-integrations-text');
    this.entitiesFetchFunction = this.configureEntityFunctions(this.componentsData.integrationScope, this.componentsData.edgeId);
    this.loadEntity = id => this.integrationService.getIntegration(id.id);
    this.saveEntity = integration => this.saveIntegration(integration);
    this.deleteEntity = id => this.integrationService.deleteIntegration(id.id);

    this.onEntityAction = action => this.onIntegrationAction(action, this.componentsData);

    this.configureIntegrationScope();

    defaultEntityTablePermissions(this.userPermissionsService, this);
  }

  private setComponentsData(params: any): IntegrationParams {
    let edgeId: string;
    let integrationScope: string;
    if (params?.hierarchyView) {
      edgeId = params.edgeId;
      integrationScope = params.integrationScope;
    } else {
      edgeId = params.params?.edgeId;
      integrationScope = params.data.integrationsType ? params.data.integrationsType : 'tenant';
    }
    return {
      edgeId,
      integrationScope,
      hierarchyView: params?.hierarchyView,
      entityGroupId: params?.entityGroupId,
      customerGroupId: params?.customerGroupId,
      customerId: params?.customerId
    };
  }

  private configureEntityTableColumns(): Array<EntityColumn<Integration>> {
    const columns: Array<EntityColumn<Integration>> = [];

    this.entityTitle = (integration) => integration ?
      this.utils.customTranslation(integration.name, integration.name) : '';

    columns.push(
      new DateEntityTableColumn<Integration>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Integration>('name', 'converter.name', '33%', this.entityTitle),
      new EntityTableColumn<Integration>('type', 'converter.type', '33%', (integration) => {
        return this.translate.instant(integrationTypeInfoMap.get(integration.type).name);
      })
    );
    return columns;
  }

  private configureGroupActions(integrationScope: string): Array<GroupActionDescriptor<Integration>> {
    const actions: Array<GroupActionDescriptor<Integration>> = [];
    if (integrationScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('integration.unassign-integrations'),
          icon: 'assignment_return',
          isEnabled: this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entities) => this.unassignIntegrationsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  private configureAddActions(integrationScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (integrationScope === 'tenant' || integrationScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('integration.add'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.getTable().addEntity($event)
        }
      );
    }
    if (integrationScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.assign-to-edge'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.assignIntegrationsToEdge($event)
        }
      );
    }
    return actions;
  }

  private configureCellActions(params: IntegrationParams): Array<CellActionDescriptor<Integration>> {
    const actions: Array<CellActionDescriptor<Integration>> = [];
    if (params.integrationScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: () => true,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    return actions;
  }

  private saveIntegration(integration: Integration): Observable<Integration> {
    if (isUndefined(integration.edgeTemplate)) {
      if (this.componentsData.integrationScope === 'tenant') {
        integration.edgeTemplate = false;
      } else if (this.componentsData.integrationScope === 'edges') {
        integration.edgeTemplate = true;
      } else {
        // safe fallback to default
        integration.edgeTemplate = false;
      }
    }
    return this.integrationService.saveIntegration(integration);
  }

  openIntegration($event: Event, integration: Integration, params?:IntegrationParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.componentsData.integrationScope === 'edge') {
      let url: UrlTree;
      if (params && params.hierarchyView) {
        url = this.router.createUrlTree(['customerGroups', params.customerGroupId, params.customerId,
          'edgeGroups', params.entityGroupId, params.edgeId, 'integrations', integration.id.id]);
        window.open(window.location.origin + url, '_blank');
      } else {
        url = this.router.createUrlTree([integration.id.id], { relativeTo: this.getActivatedRoute() });
        this.router.navigateByUrl(url);
      }
    } else if (this.componentsData.integrationScope === 'edges') {
      this.router.navigateByUrl(`edgeManagement/integrations/${integration.id.id}`);
    } else {
      this.router.navigateByUrl(`integrations/${integration.id.id}`);
    }
  }

  onIntegrationAction(action: EntityAction<Integration>, params: IntegrationParams): boolean {
    switch (action.action) {
      case 'open':
        this.openIntegration(action.event, action.entity, params);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
    }
    return false;
  }

  private configureEntityFunctions(integrationScope: string, edgeId: string): (pageLink) => Observable<PageData<Integration>> {
    if (integrationScope === 'tenant') {
      return pageLink => this.integrationService.getIntegrations(pageLink, false);
    } else if (integrationScope === 'edges') {
      return pageLink => this.integrationService.getIntegrations(pageLink, true);
    } else if (integrationScope === 'edge') {
      return pageLink => this.integrationService.getEdgeIntegrations(edgeId, pageLink);
    }
  }

  private configureIntegrationScope(): void {
    if (this.componentsData.integrationScope === 'tenant' || this.componentsData.integrationScope === 'edges') {
      this.tableTitle = this.configureTableTitle(this.componentsData.integrationScope, null);
    } else if (this.componentsData.integrationScope === 'edge') {
      this.edgeService.getEdge(this.componentsData.edgeId).subscribe(edge => {
        this.componentsData.edge = edge;
        this.tableTitle = this.configureTableTitle(this.componentsData.integrationScope, edge);
      });
      this.addEnabled = false;
      this.entitiesDeleteEnabled = false;
      this.deleteEnabled = () => false;
      this.detailsReadonly = () => true;
      if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE)) {
        this.headerActionDescriptors.push({
            name: this.translate.instant('edge.assign-to-edge'),
            icon: 'add',
            isEnabled: () => true,
            onAction: ($event) => {
              this.assignIntegrationsToEdge($event);
            }
          }
        );
      }
    }
  }

  private configureTableTitle(integrationScope: string, edge: Edge): string {
    if (integrationScope === 'tenant') {
      return this.translate.instant('integration.integrations');
    } else if (integrationScope === 'edges') {
      return this.translate.instant('edge.integration-templates');
    } else if (integrationScope === 'edge') {
      return this.tableTitle = edge.name + ': ' + this.translate.instant('edge.integrations');
    }
  }

  private assignIntegrationsToEdge($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.componentsData.edgeId,
        entityType: EntityType.INTEGRATION
      }
    }).afterClosed()
      .subscribe((res) => {
          if (res) {
            this.updateData();
          }
        }
      );
  }

  private unassignFromEdge($event: Event, integration: Integration): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('integration.unassign-integration-title', {integrationName: integration.name}),
      this.translate.instant('integration.unassign-integration-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.integrationService.unassignIntegrationFromEdge(this.componentsData.edgeId, integration.id.id).subscribe(
            () => {
              this.updateData(this.componentsData.integrationScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  private unassignIntegrationsFromEdge($event: Event, integrations: Array<Integration>): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('integration.unassign-integrations-from-edge-title', {count: integrations.length}),
      this.translate.instant('integration.unassign-integrations-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          integrations.forEach(
            (integration) => {
              tasks.push(this.integrationService.unassignIntegrationFromEdge(this.componentsData.edgeId, integration.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.updateData();
            }
          );
        }
      }
    );
  }

}
