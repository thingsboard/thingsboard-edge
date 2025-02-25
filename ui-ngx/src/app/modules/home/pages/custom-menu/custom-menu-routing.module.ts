///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { inject, NgModule } from '@angular/core';
import { defaultUserMenuMap, MenuId } from '@core/services/menu.models';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import {
  afterLoadCustomMenuConfig,
  cmScopeToAuthority,
  CustomMenuConfig,
  CustomMenuInfo,
  referenceToMenuItem
} from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { CustomMenuConfigComponent } from '@home/pages/custom-menu/custom-menu-config.component';
import { map } from 'rxjs/operators';
import { CustomMenuTableComponent } from '@home/pages/custom-menu/custom-menu-table.component';

const customMenuConfigBreadcrumbLabelFunction: BreadCrumbLabelFunction<any> = ((route) =>
  route.data.customMenu.name);

const customMenuResolver: ResolveFn<CustomMenuInfo> = (route: ActivatedRouteSnapshot) => {
  const customMenuId = route.params.customMenuId;
  return inject(CustomMenuService).getCustomMenuInfo(customMenuId);
};

const customMenuConfigResolver: ResolveFn<CustomMenuConfig> = (route: ActivatedRouteSnapshot,
                                                               _state: RouterStateSnapshot,
                                                               customMenuService = inject(CustomMenuService)) => {
  const customMenuId = route.params.customMenuId;
  return customMenuService.getCustomMenuConfig(customMenuId).pipe(
    map((config) => {
      const customMenu: CustomMenuInfo = route.parent.data.customMenu;
      const scope = customMenu.scope;
      return afterLoadCustomMenuConfig(config, scope);
    })
  );
};

export const CustomMenuRoutes: Routes = [
  {
    path: 'customMenu',
    data: {
      breadcrumb: {
        menuId: MenuId.custom_menu
      }
    },
    children: [
      {
        path: '',
        component: CustomMenuTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-menu.custom-menu'
        }
      },
      {
        path: ':customMenuId',
        data: {
          breadcrumb: {
            labelFunction: customMenuConfigBreadcrumbLabelFunction,
            icon: 'list'
          } as BreadCrumbConfig<any>,
        },
        resolve: {
          customMenu: customMenuResolver
        },
        children: [
          {
            path: '',
            component: CustomMenuConfigComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'custom-menu.custom-menu-config'
            },
            resolve: {
              customMenuConfig: customMenuConfigResolver
            }
          }
        ]
      }
    ]
  }
];

@NgModule({})
export class CustomMenuRoutingModule { }
