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

import { ActivatedRouteSnapshot, Resolve, Router, UrlTree } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  getIntegrationHelpLink,
  Integration,
  IntegrationParams,
  integrationTypeInfoMap,
  resolveIntegrationParams
} from '@shared/models/integration.models';
import { IntegrationService } from '@core/http/integration.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EdgeService } from '@core/http/edge.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { IntegrationComponent } from '@home/pages/integration/integration.component';
import { IntegrationTabsComponent } from '@home/pages/integration/integration-tabs.component';
import { Operation, Resource } from '@shared/models/security.models';
import { forkJoin, Observable } from 'rxjs';
import { isUndefined } from '@core/utils';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { PageData } from '@shared/models/page/page-data';
import { Edge } from '@shared/models/edge.models';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { map } from 'rxjs/operators';

@Injectable()
export class IntegrationsTableConfigResolver implements Resolve<EntityTableConfig<Integration>> {

  private readonly config: EntityTableConfig<Integration> = new EntityTableConfig<Integration>();

  constructor(private store: Store<AppState>,
              private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private dialogService: DialogService,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.INTEGRATION;
    this.config.entityComponent = IntegrationComponent;
    this.config.entityTabsComponent = IntegrationTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.INTEGRATION);
    this.config.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Integration): string {
        return getIntegrationHelpLink(entity);
      }
    };
    this.config.addDialogStyle = {width: '800px'};

    this.config.deleteEntityTitle = integration =>
      this.translate.instant('integration.delete-integration-title', { integrationName: integration.name });
    this.config.deleteEntityContent = () => this.translate.instant('integration.delete-integration-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('integration.delete-integrations-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('integration.delete-integrations-text');
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<Integration> {
    return this.resolveIntegrationsTableConfig(resolveIntegrationParams(route));
  }

  resolveIntegrationsTableConfig(params: IntegrationParams): EntityTableConfig<Integration> {
    this.config.tableTitle = this.translate.instant('integration.integrations');
    this.config.componentsData = params;

    this.config.columns = this.configureEntityTableColumns();
    this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.integrationScope);
    this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.integrationScope);
    this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData);

    this.config.loadEntity = id => this.integrationService.getIntegration(id.id);
    this.config.entitiesFetchFunction = this.configureEntityFunctions(this.config.componentsData.integrationScope, this.config.componentsData.edgeId);
    this.config.saveEntity = integration => this.saveIntegration(integration);
    this.config.deleteEntity = id => this.integrationService.deleteIntegration(id.id);

    this.config.onEntityAction = action => this.onIntegrationAction(action, this.config.componentsData);

    this.configureIntegrationScope();

    // TODO: @voba - maybe this line is not required, if fix correctly applied on detect changes issue
    // customers-hierarchy.component.ts
    // private updateIntegrations(integrationParams: IntegrationParams) {
    // 385 line
    this.config.updateData(true);

    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private configureEntityTableColumns(): Array<EntityColumn<Integration>> {
    const columns: Array<EntityColumn<Integration>> = [];

    this.config.entityTitle = (integration) => integration ?
      this.utils.customTranslation(integration.name, integration.name) : '';

    columns.push(
      new DateEntityTableColumn<Integration>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Integration>('name', 'converter.name', '33%', this.config.entityTitle),
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
          onAction: ($event) => this.config.getTable().addEntity($event)
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
      if (this.config.componentsData.integrationScope === 'tenant') {
        integration.edgeTemplate = false;
      } else if (this.config.componentsData.integrationScope === 'edges') {
        integration.edgeTemplate = true;
      } else {
        // safe fallback to default
        integration.edgeTemplate = false;
      }
    }
    return this.integrationService.saveIntegration(integration).pipe(
      map((integration) => {
        if (this.config.componentsData.integrationScope === 'edges') {
          this.edgeService.findAllRelatedEdgesMissingAttributes(integration.id.id).subscribe(
            (missingEdgeAttributes) => {
              if (missingEdgeAttributes && Object.keys(missingEdgeAttributes).length > 0) {
                const formattedMissingEdgeAttributes: Array<string> = new Array<string>();
                for (const missingEdgeAttribute of Object.keys(missingEdgeAttributes)) {
                  const arrayOfMissingAttributes = missingEdgeAttributes[missingEdgeAttribute];
                  const tmp = '- \'' + missingEdgeAttribute + '\': \'' + arrayOfMissingAttributes.join('\', \'') + '\'';
                  formattedMissingEdgeAttributes.push(tmp);
                }
                const message = this.translate.instant('edge.missing-all-related-attributes-text',
                  {missingEdgeAttributes: formattedMissingEdgeAttributes.join('<br>')});
                this.dialogService.alert(this.translate.instant('edge.missing-all-related-attributes-title'),
                  message, this.translate.instant('action.close'), true);
              }
            }
          );
        }
        return integration;
      })
    );
  }

  openIntegration($event: Event, integration: Integration, params?:IntegrationParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.config.componentsData.integrationScope === 'edge') {
      let url: UrlTree;
      if (params && params.hierarchyView) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'integrations', integration.id.id]);
        window.open(window.location.origin + url, '_blank');
      } else {
        url = this.router.createUrlTree([integration.id.id], { relativeTo: this.config.getActivatedRoute() });
        this.router.navigateByUrl(url);
      }
    } else if (this.config.componentsData.integrationScope === 'edges') {
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
      return pageLink => this.integrationService.getIntegrationsByEdgeTemplate(pageLink, false);
    } else if (integrationScope === 'edges') {
      return pageLink => this.integrationService.getIntegrationsByEdgeTemplate(pageLink, true);
    } else if (integrationScope === 'edge') {
      return pageLink => this.integrationService.getEdgeIntegrations(edgeId, pageLink);
    }
  }

  private configureIntegrationScope(): void {
    if (this.config.componentsData.integrationScope === 'tenant' || this.config.componentsData.integrationScope === 'edges') {
      this.config.addEnabled = true;
      this.config.entitiesDeleteEnabled = true;
      this.config.deleteEnabled = () => true;
      this.config.detailsReadonly = () => false;
      this.config.headerActionDescriptors = [];
      this.config.tableTitle = this.configureTableTitle(this.config.componentsData.integrationScope, null);
    } else if (this.config.componentsData.integrationScope === 'edge') {
      this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(edge => {
        this.config.componentsData.edge = edge;
        this.config.tableTitle = this.configureTableTitle(this.config.componentsData.integrationScope, edge);
      });
      this.config.addEnabled = false;
      this.config.entitiesDeleteEnabled = false;
      this.config.deleteEnabled = () => false;
      this.config.detailsReadonly = () => true;
      this.config.headerActionDescriptors = [];
      if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE)) {
        this.config.headerActionDescriptors.push({
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
      return this.config.tableTitle = edge.name + ': ' + this.translate.instant('edge.integrations');
    }
  }

  private assignIntegrationsToEdge($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      Array<string>>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.INTEGRATION
      }
    }).afterClosed()
      .subscribe((res) => {
          if (res) {
            this.edgeService.findEdgeMissingAttributes(res, this.config.componentsData.edgeId).subscribe(
              (missingEdgeAttributes) => {
                if (missingEdgeAttributes && Object.keys(missingEdgeAttributes).length > 0) {
                  const formattedMissingEdgeAttributes: Array<string> = new Array<string>();
                  for (const missingEdgeAttribute of Object.keys(missingEdgeAttributes)) {
                    const arrayOfMissingAttributes = missingEdgeAttributes[missingEdgeAttribute];
                    const tmp = '- \'' + missingEdgeAttribute + '\': \'' + arrayOfMissingAttributes.join('\', \'') + '\'';
                    formattedMissingEdgeAttributes.push(tmp);
                  }
                  const message = this.translate.instant('edge.missing-attributes-text',
                    {missingEdgeAttributes: formattedMissingEdgeAttributes.join('<br>')});
                  this.dialogService.alert(this.translate.instant('edge.missing-attributes-title'),
                    message, this.translate.instant('action.close'), true).subscribe(
                    () => {
                      this.config.updateData();
                    }
                  );
                } else {
                  this.config.updateData();
                }
              }
            );
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
          this.integrationService.unassignIntegrationFromEdge(this.config.componentsData.edgeId, integration.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.integrationScope !== 'tenant');
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
              tasks.push(this.integrationService.unassignIntegrationFromEdge(this.config.componentsData.edgeId, integration.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

}
