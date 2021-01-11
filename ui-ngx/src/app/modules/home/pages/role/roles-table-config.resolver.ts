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
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Role } from '@shared/models/role.models';
import { RoleService } from '@core/http/role.service';
import { RoleComponent } from '@home/pages/role/role.component';
import { RoleTabsComponent } from '@home/pages/role/role-tabs.component';
import { roleTypeTranslationMap } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class RolesTableConfigResolver implements Resolve<EntityTableConfig<Role>> {

  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(private roleService: RoleService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.ROLE;
    this.config.entityComponent = RoleComponent;
    this.config.entityTabsComponent = RoleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);

    this.config.addDialogStyle = {width: '800px'};

    this.config.entityTitle = (role) => role ? role.name : '';

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Role>('name', 'role.name', '25%', this.config.entityTitle),
      new EntityTableColumn<Role>('type', 'role.role-type', '25%', (role) => {
        return this.translate.instant(roleTypeTranslationMap.get(role.type))
      }),
      new EntityTableColumn<Role>('description', 'role.description', '40%',
        (role) => role && role.additionalInfo ? role.additionalInfo.description : '', entity => ({}), false)
    );

    this.config.deleteEntityTitle = role =>
      this.translate.instant('role.delete-role-title', { roleName: role.name });
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-role-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');
    this.config.entitiesFetchFunction = pageLink => this.roleService.getRoles(pageLink);
    this.config.loadEntity = id => this.roleService.getRole(id.id);
    this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);

    this.config.onEntityAction = action => this.onRoleAction(action);
  }

  resolve(): EntityTableConfig<Role> {
    this.config.tableTitle = this.translate.instant('role.roles');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  onRoleAction(action: EntityAction<Role>): boolean {
    return false;
  }

}
