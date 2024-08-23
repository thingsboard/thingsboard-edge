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

import { MenuId, MenuReference } from '@core/services/menu.models';
import { isNotEmptyStr } from '@core/utils';
import { BaseData } from '@shared/models/base-data';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityInfo, HasTenantId } from '@shared/models/entity.models';

export enum CMScope {
  SYSTEM = 'SYSTEM',
  TENANT = 'TENANT',
  CUSTOMER = 'CUSTOMER'
}

export const cmScopeTranslations = new Map<CMScope, string>(
  [
    [CMScope.SYSTEM, 'custom-menu.scope-system'],
    [CMScope.TENANT, 'custom-menu.scope-tenant'],
    [CMScope.CUSTOMER, 'custom-menu.scope-customer']
  ]
);

export enum CMAssigneeType {
  NO_ASSIGN = 'NO_ASSIGN',
  ALL = 'ALL',
  CUSTOMERS = 'CUSTOMERS',
  USERS = 'USERS'
}

export const cmAssigneeTypeTranslations = new Map<CMAssigneeType, string>(
  [
    [CMAssigneeType.NO_ASSIGN, 'custom-menu.assignee-no-assign'],
    [CMAssigneeType.ALL, 'custom-menu.assignee-all'],
    [CMAssigneeType.CUSTOMERS, 'custom-menu.assignee-customers'],
    [CMAssigneeType.USERS, 'custom-menu.assignee-users']
  ]
);

export interface CustomMenuInfo extends BaseData<CustomMenuId>, HasTenantId {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  scope: CMScope;
  assigneeType: CMAssigneeType;
}

export interface DefaultMenuItem extends MenuReference {
  id: MenuId;
  name?: string;
  icon?: string;
  pages?: DefaultMenuItem[];
  visible?: boolean;
}

export enum HomeMenuItemType {
  DEFAULT = 'DEFAULT',
  DASHBOARD = 'DASHBOARD'
}

export interface HomeMenuItem extends DefaultMenuItem {
  id: MenuId.home;
  homeType: HomeMenuItemType;
  dashboardId?: string;
  hideDashboardToolbar?: boolean;
}

export enum CMItemType {
  LINK = 'LINK',
  SECTION = 'SECTION'
}

export enum CMItemLinkType {
  URL = 'URL',
  DASHBOARD = 'DASHBOARD'
}

export interface CustomMenuItem {
  name: string;
  icon: string;
  menuItemType: CMItemType;
  linkType?: CMItemLinkType;
  dashboardId?: string;
  hideDashboardToolbar?: boolean;
  url?: string;
  setAccessToken?: boolean;
  pages?: CustomMenuItem[];
  visible?: boolean;
}

export type MenuItem = DefaultMenuItem | HomeMenuItem | CustomMenuItem;

export interface CustomMenuConfig {
  items: MenuItem[];
}

export interface CustomMenu extends CustomMenuInfo {
  config: CustomMenuConfig;
}

export interface CustomMenuDeleteResult {
  success: boolean;
  assigneeType: CMAssigneeType;
  assigneeList: EntityInfo[];
}

export const isDefaultMenuItem = (item: MenuItem): item is DefaultMenuItem => {
  const id = (item as DefaultMenuItem).id;
  return isNotEmptyStr(id) && !!MenuId[id];
};

export const isHomeMenuItem = (item: MenuItem): item is HomeMenuItem =>
  (item as DefaultMenuItem).id === MenuId.home;

export const isCustomMenuItem = (item: MenuItem): item is CustomMenuItem => {
  const customItem = item as CustomMenuItem;
  return isNotEmptyStr(customItem.name) && isNotEmptyStr(customItem.icon)
    && isNotEmptyStr(customItem.menuItemType) && !!CMItemType[customItem.menuItemType];
};

export const referenceToMenuItem = (reference: MenuReference): DefaultMenuItem => {
  const menuItem: MenuItem = {
    id: MenuId[reference.id],
    visible: true
  };
  if (isHomeMenuItem(menuItem)) {
    menuItem.homeType = HomeMenuItemType.DEFAULT;
  } else if (reference.pages?.length) {
    menuItem.pages = reference.pages.map(page =>
      referenceToMenuItem(page));
  }
  return menuItem;
};
