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

import { Resolve, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { Direction } from '@shared/models/page/sort-order';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { cmAssigneeTypeTranslations, cmScopeTranslations, CustomMenuInfo } from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { CustomMenuTableHeaderComponent } from '@home/pages/custom-menu/custom-menu-table-header.component';

@Injectable()
export class CustomMenuTableConfigResolver implements Resolve<EntityTableConfig<CustomMenuInfo>> {

  private readonly config: EntityTableConfig<CustomMenuInfo> = new EntityTableConfig<CustomMenuInfo>();

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService) {

    this.config.tableTitle = '';
    this.config.headerComponent = CustomMenuTableHeaderComponent;
    this.config.detailsPanelEnabled = false;
    this.config.searchEnabled = true;
    this.config.entityTranslations = {
      add: 'custom-menu.add',
      noEntities: 'custom-menu.no-custom-menus-prompt',
      search: 'custom-menu.search'
    };
    this.config.entityResources = {
     } as EntityTypeResource<CustomMenuInfo>;
    this.config.entitiesFetchFunction = pageLink => this.customMenuService.getCustomMenuInfos(pageLink);
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.ASC};

    this.config.deleteEntityTitle = customMenu => this.translate.instant('custom-menu.delete-custom-menu-title',
      { customMenuName: customMenu.name });
    this.config.deleteEntityContent = () => this.translate.instant('custom-menu.delete-custom-menu-text');

    this.config.saveEntity = customMenu => this.customMenuService.saveCustomMenu(customMenu);
    this.config.deleteEntity = id => this.customMenuService.deleteCustomMenu(id.id);
  }

  resolve(): EntityTableConfig<CustomMenuInfo> {
    const authUser = getCurrentAuthUser(this.store);
    const readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
    this.config.addEnabled = !readonly && authUser.authority !== Authority.SYS_ADMIN;
    this.config.entitiesDeleteEnabled = false;
    this.config.deleteEnabled = () => !readonly && authUser.authority !== Authority.SYS_ADMIN;

    this.config.columns.length = 0;

    let mainColumnsWidth: string;
    switch (authUser.authority) {
      case Authority.SYS_ADMIN:
        mainColumnsWidth = '50%';
        break;
      case Authority.TENANT_ADMIN:
        mainColumnsWidth = '33%';
        break;
      case Authority.CUSTOMER_USER:
        mainColumnsWidth = '50%';
        break;
    }

    this.config.columns.push(
      new DateEntityTableColumn<CustomMenuInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<CustomMenuInfo>('name', 'custom-menu.name', mainColumnsWidth)
    );
    if (authUser.authority !== Authority.CUSTOMER_USER) {
      this.config.columns.push(new EntityTableColumn<CustomMenuInfo>('scope', 'custom-menu.scope', mainColumnsWidth,
        (menu) => this.translate.instant(cmScopeTranslations.get(menu.scope))));
    }
    if (authUser.authority !== Authority.SYS_ADMIN) {
      this.config.columns.push(new EntityTableColumn<CustomMenuInfo>('assigneeType', 'custom-menu.assignee-type', mainColumnsWidth,
        (menu) => this.translate.instant(cmAssigneeTypeTranslations.get(menu.assigneeType))));
    }

    this.config.cellActionDescriptors.length = 0;
    if (authUser.authority !== Authority.SYS_ADMIN) {
      this.config.cellActionDescriptors.push(
        {
          name: this.translate.instant('custom-menu.manage-assignees'),
          icon: 'file_download',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCustomMenuAssignees($event, entity)
        }
      );
    }
    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('custom-menu.manage-config'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.openCustomMenuConfig($event, entity)
      }
    );

    return this.config;
  }

  updateCustomMenuName($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
    }
  }

  manageCustomMenuAssignees($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
    }
  }

  openCustomMenuConfig($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`white-labeling/customMenu/${customMenu.id.id}`);
  }
}
