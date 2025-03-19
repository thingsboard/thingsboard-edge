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

import { BaseData } from '@shared/models/base-data';
import { MobileAppId } from '@shared/models/id/mobile-app-id';
import { OAuth2ClientInfo, PlatformType } from '@shared/models/oauth2.models';
import { MobileAppBundleId } from '@shared/models/id/mobile-app-bundle-id';
import { HasTenantId } from '@shared/models/entity.models';
import { deepClone, isNotEmptyStr } from '@core/utils';
import { MobileSelfRegistrationParams } from '@shared/models/self-register.models';

export const WEB_URL_REGEX = /^(https?:\/\/)?(localhost|([\p{L}\p{M}\w-]+\.)+[\p{L}\p{M}\w-]+)(:\d+)?(\/[\w\-._~:/?#[\]@!$&'()*+,;=%\p{L}\p{N}]*)?$/u;

export interface QrCodeSettings extends HasTenantId {
  useSystemSettings: boolean;
  useDefaultApp: boolean;
  mobileAppBundleId: MobileAppBundleId
  androidEnabled: boolean;
  iosEnabled: boolean;
  qrCodeConfig: QRCodeConfig;
  readonly googlePlayLink: string;
  readonly appStoreLink: string;
  id: {
    id: string;
  }
}

export interface QRCodeConfig {
  showOnHomePage: boolean;
  badgeEnabled: boolean;
  badgePosition: BadgePosition;
  qrCodeLabelEnabled: boolean;
  qrCodeLabel: string;
}

export enum BadgePosition {
  RIGHT = 'RIGHT',
  LEFT = 'LEFT'
}

export const badgePositionTranslationsMap = new Map<BadgePosition, string>([
  [BadgePosition.RIGHT, 'admin.mobile-app.right'],
  [BadgePosition.LEFT, 'admin.mobile-app.left']
]);

export enum MobileAppStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  SUSPENDED = 'SUSPENDED'
}

export const mobileAppStatusTranslations = new Map<MobileAppStatus, string>(
  [
    [MobileAppStatus.DRAFT, 'mobile.status-type.draft'],
    [MobileAppStatus.PUBLISHED, 'mobile.status-type.published'],
    [MobileAppStatus.DEPRECATED, 'mobile.status-type.deprecated'],
    [MobileAppStatus.SUSPENDED, 'mobile.status-type.suspended'],
  ]
);

export interface VersionInfo {
  minVersion: string;
  minVersionReleaseNotes?: string;
  latestVersion: string;
  latestVersionReleaseNotes?: string;
}

export interface StoreInfo {
  sha256CertFingerprints?: string;
  storeLink: string;
  appId?: string;
}

export interface MobileApp extends BaseData<MobileAppId>, HasTenantId {
  pkgName: string;
  appSecret: string;
  platformType: PlatformType;
  status: MobileAppStatus;
  versionInfo: VersionInfo;
  storeInfo: StoreInfo;
}

enum MobileMenuPath {
  HOME = 'HOME',
  ASSETS = 'ASSETS',
  DEVICES = 'DEVICES',
  DEVICE_LIST = 'DEVICE_LIST',
  ALARMS = 'ALARMS',
  DASHBOARDS = 'DASHBOARDS',
  DASHBOARD = 'DASHBOARD',
  AUDIT_LOGS = 'AUDIT_LOGS',
  CUSTOMERS = 'CUSTOMERS',
  NOTIFICATIONS = 'NOTIFICATIONS'
}

export enum MobilePageType {
  DEFAULT = 'DEFAULT',
  CUSTOM = 'CUSTOM',
  DASHBOARD = 'DASHBOARD',
  WEB_VIEW = 'WEB_VIEW',
}

export const mobilePageTypeTranslations = new Map<MobilePageType, string>(
  [
    [MobilePageType.CUSTOM, 'mobile.pages-types.custom'],
    [MobilePageType.DASHBOARD, 'mobile.pages-types.dashboard'],
    [MobilePageType.WEB_VIEW, 'mobile.pages-types.web-view'],
  ]
);

export interface MobilePage {
  label?: string;
  icon?: string;
  type: MobilePageType;
  visible: boolean;
}

export interface DefaultMobilePage extends MobilePage {
  id: MobileMenuPath;
}

export interface CustomMobilePage extends MobilePage {
  dashboardId?: string;
  url?: string;
  path?: string;
}

export interface MobileLayoutConfig {
  pages: MobilePage[];
}

export interface MobileAppBundle extends Omit<BaseData<MobileAppBundleId>, 'label'>, HasTenantId {
  title?: string;
  description?: string;
  androidAppId?: MobileAppId;
  iosAppId?: MobileAppId;
  layoutConfig?: MobileLayoutConfig;
  selfRegistrationParams?: MobileSelfRegistrationParams;
  oauth2Enabled: boolean;
}

export interface MobileAppBundleInfo extends MobileAppBundle {
  androidPkgName: string;
  iosPkgName: string;
  androidPkg?: {
    name: string;
    id: MobileAppId
  };
  iosPkg?: {
    name: string;
    id: MobileAppId
  }
  oauth2ClientInfos?: Array<OAuth2ClientInfo>;
  qrCodeEnabled: boolean;
}

const defaultMobileMenu = [
  MobileMenuPath.HOME,
  MobileMenuPath.ALARMS,
  MobileMenuPath.DEVICES,
  MobileMenuPath.CUSTOMERS,
  MobileMenuPath.ASSETS,
  MobileMenuPath.AUDIT_LOGS,
  MobileMenuPath.NOTIFICATIONS,
  MobileMenuPath.DEVICE_LIST,
  MobileMenuPath.DASHBOARDS
];

export const hideDefaultMenuItems = [
  MobileMenuPath.DEVICE_LIST,
  MobileMenuPath.DASHBOARDS
];

export const getDefaultMobileMenuItem = (): DefaultMobilePage[] => {
  return deepClone(defaultMobileMenu).map(item => ({
    visible: !hideDefaultMenuItems.includes(item),
    type: MobilePageType.DEFAULT,
    id: item
  }))
}

export const isDefaultMobileMenuItem = (item: MobilePage): item is DefaultMobilePage => {
  const path = (item as DefaultMobilePage).id;
  return isNotEmptyStr(path) && defaultMobilePageMap.has(path);
};


const mobilePageEqualToDefault = (item: MobilePage, defaultMobilePage: DefaultMobilePage): boolean => {
  if (isDefaultMobileMenuItem(item) && (hideDefaultMenuItems.includes(item.id) ? !item.visible : item.visible)) {
    return !(item.id !== defaultMobilePage.id || !!item.label || !!item.icon);
  } else {
    return false;
  }
};

export const isDefaultMobilePagesConfig = (items: MobilePage[]): boolean => {
  const defaultMenus = getDefaultMobileMenuItem();
  if (!items?.length && !defaultMenus?.length) {
    return true;
  } else if (items.length !== defaultMenus.length) {
    return false;
  } else {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      const defaultMenuItem = defaultMenus[i];
      if (!mobilePageEqualToDefault(item, defaultMenuItem)) {
        return false;
      }
    }
    return true;
  }
};

