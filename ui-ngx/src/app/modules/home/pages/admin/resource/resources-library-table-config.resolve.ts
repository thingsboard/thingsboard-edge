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
import {
  checkBoxCell,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Resolve, Router } from '@angular/router';
import { Resource, ResourceInfo, ResourceTypeTranslationMap } from '@shared/models/resource.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ResourceService } from '@core/http/resource.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { ResourcesLibraryComponent } from '@home/pages/admin/resource/resources-library.component';
import { PageLink } from '@shared/models/page/page-link';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { map } from 'rxjs/operators';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class ResourcesLibraryTableConfigResolver implements Resolve<EntityTableConfig<Resource, PageLink, ResourceInfo>> {

  private readonly config: EntityTableConfig<Resource, PageLink, ResourceInfo> = new EntityTableConfig<Resource, PageLink, ResourceInfo>();
  private readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;

  constructor(private store: Store<AppState>,
              private resourceService: ResourceService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private router: Router,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.TB_RESOURCE;
    this.config.entityComponent = ResourcesLibraryComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.TB_RESOURCE);
    this.config.entityResources = entityTypeResources.get(EntityType.TB_RESOURCE);

    this.config.entityTitle = (resource) => resource ?
      resource.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<ResourceInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ResourceInfo>('title', 'resource.title', '60%'),
      new EntityTableColumn<ResourceInfo>('resourceType', 'resource.resource-type', '40%',
        entity => this.resourceTypesTranslationMap.get(entity.resourceType)),
      new EntityTableColumn<ResourceInfo>('tenantId', 'resource.system', '60px',
        entity => {
          return checkBoxCell(entity.tenantId.id === NULL_UUID);
        }),
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('resource.download'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.downloadResource($event, entity)
      }
    );

    this.config.deleteEntityTitle = resource => this.translate.instant('resource.delete-resource-title',
      { resourceTitle: resource.title });
    this.config.deleteEntityContent = () => this.translate.instant('resource.delete-resource-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('resource.delete-resources-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('resource.delete-resources-text');

    this.config.entitiesFetchFunction = pageLink => this.resourceService.getResources(pageLink);
    this.config.loadEntity = id => this.resourceService.getResource(id.id);
    this.config.saveEntity = resource => this.saveResource(resource);
    this.config.deleteEntity = id => this.resourceService.deleteResource(id.id);

    this.config.onEntityAction = action => this.onResourceAction(action);
  }

  saveResource(resource) {
    if (Array.isArray(resource.data)) {
      const resources = [];
      resource.data.forEach((data, index) => {
        resources.push({
          resourceType: resource.resourceType,
          data,
          fileName: resource.fileName[index],
          title: resource.title
        });
      });
      return this.resourceService.saveResources(resources, {resendRequest: true}).pipe(
        map((response) => response[0])
      );
    } else {
      return this.resourceService.saveResource(resource);
    }
  }

  resolve(): EntityTableConfig<Resource, PageLink, ResourceInfo> {
    this.config.tableTitle = this.translate.instant('resource.resources-library');
    const authUser = getCurrentAuthUser(this.store);
    this.config.deleteEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.entitySelectionEnabled = (resource) => this.isResourceEditable(resource, authUser.authority);
    this.config.detailsReadonly = (resource) => !this.isResourceEditable(resource, authUser.authority);
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private openResource($event: Event, resourceInfo: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['settings', 'resources-library', resourceInfo.id.id]);
    this.router.navigateByUrl(url);
  }

  downloadResource($event: Event, resource: ResourceInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.resourceService.downloadResource(resource.id.id).subscribe();
  }

  onResourceAction(action: EntityAction<ResourceInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openResource(action.event, action.entity);
        return true;
      case 'downloadResource':
        this.downloadResource(action.event, action.entity);
        return true;
    }
    return false;
  }

  private isResourceEditable(resource: ResourceInfo, authority: Authority): boolean {
    if (authority === Authority.TENANT_ADMIN) {
      return resource && resource.tenantId && resource.tenantId.id !== NULL_UUID;
    } else {
      return authority === Authority.SYS_ADMIN;
    }
  }
}
