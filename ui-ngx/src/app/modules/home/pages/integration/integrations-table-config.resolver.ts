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

import { Injectable } from '@angular/core';

import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
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

@Injectable()
export class IntegrationsTableConfigResolver implements Resolve<EntityTableConfig<Integration>> {

  private readonly config: EntityTableConfig<Integration> = new EntityTableConfig<Integration>();

  constructor(private integrationService: IntegrationService,
              private translate: TranslateService,
              private datePipe: DatePipe) {

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

    this.config.columns.push(
      new DateEntityTableColumn<Integration>('createdTime', 'integration.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Integration>('name', 'converter.name', '33%'),
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

    return this.config;
  }

  onIntegrationAction(action: EntityAction<Integration>): boolean {
    return false;
  }

}
