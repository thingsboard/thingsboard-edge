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

import { BaseData } from '@shared/models/base-data';
import { MobileAppId } from '@shared/models/id/mobile-app-id';
import { OAuth2ClientInfo, PlatformType } from '@shared/models/oauth2.models';
import { MobileAppBundleId } from '@shared/models/id/mobile-app-bundle-id';
import { HasTenantId } from '@shared/models/entity.models';

export interface QrCodeSettings extends HasTenantId {
  useDefaultApp: boolean;
  useSystemSettings: boolean;
  mobileAppBundleId: MobileAppBundleId
  androidConfig: AndroidConfig; //TODO: need remove
  iosConfig: IosConfig; //TODO: need remove
  qrCodeConfig: QRCodeConfig;
  defaultGooglePlayLink: string;
  defaultAppStoreLink: string;
  id: {
    id: string;
  }
}

export interface AndroidConfig {
  enabled: boolean;
  appPackage: string;
  sha256CertFingerprints: string;
  storeLink: string;
}

export interface IosConfig {
  enabled: boolean;
  appId: string;
  storeLink: string;
}

export interface QRCodeConfig {
  showOnHomePage: boolean;
  badgeEnabled: boolean;
  badgePosition: BadgePosition;
  qrCodeLabelEnabled: boolean;
  qrCodeLabel: string;
}

export interface MobileOSBadgeURL {
  iOS: string;
  android: string;
}

export enum BadgePosition {
  RIGHT = 'RIGHT',
  LEFT = 'LEFT'
}

export const badgePositionTranslationsMap = new Map<BadgePosition, string>([
  [BadgePosition.RIGHT, 'admin.mobile-app.right'],
  [BadgePosition.LEFT, 'admin.mobile-app.left']
]);

export type QrCodeConfig = AndroidConfig & IosConfig;

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
  CUSTOMER = 'CUSTOMER',
  NOTIFICATION = 'NOTIFICATION',
  CUSTOM = 'CUSTOM'
}

export interface MobileMenuItem {
  label: string;
  icon: string;
  path: MobileMenuPath;
  id: string;
}

export interface MobileLayoutConfig {
  items: MobileMenuItem[];
}

export interface MobileAppBundle extends Omit<BaseData<MobileAppBundleId>, 'label'>, HasTenantId {
  title?: string;
  description?: string;
  androidAppId?: MobileAppId;
  iosAppId?: MobileAppId;
  layoutConfig?: MobileLayoutConfig;
  oauth2Enabled: boolean;
}

export interface MobileAppBundleInfo extends MobileAppBundle {
  androidPkgName: string;
  iosPkgName: string;
  oauth2ClientInfos?: Array<OAuth2ClientInfo>;
}
