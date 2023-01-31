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

import { Injectable } from '@angular/core';

import { Resolve, Router } from '@angular/router';

import { TenantInfo } from '@shared/models/tenant.model';
import {
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TenantService } from '@core/http/tenant.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TenantComponent } from '@modules/home/pages/tenant/tenant.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { TenantTabsComponent } from '@home/pages/tenant/tenant-tabs.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UtilsService } from '@core/services/utils.service';
import { mergeMap } from 'rxjs/operators';

@Injectable()
export class TenantsTableConfigResolver implements Resolve<EntityTableConfig<TenantInfo>> {

  private readonly config: EntityTableConfig<TenantInfo> = new EntityTableConfig<TenantInfo>();

  constructor(private tenantService: TenantService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private userPermissionService: UserPermissionsService) {

    this.config.entityType = EntityType.TENANT;
    this.config.entityComponent = TenantComponent;
    this.config.entityTabsComponent = TenantTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TENANT);
    this.config.entityResources = entityTypeResources.get(EntityType.TENANT);

    this.config.entityTitle = (tenant) => tenant ?
      this.utils.customTranslation(tenant.title, tenant.title) : '';

    this.config.columns.push(
      new DateEntityTableColumn<TenantInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<TenantInfo>('title', 'tenant.title', '20%', this.config.entityTitle),
      new EntityTableColumn<TenantInfo>('tenantProfileName', 'tenant-profile.tenant-profile', '20%'),
      new EntityTableColumn<TenantInfo>('email', 'contact.email', '20%'),
      new EntityTableColumn<TenantInfo>('country', 'contact.country', '20%'),
      new EntityTableColumn<TenantInfo>('city', 'contact.city', '20%')
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('tenant.manage-tenant-admins'),
        icon: 'account_circle',
        isEnabled: () => this.userPermissionService.hasGenericPermission(Resource.USER, Operation.READ),
        onAction: ($event, entity) => this.manageTenantAdmins($event, entity)
      }
    );

    this.config.deleteEntityTitle = tenant => this.translate.instant('tenant.delete-tenant-title', { tenantTitle: tenant.title });
    this.config.deleteEntityContent = () => this.translate.instant('tenant.delete-tenant-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('tenant.delete-tenants-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('tenant.delete-tenants-text');
    this.config.entitiesFetchFunction = pageLink => this.tenantService.getTenantInfos(pageLink);
    this.config.loadEntity = id => this.tenantService.getTenantInfo(id.id);
    this.config.saveEntity = tenant => this.tenantService.saveTenant(tenant).pipe(
      mergeMap((savedTenant) => this.tenantService.getTenantInfo(savedTenant.id.id))
    );
    this.config.deleteEntity = id => this.tenantService.deleteTenant(id.id);
    this.config.onEntityAction = action => this.onTenantAction(action, this.config);
  }

  resolve(): EntityTableConfig<TenantInfo> {
    this.config.tableTitle = this.translate.instant('tenant.tenants');
    defaultEntityTablePermissions(this.userPermissionService, this.config);
    return this.config;
  }

  private openTenant($event: Event, tenant: TenantInfo, config: EntityTableConfig<TenantInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([tenant.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  manageTenantAdmins($event: Event, tenant: TenantInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`tenants/${tenant.id.id}/users`);
  }

  onTenantAction(action: EntityAction<TenantInfo>, config: EntityTableConfig<TenantInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openTenant(action.event, action.entity, config);
        return true;
      case 'manageTenantAdmins':
        this.manageTenantAdmins(action.event, action.entity);
        return true;
    }
    return false;
  }

}