export const mobileMenuDividers = new Map<number, string>([
  [2, 'mobile.mobile-599'],
  [4, 'mobile.tablet-959'],
  [6, 'mobile.max-element-number'],
]);

export const defaultMobilePageMap = new Map<MobileMenuPath, Omit<DefaultMobilePage, 'type' | 'visible'>>([
  [
    MobileMenuPath.HOME,
    {
      id: MobileMenuPath.HOME,
      icon: 'home',
      label: 'Home'
    }
  ],
  [
    MobileMenuPath.ALARMS,
    {
      id: MobileMenuPath.ALARMS,
      icon: 'notifications',
      label: 'Alarms'
    }
  ],
  [
    MobileMenuPath.DEVICES,
    {
      id: MobileMenuPath.DEVICES,
      icon: 'devices_other',
      label: 'Devices'
    }
  ],
  [
    MobileMenuPath.CUSTOMERS,
    {
      id: MobileMenuPath.CUSTOMERS,
      icon: 'supervisor_account',
      label: 'Customers'
    }
  ],
  [
    MobileMenuPath.ASSETS,
    {
      id: MobileMenuPath.ASSETS,
      icon: 'domain',
      label: 'Assets'
    }
  ],
  [
    MobileMenuPath.DEVICE_LIST,
    {
      id: MobileMenuPath.DEVICE_LIST,
      icon: 'devices',
      label: 'Device list'
    }
  ],
  [
    MobileMenuPath.DASHBOARDS,
    {
      id: MobileMenuPath.DASHBOARDS,
      icon: 'dashboard',
      label: 'Dashboards'
    }
  ],
  [
    MobileMenuPath.AUDIT_LOGS,
    {
      id: MobileMenuPath.AUDIT_LOGS,
      icon: 'track_changes',
      label: 'Audit logs'
    }
  ],
  [
    MobileMenuPath.NOTIFICATIONS,
    {
      id: MobileMenuPath.NOTIFICATIONS,
      icon: 'notifications_active',
      label: 'Notification'
    }
  ]
])
