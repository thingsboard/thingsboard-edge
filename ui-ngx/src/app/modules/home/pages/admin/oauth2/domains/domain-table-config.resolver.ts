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
import { ActivatedRouteSnapshot } from '@angular/router';
import {
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityActionTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { DomainInfo } from '@shared/models/oauth2.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomainService } from '@app/core/http/domain.service';
import { DomainComponent } from '@home/pages/admin/oauth2/domains/domain.component';
import { isEqual } from '@core/utils';
import { DomainTableHeaderComponent } from '@home/pages/admin/oauth2/domains/domain-table-header.component';
import { Direction } from '@app/shared/models/page/sort-order';
import { map, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class DomainTableConfigResolver  {

  private readonly config: EntityTableConfig<DomainInfo> = new EntityTableConfig<DomainInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private domainService: DomainService,
              private userPermissionsService: UserPermissionsService,
              ) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.DOMAIN;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DOMAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.DOMAIN);
    this.config.entityComponent = DomainComponent;
    this.config.headerComponent = DomainTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<DomainInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<DomainInfo>('name', 'admin.oauth2.domain-name', '170px'),
      new EntityChipsEntityTableColumn<DomainInfo>('oauth2ClientInfos', 'admin.oauth2.clients', '40%'),
      new EntityActionTableColumn('oauth2Enabled', 'admin.oauth2.enable',
        {
          name: '',
          nameFunction: (domain) =>
            this.translate.instant(domain.oauth2Enabled ? 'admin.oauth2.disable' : 'admin.oauth2.enable'),
          icon: 'mdi:toggle-switch',
          iconFunction: (domain) => domain.oauth2Enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
          isEnabled: () => true,
          onAction: ($event, entity) => this.toggleEnableOAuth($event, entity)
        }),
      new EntityActionTableColumn('propagateToEdge', 'admin.oauth2.edge',
        {
          name: '',
          nameFunction: (domain) =>
            this.translate.instant(domain.propagateToEdge ? 'admin.oauth2.edge-disable' : 'admin.oauth2.edge-enable'),
          icon: 'mdi:toggle-switch',
          iconFunction: (entity) => entity.propagateToEdge ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
          isEnabled: () => true,
          onAction: ($event, entity) => this.togglePropagateToEdge($event, entity)
        })
    );

    this.config.deleteEntityTitle = (domain) => this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domain.name});
    this.config.deleteEntityContent = () => this.translate.instant('admin.oauth2.delete-domain-text');
    this.config.entitiesFetchFunction = pageLink => this.domainService.getDomainInfos(pageLink);
    this.config.loadEntity = id => this.domainService.getDomainInfoById(id.id);
    this.config.saveEntity = (domain, originalDomain) => {
      const clientsIds = domain.oauth2ClientInfos as Array<string> || [];
      const shouldUpdateClients = domain.id && !isEqual(domain.oauth2ClientInfos?.sort(),
        originalDomain.oauth2ClientInfos?.map(info => info.id ? info.id.id : info).sort());
      delete domain.oauth2ClientInfos;

      return this.domainService.saveDomain(domain, domain.id ? null : clientsIds).pipe(
        switchMap(savedDomain => shouldUpdateClients
          ? this.domainService.updateOauth2Clients(domain.id.id, clientsIds).pipe(map(() => savedDomain))
          : of(savedDomain)
        ),
        map(savedDomain => {
          (savedDomain as DomainInfo).oauth2ClientInfos = clientsIds;
          return savedDomain;
        })
      );
    };
    this.config.deleteEntity = id => this.domainService.deleteDomain(id.id);
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<DomainInfo> {
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private toggleEnableOAuth($event: Event, domain: DomainInfo): void {
    if ($event) {
      $event.stopPropagation();
    }

    const { oauth2ClientInfos, oauth2Enabled, ...updatedDomain } = domain;

    this.domainService.saveDomain({ ...updatedDomain, oauth2Enabled: !oauth2Enabled }, null,
      {ignoreLoading: true})
      .subscribe((result) => {
        domain.oauth2Enabled = result.oauth2Enabled;
        this.config.getTable().detectChanges();
      });
  }

  private togglePropagateToEdge($event: Event, domain: DomainInfo): void {
    if ($event) {
      $event.stopPropagation();
    }

    const { oauth2ClientInfos, propagateToEdge, ...updatedDomain } = domain;

    this.domainService.saveDomain({ ...updatedDomain, propagateToEdge: !propagateToEdge }, null,
      {ignoreLoading: true})
      .subscribe((result) => {
        domain.propagateToEdge = result.propagateToEdge;
        this.config.getTable().detectChanges();
      });
  }

}
