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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { ShortCustomerInfo } from '@shared/models/customer.model';
import { Widget } from './widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityAliases } from './alias.models';
import { CustomerId } from '@shared/models/id/customer-id';
import { Filters } from '@shared/models/query/query.models';
import { MatDialogRef } from '@angular/material/dialog';

export interface DashboardInfo extends BaseData<DashboardId>, ExportableEntity<DashboardId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  title?: string;
  image?: string;
  assignedCustomers?: Array<ShortCustomerInfo>;
  mobileHide?: boolean;
  mobileOrder?: number;
}

export interface WidgetLayout {
  sizeX?: number;
  sizeY?: number;
  desktopHide?: boolean;
  mobileHide?: boolean;
  mobileHeight?: number;
  mobileOrder?: number;
  col?: number;
  row?: number;
}

export interface WidgetLayouts {
  [id: string]: WidgetLayout;
}

export interface GridSettings {
  backgroundColor?: string;
  columns?: number;
  margin?: number;
  backgroundSizeMode?: string;
  backgroundImageUrl?: string;
  autoFillHeight?: boolean;
  mobileAutoFillHeight?: boolean;
  mobileRowHeight?: number;
  layoutDimension?: LayoutDimension;
  [key: string]: any;
}

export interface DashboardLayout {
  widgets: WidgetLayouts;
  gridSettings: GridSettings;
}

export interface DashboardLayoutInfo {
  widgetIds?: string[];
  widgetLayouts?: WidgetLayouts;
  gridSettings?: GridSettings;
}

export interface LayoutDimension {
  type?: LayoutType,
  fixedWidth?: number,
  fixedLayout?: DashboardLayoutId,
  leftWidthPercentage?: number
}

export declare type DashboardLayoutId = 'main' | 'right';

export declare type LayoutType = 'percentage' | 'fixed';

export declare type DashboardStateLayouts = {[key in DashboardLayoutId]?: DashboardLayout};

export declare type DashboardLayoutsInfo = {[key in DashboardLayoutId]?: DashboardLayoutInfo};

export interface DashboardState {
  name: string;
  root: boolean;
  layouts: DashboardStateLayouts;
}

export declare type StateControllerId = 'entity' | 'default' | string;

export interface DashboardSettings {
  stateControllerId?: StateControllerId;
  showTitle?: boolean;
  showDashboardsSelect?: boolean;
  showEntitiesSelect?: boolean;
  showFilters?: boolean;
  showDashboardLogo?: boolean;
  dashboardLogoUrl?: string;
  showDashboardTimewindow?: boolean;
  showDashboardExport?: boolean;
  showUpdateDashboardImage?: boolean;
  toolbarAlwaysOpen?: boolean;
  hideToolbar?: boolean;
  titleColor?: string;
  dashboardCss?: string;
}

export interface DashboardConfiguration {
  timewindow?: Timewindow;
  settings?: DashboardSettings;
  widgets?: {[id: string]: Widget } | Widget[];
  states?: {[id: string]: DashboardState };
  entityAliases?: EntityAliases;
  filters?: Filters;
  [key: string]: any;
}

export interface Dashboard extends DashboardInfo {
  configuration?: DashboardConfiguration;
  dialogRef?: MatDialogRef<any>;
}

export interface HomeDashboard extends Dashboard {
  hideDashboardToolbar: boolean;
}

export interface HomeDashboardInfo {
  dashboardId: DashboardId;
  hideDashboardToolbar: boolean;
}

export function isPublicDashboard(dashboard: DashboardInfo): boolean {
  if (dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers
      .filter(customerInfo => customerInfo.public).length > 0;
  } else {
    return false;
  }
}

export function getDashboardAssignedCustomersText(dashboard: DashboardInfo): string {
  if (dashboard && dashboard.assignedCustomers && dashboard.assignedCustomers.length > 0) {
    return dashboard.assignedCustomers
      .filter(customerInfo => !customerInfo.public)
      .map(customerInfo => customerInfo.title)
      .join(', ');
  } else {
    return '';
  }
}

export function isCurrentPublicDashboardCustomer(dashboard: DashboardInfo, customerId: string): boolean {
  if (customerId && dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers.filter(customerInfo => {
      return customerInfo.public && customerId === customerInfo.customerId.id;
    }).length > 0;
  } else {
    return false;
  }
}
