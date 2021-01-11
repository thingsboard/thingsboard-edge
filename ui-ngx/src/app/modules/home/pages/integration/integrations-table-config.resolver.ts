///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';

import { Resolve } from '@angular/router';
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
import { getIntegrationHelpLink, Integration, integrationTypeInfoMap } from '@shared/models/integration.models';
import { IntegrationService } from '@core/http/integration.service';
import { IntegrationComponent } from '@home/pages/integration/integration.component';
import { IntegrationTabsComponent } from '@home/pages/integration/integration-tabs.component';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class IntegrationsTableConfigResolver implements Resolve<EntityTableConfig<Integration>> {

  private readonly config: EntityTableConfig<Integration> = new EntityTableConfig<Integration>();

  constructor(private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService) {

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

    this.config.entityTitle = (integration) => integration ?
      this.utils.customTranslation(integration.name, integration.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<Integration>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Integration>('name', 'converter.name', '33%', this.config.entityTitle),
      new EntityTableColumn<Integration>('type', 'converter.type', '33%', (integration) => {
        return this.translate.instant(integrationTypeInfoMap.get(integration.type).name)
      })
    );

    this.config.deleteEntityTitle = integration =>
      this.translate.instant('integration.delete-integration-title', { integrationName: integration.name });
    this.config.deleteEntityContent = () => this.translate.instant('integration.delete-integration-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('integration.delete-integrations-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('integration.delete-integrations-text');
    this.config.entitiesFetchFunction = pageLink => this.integrationService.getIntegrations(pageLink);
    this.config.loadEntity = id => this.integrationService.getIntegration(id.id);
    this.config.saveEntity = integration => this.integrationService.saveIntegration(integration);
    this.config.deleteEntity = id => this.integrationService.deleteIntegration(id.id);

    this.config.onEntityAction = action => this.onIntegrationAction(action);
  }

  resolve(): EntityTableConfig<Integration> {
    this.config.tableTitle = this.translate.instant('integration.integrations');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  onIntegrationAction(action: EntityAction<Integration>): boolean {
    return false;
  }

}
