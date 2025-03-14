///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import {
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { OAuth2Client, OAuth2ClientInfo, platformTypeTranslations } from '@shared/models/oauth2.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { OAuth2Service } from '@core/http/oauth2.service';
import { ClientComponent } from '@home/pages/admin/oauth2/clients/client.component';
import { ClientTableHeaderComponent } from '@home/pages/admin/oauth2/clients/client-table-header.component';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class ClientsTableConfigResolver  {

  private readonly config: EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo> =
    new EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private oauth2Service: OAuth2Service,
              private userPermissionsService: UserPermissionsService,
              ) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.OAUTH2_CLIENT;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.OAUTH2_CLIENT);
    this.config.entityResources = entityTypeResources.get(EntityType.OAUTH2_CLIENT);
    this.config.entityComponent = ClientComponent;
    this.config.headerComponent = ClientTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<OAuth2ClientInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<OAuth2ClientInfo>('title', 'admin.oauth2.title', '350px'),
      new EntityTableColumn<OAuth2ClientInfo>('platforms', 'admin.oauth2.allowed-platforms', '100%',
        (clientInfo) => clientInfo.platforms && clientInfo.platforms.length ?
          clientInfo.platforms.map(platform => this.translate.instant(platformTypeTranslations.get(platform))).join(', ') :
          this.translate.instant('admin.oauth2.all-platforms'), () => ({}), false)
    );

    this.config.deleteEntityTitle = (client) => this.translate.instant('admin.oauth2.delete-client-title', {clientName: client.title});
    this.config.deleteEntityContent = () => this.translate.instant('admin.oauth2.delete-client-text');
    this.config.entitiesFetchFunction = pageLink => this.oauth2Service.findTenantOAuth2ClientInfos(pageLink);
    this.config.loadEntity = id => this.oauth2Service.getOAuth2ClientById(id.id);
    this.config.saveEntity = client => this.oauth2Service.saveOAuth2Client(client);
    this.config.deleteEntity = id => this.oauth2Service.deleteOauth2Client(id.id);

    // edge-only: allow to read-only
    this.config.detailsReadonly = () => true;
    this.config.deleteEnabled = () => false;
    this.config.addEnabled = false;
    this.config.entitiesDeleteEnabled = false;
  }

  resolve(): EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo> {
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

}
