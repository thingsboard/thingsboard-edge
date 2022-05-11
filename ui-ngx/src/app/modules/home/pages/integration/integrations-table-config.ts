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
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
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
import { Observable } from 'rxjs';
import { isUndefined } from '@core/utils';

export class IntegrationsTableConfig extends EntityTableConfig<Integration> {

  constructor(private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private params: ActivatedRouteSnapshot) {
    super();

    this.entityType = EntityType.INTEGRATION;
    this.entityComponent = IntegrationComponent;
    this.entityTabsComponent = IntegrationTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.INTEGRATION);

    this.componentsData = this.setComponentsData(this.params);

    this.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Integration): string {
        return getIntegrationHelpLink(entity);
      }
    };
    this.addDialogStyle = {width: '800px'};

    this.entityTitle = (integration) => integration ?
      this.utils.customTranslation(integration.name, integration.name) : '';

    this.columns.push(
      new DateEntityTableColumn<Integration>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Integration>('name', 'converter.name', '33%', this.entityTitle),
      new EntityTableColumn<Integration>('type', 'converter.type', '33%', (integration) => {
        return this.translate.instant(integrationTypeInfoMap.get(integration.type).name);
      })
    );

    this.deleteEntityTitle = integration =>
      this.translate.instant('integration.delete-integration-title', { integrationName: integration.name });
    this.deleteEntityContent = () => this.translate.instant('integration.delete-integration-text');
    this.deleteEntitiesTitle = count => this.translate.instant('integration.delete-integrations-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('integration.delete-integrations-text');
    this.entitiesFetchFunction = pageLink => this.integrationService.getIntegrations(pageLink, this.componentsData.isEdgeTemplate);
    this.loadEntity = id => this.integrationService.getIntegration(id.id);
    this.saveEntity = integration => this.saveIntegration(integration);
    this.deleteEntity = id => this.integrationService.deleteIntegration(id.id);

    this.onEntityAction = action => this.onIntegrationAction(action);

    this.tableTitle = this.configureTableTitle(this.componentsData.isEdgeTemplate);
    defaultEntityTablePermissions(this.userPermissionsService, this);
  }

  private setComponentsData(params: any): IntegrationParams {
    return {
      isEdgeTemplate: params.data.isEdgeTemplate ? params.data.isEdgeTemplate : false
    };
  }

  private saveIntegration(integration: Integration): Observable<Integration> {
    if (isUndefined(integration.edgeTemplate)) {
      integration.edgeTemplate = this.componentsData.isEdgeTemplate;
    }
    return this.integrationService.saveIntegration(integration);
  }

  openIntegration($event: Event, integration: Integration) {
    if ($event) {
      $event.stopPropagation();
    }
    let url = this.configureNavigateUrl(this.componentsData.isEdgeTemplate, integration);
    this.router.navigateByUrl(url);
  }

  onIntegrationAction(action: EntityAction<Integration>): boolean {
    switch (action.action) {
      case 'open':
        this.openIntegration(action.event, action.entity);
        return true;
    }
    return false;
  }

  private configureNavigateUrl(isEdgeTemplate: boolean, integration: Integration): UrlTree {
    if (isEdgeTemplate) {
      return this.router.createUrlTree(['edgeManagement/integrations', integration.id.id]);
    } else {
      return this.router.createUrlTree(['integrations', integration.id.id]);
    }
  }

  private configureTableTitle(isEdgeTemplate: boolean): string {
    if (isEdgeTemplate) {
      return this.translate.instant('edge.integration-templates');
    } else {
      return this.translate.instant('integration.integrations');
    }
  }

}
