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


import { MenuId } from '@core/services/menu.models';

export interface CustomMenuItem {
  name: string;
  iconUrl: string;
  materialIcon: string;
  iframeUrl: string;
  dashboardId: string;
  hideDashboardToolbar: boolean;
  setAccessToken: boolean;
  childMenuItems: CustomMenuItem[];
}

export interface CustomMenu {
  disabledMenuItems: MenuId[];
  menuItems: CustomMenuItem[];
}

export enum CustomMenuItemType {
  link = 'link',
  section = 'section'
}

export enum CustomMenuItemLinkType {
  url = 'url',
  dashboard = 'dashboard'
}

export interface CustomMenuItemV2 {
  name: string;
  icon: string;
  type: CustomMenuItemType;
  linkType?: CustomMenuItemLinkType;
  dashboardId?: string;
  hideDashboardToolbar?: boolean;
  url?: string;
  setAccessToken?: boolean;
  pages?: CustomMenuItemV2[];
}

export interface DefaultMenuItem {
  id: MenuId;
  name?: string;
  icon?: string;
  pages?: DefaultMenuItem[];
}

export enum HomeMenuItemType {
  default = 'default',
  dashboard = 'dashboard'
}

export interface HomeMenuItem extends DefaultMenuItem {
  id: MenuId.home;
  type: HomeMenuItemType;
  dashboardId?: string;
  hideDashboardToolbar?: boolean;
}

export type MenuItem = DefaultMenuItem | HomeMenuItem | CustomMenuItemV2;

export interface CustomMenuV2 {
  items: MenuItem[];
}

export interface DefaultMenuItemConfig extends DefaultMenuItem {
  visible: boolean;
  pages?: DefaultMenuItemConfig[];
}

export interface CustomMenuItemConfig extends CustomMenuItemV2 {
  visible: boolean;
  pages?: CustomMenuItemConfig[];
}

export type MenuItemConfig = DefaultMenuItemConfig | HomeMenuItem | CustomMenuItemConfig;

export interface CustomMenuConfig {
  items: MenuItemConfig[];
}

