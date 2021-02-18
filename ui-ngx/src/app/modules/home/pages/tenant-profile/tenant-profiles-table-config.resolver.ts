///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { TenantProfile } from '@shared/models/tenant.model';
import {
  checkBoxCell,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { TenantProfileComponent } from '../../components/profile/tenant-profile.component';
import { TenantProfileTabsComponent } from './tenant-profile-tabs.component';
import { DialogService } from '@core/services/dialog.service';
import { UserPermissionsService } from '../../../../core/http/user-permissions.service';
import { UtilsService } from '../../../../core/services/utils.service';
import { Operation, Resource } from '../../../../shared/models/security.models';

@Injectable()
export class TenantProfilesTableConfigResolver implements Resolve<EntityTableConfig<TenantProfile>> {

  private readonly config: EntityTableConfig<TenantProfile> = new EntityTableConfig<TenantProfile>();

  constructor(private tenantProfileService: TenantProfileService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private utils: UtilsService,
              private userPermissionService: UserPermissionsService) {

    this.config.entityType = EntityType.TENANT_PROFILE;
    this.config.entityComponent = TenantProfileComponent;
    this.config.entityTabsComponent = TenantProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TENANT_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.TENANT_PROFILE);

    this.config.entityTitle = (tenantProfile) => tenantProfile ?
      this.utils.customTranslation(tenantProfile.name, tenantProfile.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<TenantProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<TenantProfile>('name', 'tenant-profile.name', '40%'),
      new EntityTableColumn<TenantProfile>('description', 'tenant-profile.description', '60%'),
      new EntityTableColumn<TenantProfile>('isDefault', 'tenant-profile.default', '60px',
        entity => {
          return checkBoxCell(entity.default);
        })
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('tenant-profile.set-default'),
        icon: 'flag',
        isEnabled: (tenantProfile) => !tenantProfile.default &&
          this.userPermissionService.hasGenericPermission(Resource.TENANT_PROFILE, Operation.WRITE),
        onAction: ($event, entity) => this.setDefaultTenantProfile($event, entity)
      }
    );

    this.config.deleteEntityTitle = tenantProfile => this.translate.instant('tenant-profile.delete-tenant-profile-title',
      { tenantProfileName: tenantProfile.name });
    this.config.deleteEntityContent = () => this.translate.instant('tenant-profile.delete-tenant-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('tenant-profile.delete-tenant-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('tenant-profile.delete-tenant-profiles-text');

    this.config.entitiesFetchFunction = pageLink => this.tenantProfileService.getTenantProfiles(pageLink);
    this.config.loadEntity = id => this.tenantProfileService.getTenantProfile(id.id);
    this.config.saveEntity = tenantProfile => this.tenantProfileService.saveTenantProfile(tenantProfile);
    this.config.deleteEntity = id => this.tenantProfileService.deleteTenantProfile(id.id);
    this.config.onEntityAction = action => this.onTenantProfileAction(action);
    this.config.deleteEnabled = (tenantProfile) => tenantProfile && !tenantProfile.default;
    this.config.entitySelectionEnabled = (tenantProfile) => tenantProfile && !tenantProfile.default;
  }

  resolve(): EntityTableConfig<TenantProfile> {
    this.config.tableTitle = this.translate.instant('tenant-profile.tenant-profiles');
    defaultEntityTablePermissions(this.userPermissionService, this.config);
    return this.config;
  }

  setDefaultTenantProfile($event: Event, tenantProfile: TenantProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('tenant-profile.set-default-tenant-profile-title', {tenantProfileName: tenantProfile.name}),
      this.translate.instant('tenant-profile.set-default-tenant-profile-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.tenantProfileService.setDefaultTenantProfile(tenantProfile.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  onTenantProfileAction(action: EntityAction<TenantProfile>): boolean {
    switch (action.action) {
      case 'setDefault':
        this.setDefaultTenantProfile(action.event, action.entity);
        return true;
    }
    return false;
  }

}
