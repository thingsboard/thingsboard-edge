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

import { EntityType } from '@shared/models/entity-type.models';
import { AuthState } from '@core/auth/auth.models';
import { Authority } from '@shared/models/authority.enum';
import { deepClone, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import {
  CMItemLinkType,
  CMItemType,
  CustomMenuConfig,
  CustomMenuItem,
  HomeMenuItemType,
  isCustomMenuItem,
  isDefaultMenuItem,
  isHomeMenuItem,
  MenuItem
} from '@shared/models/custom-menu.models';

export declare type MenuSectionType = 'link' | 'toggle';

export interface MenuReference {
  id: MenuId  | string;
  pages?: Array<MenuReference>;
}

export interface MenuSection extends MenuReference {
  id: MenuId | string;
  name: string;
  fullName?: string;
  type: MenuSectionType;
  path: string;
  queryParams?: {[k: string]: any};
  icon: string;
  pages?: Array<MenuSection>;
  opened?: boolean;
  rootOnly?: boolean;
  isCustom?: boolean;
  isNew?: boolean;
  stateId?: string;
  childStateIds?: {[stateId: string]: boolean};
  customTranslate?: boolean;
  homeDashboardId?: string;
  homeHideDashboardToolbar?: boolean;
}

export const sectionPath = (section: MenuSection): string => {
  if (section.isCustom) {
    return section.path + '/' + section.stateId;
  } else {
    return section.path;
  }
};

export const filterOpenedMenuSection = (section: MenuSection, url: string, openedMenuSections: string[]): boolean =>
  section.type === 'toggle' &&
    ((!section.isCustom && url.startsWith(section.path)) || openedMenuSections.includes(sectionPath(section)));

export interface HomeSectionReference {
  name: string;
  places: Array<MenuId>;
}

export interface HomeSection {
  name: string;
  places: Array<MenuSection>;
}

export enum MenuId {
  home = 'home',
  tenants = 'tenants',
  tenant_profiles = 'tenant_profiles',
  resources = 'resources',
  widget_library = 'widget_library',
  widget_types = 'widget_types',
  widgets_bundles = 'widgets_bundles',
  images = 'images',
  scada_symbols = 'scada_symbols',
  resources_library = 'resources_library',
  javascript_library = 'javascript_library',
  notifications_center = 'notifications_center',
  notification_inbox = 'notification_inbox',
  notification_sent = 'notification_sent',
  notification_recipients = 'notification_recipients',
  notification_templates = 'notification_templates',
  notification_rules = 'notification_rules',
  mobile_center = 'mobile_center',
  mobile_apps = 'mobile_apps',
  mobile_bundles = 'mobile_bundles',
  mobile_qr_code_widget = 'mobile_qr_code_widget',
  settings = 'settings',
  general = 'general',
  mail_server = 'mail_server',
  home_settings = 'home_settings',
  notification_settings = 'notification_settings',
  repository_settings = 'repository_settings',
  auto_commit_settings = 'auto_commit_settings',
  queues = 'queues',
  security_settings = 'security_settings',
  security_settings_general = 'security_settings_general',
  two_fa = 'two_fa',
  oauth2 = 'oauth2',
  domains = 'domains',
  clients = 'clients',
  audit_log = 'audit_log',
  alarms = 'alarms',
  dashboards = 'dashboards',
  entities = 'entities',
  devices = 'devices',
  assets = 'assets',
  entity_views = 'entity_views',
  gateways = 'gateways',
  profiles = 'profiles',
  device_profiles = 'device_profiles',
  asset_profiles = 'asset_profiles',
  customers = 'customers',
  rule_chains = 'rule_chains',
  edge_management = 'edge_management',
  edges = 'edges',
  edge_instances = 'edge_instances',
  rulechain_templates = 'rulechain_templates',
  features = 'features',
  otaUpdates = 'otaUpdates',
  version_control = 'version_control',
  api_usage = 'api_usage',
  white_labeling = 'white_labeling',
  white_labeling_general = 'white_labeling_general',
  login_white_labeling = 'login_white_labeling',
  mail_templates = 'mail_templates',
  custom_translation = 'custom_translation',
  custom_menu = 'custom_menu',
  dashboard_all = 'dashboard_all',
  dashboard_groups = 'dashboard_groups',
  dashboard_shared = 'dashboard_shared',
  solution_templates = 'solution_templates',
  device_all = 'device_all',
  device_groups = 'device_groups',
  device_shared = 'device_shared',
  asset_all = 'asset_all',
  asset_groups = 'asset_groups',
  asset_shared = 'asset_shared',
  entity_view_all = 'entity_view_all',
  entity_view_groups = 'entity_view_groups',
  entity_view_shared = 'entity_view_shared',
  customer_all = 'customer_all',
  customer_groups = 'customer_groups',
  customer_shared = 'customer_shared',
  customers_hierarchy = 'customers_hierarchy',
  users = 'users',
  user_all = 'user_all',
  user_groups = 'user_groups',
  integrations_center = 'integrations_center',
  integrations = 'integrations',
  converters = 'converters',
  edge_all = 'edge_all',
  edge_groups = 'edge_groups',
  edge_shared = 'edge_shared',
  integration_templates = 'integration_templates',
  converter_templates = 'converter_templates',
  scheduler = 'scheduler',
  roles = 'roles',
  self_registration = 'self_registration',
  trendz_settings = 'trendz_settings'
}

declare type MenuFilter = (_authState: AuthState, userPermissionsService: UserPermissionsService) => boolean;

export const menuSectionMap = new Map<MenuId, MenuSection>([
  [
    MenuId.home,
    {
      id: MenuId.home,
      name: 'home.home',
      type: 'link',
      path: '/home',
      icon: 'home'
    }
  ],
  [
    MenuId.tenants,
    {
      id: MenuId.tenants,
      name: 'tenant.tenants',
      type: 'link',
      path: '/tenants',
      icon: 'supervisor_account'
    }
  ],
  [
    MenuId.tenant_profiles,
    {
      id: MenuId.tenant_profiles,
      name: 'tenant-profile.tenant-profiles',
      type: 'link',
      path: '/tenantProfiles',
      icon: 'mdi:alpha-t-box'
    }
  ],
  [
    MenuId.resources,
    {
      id: MenuId.resources,
      name: 'admin.resources',
      type: 'toggle',
      path: '/resources',
      icon: 'folder'
    }
  ],
  [
    MenuId.widget_library,
    {
      id: MenuId.widget_library,
      name: 'widget.widget-library',
      type: 'link',
      path: '/resources/widgets-library',
      icon: 'now_widgets'
    }
  ],
  [
    MenuId.widget_types,
    {
      id: MenuId.widget_types,
      name: 'widget.widgets',
      type: 'link',
      path: '/resources/widgets-library/widget-types',
      icon: 'now_widgets'
    }
  ],
  [
    MenuId.widgets_bundles,
    {
      id: MenuId.widgets_bundles,
      name: 'widgets-bundle.widgets-bundles',
      type: 'link',
      path: '/resources/widgets-library/widgets-bundles',
      icon: 'now_widgets'
    }
  ],
  [
    MenuId.images,
    {
      id: MenuId.images,
      name: 'image.gallery',
      type: 'link',
      path: '/resources/images',
      icon: 'filter'
    }
  ],
  [
    MenuId.scada_symbols,
    {
      id: MenuId.scada_symbols,
      name: 'scada.symbols',
      type: 'link',
      path: '/resources/scada-symbols',
      icon: 'view_in_ar'
    }
  ],
  [
    MenuId.resources_library,
    {
      id: MenuId.resources_library,
      name: 'resource.resources-library',
      type: 'link',
      path: '/resources/resources-library',
      icon: 'mdi:rhombus-split'
    }
  ],
  [
    MenuId.javascript_library,
    {
      id: MenuId.javascript_library,
      name: 'javascript.javascript-library',
      type: 'link',
      path: '/resources/javascript-library',
      icon: 'mdi:language-javascript'
    }
  ],
  [
    MenuId.notifications_center,
    {
      id: MenuId.notifications_center,
      name: 'notification.notification-center',
      type: 'link',
      path: '/notification',
      icon: 'mdi:message-badge'
    }
  ],
  [
    MenuId.notification_inbox,
    {
      id: MenuId.notification_inbox,
      name: 'notification.inbox',
      fullName: 'notification.notification-inbox',
      type: 'link',
      path: '/notification/inbox',
      icon: 'inbox'
    }
  ],
  [
    MenuId.notification_sent,
    {
      id: MenuId.notification_sent,
      name: 'notification.sent',
      fullName: 'notification.notification-sent',
      type: 'link',
      path: '/notification/sent',
      icon: 'outbox'
    }
  ],
  [
    MenuId.notification_recipients,
    {
      id: MenuId.notification_recipients,
      name: 'notification.recipients',
      fullName: 'notification.notification-recipients',
      type: 'link',
      path: '/notification/recipients',
      icon: 'contacts'
    }
  ],
  [
    MenuId.notification_templates,
    {
      id: MenuId.notification_templates,
      name: 'notification.templates',
      fullName: 'notification.notification-templates',
      type: 'link',
      path: '/notification/templates',
      icon: 'mdi:message-draw'
    }
  ],
  [
    MenuId.notification_rules,
    {
      id: MenuId.notification_rules,
      name: 'notification.rules',
      fullName: 'notification.notification-rules',
      type: 'link',
      path: '/notification/rules',
      icon: 'mdi:message-cog'
    }
  ],
  [
    MenuId.mobile_center,
    {
      id: MenuId.mobile_center,
      name: 'mobile.mobile-center',
      type: 'link',
      path: '/mobile-center',
      icon: 'smartphone'
    }
  ],
  [
    MenuId.mobile_apps,
    {
      id: MenuId.mobile_apps,
      name: 'mobile.applications',
      type: 'link',
      path: '/mobile-center/applications',
      icon: 'list'
    }
  ],
  [
    MenuId.mobile_bundles,
    {
      id: MenuId.mobile_bundles,
      name: 'mobile.bundles',
      type: 'link',
      path: '/mobile-center/bundles',
      icon: 'mdi:package'
    }
  ],
  [
    MenuId.mobile_qr_code_widget,
    {
      id: MenuId.mobile_qr_code_widget,
      name: 'mobile.qr-code-widget',
      fullName: 'mobile.qr-code-widget',
      type: 'link',
      path: '/mobile-center/qr-code-widget',
      icon: 'qr_code'
    }
  ],
  [
    MenuId.settings,
    {
      id: MenuId.settings,
      name: 'admin.settings',
      type: 'link',
      path: '/settings',
      icon: 'settings'
    }
  ],
  [
    MenuId.general,
    {
      id: MenuId.general,
      name: 'admin.general',
      fullName: 'admin.general-settings',
      type: 'link',
      path: '/settings/general',
      icon: 'settings_applications'
    }
  ],
  [
    MenuId.mail_server,
    {
      id: MenuId.mail_server,
      name: 'admin.outgoing-mail',
      type: 'link',
      path: '/settings/outgoing-mail',
      icon: 'mail'
    }
  ],
  [
    MenuId.home_settings,
    {
      id: MenuId.home_settings,
      name: 'admin.home',
      fullName: 'admin.home-settings',
      type: 'link',
      path: '/settings/home',
      icon: 'settings_applications'
    }
  ],
  [
    MenuId.notification_settings,
    {
      id: MenuId.notification_settings,
      name: 'admin.notifications',
      fullName: 'admin.notifications-settings',
      type: 'link',
      path: '/settings/notifications',
      icon: 'mdi:message-badge'
    }
  ],
  [
    MenuId.repository_settings,
    {
      id: MenuId.repository_settings,
      name: 'admin.repository',
      fullName: 'admin.repository-settings',
      type: 'link',
      path: '/settings/repository',
      icon: 'manage_history'
    }
  ],
  [
    MenuId.auto_commit_settings,
    {
      id: MenuId.auto_commit_settings,
      name: 'admin.auto-commit',
      fullName: 'admin.auto-commit-settings',
      type: 'link',
      path: '/settings/auto-commit',
      icon: 'settings_backup_restore'
    }
  ],
  [
    MenuId.queues,
    {
      id: MenuId.queues,
      name: 'admin.queues',
      type: 'link',
      path: '/settings/queues',
      icon: 'swap_calls'
    }
  ],
  [
    MenuId.security_settings,
    {
      id: MenuId.security_settings,
      name: 'security.security',
      type: 'toggle',
      path: '/security-settings',
      icon: 'security'
    }
  ],
  [
    MenuId.security_settings_general,
    {
      id: MenuId.security_settings_general,
      name: 'admin.general',
      fullName: 'security.general-settings',
      type: 'link',
      path: '/security-settings/general',
      icon: 'settings_applications'
    }
  ],
  [
    MenuId.two_fa,
    {
      id: MenuId.two_fa,
      name: 'admin.2fa.2fa',
      type: 'link',
      path: '/security-settings/2fa',
      icon: 'mdi:two-factor-authentication'
    }
  ],
  [
    MenuId.oauth2,
    {
      id: MenuId.oauth2,
      name: 'admin.oauth2.oauth2',
      type: 'link',
      path: '/security-settings/oauth2',
      icon: 'mdi:shield-account'
    }
  ],
  [
    MenuId.domains,
    {
      id: MenuId.domains,
      name: 'admin.oauth2.domains',
      type: 'link',
      path: '/security-settings/oauth2/domains',
      icon: 'domain'
    }
  ],
  [
    MenuId.clients,
    {
      id: MenuId.clients,
      name: 'admin.oauth2.clients',
      type: 'link',
      path: '/security-settings/oauth2/clients',
      icon: 'public'
    }
  ],
  [
    MenuId.audit_log,
    {
      id: MenuId.audit_log,
      name: 'audit-log.audit-logs',
      type: 'link',
      path: '/security-settings/auditLogs',
      icon: 'track_changes'
    }
  ],
  [
    MenuId.alarms,
    {
      id: MenuId.alarms,
      name: 'alarm.alarms',
      type: 'link',
      path: '/alarms',
      icon: 'mdi:alert-outline'
    }
  ],
  [
    MenuId.dashboards,
    {
      id: MenuId.dashboards,
      name: 'dashboard.dashboards',
      type: 'link',
      path: '/dashboards',
      icon: 'dashboards'
    }
  ],
  [
    MenuId.entities,
    {
      id: MenuId.entities,
      name: 'entity.entities',
      type: 'toggle',
      path: '/entities',
      icon: 'category'
    }
  ],
  [
    MenuId.devices,
    {
      id: MenuId.devices,
      name: 'device.devices',
      type: 'link',
      path: '/entities/devices',
      icon: 'devices_other'
    }
  ],
  [
    MenuId.assets,
    {
      id: MenuId.assets,
      name: 'asset.assets',
      type: 'link',
      path: '/entities/assets',
      icon: 'domain'
    }
  ],
  [
    MenuId.entity_views,
    {
      id: MenuId.entity_views,
      name: 'entity-view.entity-views',
      type: 'link',
      path: '/entities/entityViews',
      icon: 'view_quilt'
    }
  ],
  [
    MenuId.gateways,
    {
      id: MenuId.gateways,
      name: 'gateway.gateways',
      type: 'link',
      path: '/entities/gateways',
      icon: 'lan'
    }
  ],
  [
    MenuId.profiles,
    {
      id: MenuId.profiles,
      name: 'profiles.profiles',
      type: 'toggle',
      path: '/profiles',
      icon: 'badge'
    }
  ],
  [
    MenuId.device_profiles,
    {
      id: MenuId.device_profiles,
      name: 'device-profile.device-profiles',
      type: 'link',
      path: '/profiles/deviceProfiles',
      icon: 'mdi:alpha-d-box'
    }
  ],
  [
    MenuId.asset_profiles,
    {
      id: MenuId.asset_profiles,
      name: 'asset-profile.asset-profiles',
      type: 'link',
      path: '/profiles/assetProfiles',
      icon: 'mdi:alpha-a-box'
    }
  ],
  [
    MenuId.customers,
    {
      id: MenuId.customers,
      name: 'customer.customers',
      type: 'link',
      path: '/customers',
      icon: 'supervisor_account'
    }
  ],
  [
    MenuId.rule_chains,
    {
      id: MenuId.rule_chains,
      name: 'rulechain.rulechains',
      type: 'link',
      path: '/ruleChains',
      icon: 'settings_ethernet'
    }
  ],
  [
    MenuId.edge_management,
    {
      id: MenuId.edge_management,
      name: 'edge.management',
      type: 'toggle',
      path: '/edgeManagement',
      icon: 'settings_input_antenna'
    }
  ],
  [
    MenuId.edges,
    {
      id: MenuId.edges,
      name: 'edge.instances',
      fullName: 'edge.edge-instances',
      type: 'link',
      path: '/edgeManagement/instances',
      icon: 'router'
    }
  ],
  [
    MenuId.edge_instances,
    {
      id: MenuId.edge_instances,
      name: 'edge.edge-instances',
      fullName: 'edge.edge-instances',
      type: 'link',
      path: '/edgeManagement/instances',
      icon: 'router'
    }
  ],
  [
    MenuId.rulechain_templates,
    {
      id: MenuId.rulechain_templates,
      name: 'edge.rulechain-templates',
      fullName: 'edge.edge-rulechain-templates',
      type: 'link',
      path: '/edgeManagement/ruleChains',
      icon: 'settings_ethernet'
    }
  ],
  [
    MenuId.features,
    {
      id: MenuId.features,
      name: 'feature.advanced-features',
      type: 'toggle',
      path: '/features',
      icon: 'construction'
    }
  ],
  [
    MenuId.otaUpdates,
    {
      id: MenuId.otaUpdates,
      name: 'ota-update.ota-updates',
      type: 'link',
      path: '/features/otaUpdates',
      icon: 'memory'
    }
  ],
  [
    MenuId.version_control,
    {
      id: MenuId.version_control,
      name: 'version-control.version-control',
      type: 'link',
      path: '/features/vc',
      icon: 'history'
    }
  ],
  [
    MenuId.api_usage,
    {
      id: MenuId.api_usage,
      name: 'api-usage.api-usage',
      type: 'link',
      path: '/usage',
      icon: 'insert_chart'
    }
  ],
  [
    MenuId.white_labeling,
    {
      id: MenuId.white_labeling,
      name: 'white-labeling.white-labeling',
      type: 'link',
      path: '/white-labeling',
      icon: 'format_paint'
    }
  ],
  [
    MenuId.white_labeling_general,
    {
      id: MenuId.white_labeling_general,
      name: 'white-labeling.general',
      fullName: 'white-labeling.white-labeling-general',
      type: 'link',
      path: '/white-labeling/whiteLabel',
      icon: 'format_paint'
    }
  ],
  [
    MenuId.login_white_labeling,
    {
      id: MenuId.login_white_labeling,
      name: 'white-labeling.login',
      fullName: 'white-labeling.login-white-labeling',
      type: 'link',
      path: '/white-labeling/loginWhiteLabel',
      icon: 'format_paint'
    }
  ],
  [
    MenuId.mail_templates,
    {
      id: MenuId.mail_templates,
      name: 'admin.mail-templates',
      type: 'link',
      path: '/white-labeling/mail-template',
      icon: 'format_shapes'
    }
  ],
  [
    MenuId.custom_translation,
    {
      id: MenuId.custom_translation,
      name: 'custom-translation.custom-translation',
      type: 'link',
      path: '/white-labeling/customTranslation',
      icon: 'language'
    }
  ],
  [
    MenuId.custom_menu,
    {
      id: MenuId.custom_menu,
      name: 'custom-menu.custom-menu',
      type: 'link',
      path: '/white-labeling/customMenu',
      icon: 'list'
    }
  ],
  [
    MenuId.dashboard_all,
    {
      id: MenuId.dashboard_all,
      name: 'dashboard.all',
      fullName: 'dashboard.all-dashboards',
      type: 'link',
      path: '/dashboards/all',
      icon: 'dashboards'
    }
  ],
  [
    MenuId.dashboard_groups,
    {
      id: MenuId.dashboard_groups,
      name: 'dashboard.groups',
      fullName: 'entity-group.dashboard-groups',
      type: 'link',
      path: '/dashboards/groups',
      icon: 'dashboard'
    }
  ],
  [
    MenuId.dashboard_shared,
    {
      id: MenuId.dashboard_shared,
      name: 'dashboard.shared',
      fullName: 'entity-group.shared-dashboard-groups',
      type: 'link',
      path: '/dashboards/shared',
      icon: 'dashboard',
      rootOnly: true
    }
  ],
  [
    MenuId.solution_templates,
    {
      id: MenuId.solution_templates,
      name: 'solution-template.solution-templates',
      type: 'link',
      path: '/solutionTemplates',
      icon: 'apps',
      isNew: true
    }
  ],
  [
    MenuId.device_all,
    {
      id: MenuId.device_all,
      name: 'device.all',
      fullName: 'device.all-devices',
      type: 'link',
      path: '/entities/devices/all',
      icon: 'devices_other'
    }
  ],
  [
    MenuId.device_groups,
    {
      id: MenuId.device_groups,
      name: 'device.groups',
      fullName: 'entity-group.device-groups',
      type: 'link',
      path: '/entities/devices/groups',
      icon: 'devices_other'
    }
  ],
  [
    MenuId.device_shared,
    {
      id: MenuId.device_shared,
      name: 'device.shared',
      fullName: 'entity-group.shared-device-groups',
      type: 'link',
      path: '/entities/devices/shared',
      icon: 'devices_other',
      rootOnly: true
    }
  ],
  [
    MenuId.asset_all,
    {
      id: MenuId.asset_all,
      name: 'asset.all',
      fullName: 'asset.all-assets',
      type: 'link',
      path: '/entities/assets/all',
      icon: 'domain'
    }
  ],
  [
    MenuId.asset_groups,
    {
      id: MenuId.asset_groups,
      name: 'asset.groups',
      fullName: 'entity-group.asset-groups',
      type: 'link',
      path: '/entities/assets/groups',
      icon: 'domain'
    }
  ],
  [
    MenuId.asset_shared,
    {
      id: MenuId.asset_shared,
      name: 'asset.shared',
      fullName: 'entity-group.shared-asset-groups',
      type: 'link',
      path: '/entities/assets/shared',
      icon: 'domain',
      rootOnly: true
    }
  ],
  [
    MenuId.entity_view_all,
    {
      id: MenuId.entity_view_all,
      name: 'entity-view.all',
      fullName: 'entity-view.all-entity-views',
      type: 'link',
      path: '/entities/entityViews/all',
      icon: 'view_quilt'
    }
  ],
  [
    MenuId.entity_view_groups,
    {
      id: MenuId.entity_view_groups,
      name: 'entity-view.groups',
      fullName: 'entity-group.entity-view-groups',
      type: 'link',
      path: '/entities/entityViews/groups',
      icon: 'view_quilt'
    }
  ],
  [
    MenuId.entity_view_shared,
    {
      id: MenuId.entity_view_shared,
      name: 'entity-view.shared',
      fullName: 'entity-group.shared-entity-view-groups',
      type: 'link',
      path: '/entities/entityViews/shared',
      icon: 'view_quilt',
      rootOnly: true
    }
  ],
  [
    MenuId.customer_all,
    {
      id: MenuId.customer_all,
      name: 'customer.all',
      fullName: 'customer.all-customers',
      type: 'link',
      path: '/customers/all',
      icon: 'supervisor_account'
    }
  ],
  [
    MenuId.customer_groups,
    {
      id: MenuId.customer_groups,
      name: 'customer.groups',
      fullName: 'entity-group.customer-groups',
      type: 'link',
      path: '/customers/groups',
      icon: 'supervisor_account'
    }
  ],
  [
    MenuId.customer_shared,
    {
      id: MenuId.customer_shared,
      name: 'customer.shared',
      fullName: 'entity-group.shared-customer-groups',
      type: 'link',
      path: '/customers/shared',
      icon: 'supervisor_account',
      rootOnly: true
    }
  ],
  [
    MenuId.customers_hierarchy,
    {
      id: MenuId.customers_hierarchy,
      name: 'customer.hierarchy',
      fullName: 'customers-hierarchy.customers-hierarchy',
      type: 'link',
      path: '/customers/hierarchy',
      icon: 'sort',
      rootOnly: true
    }
  ],
  [
    MenuId.users,
    {
      id: MenuId.users,
      name: 'user.users',
      type: 'link',
      path: '/users',
      icon: 'account_circle'
    }
  ],
  [
    MenuId.user_all,
    {
      id: MenuId.user_all,
      name: 'user.all',
      fullName: 'user.all-users',
      type: 'link',
      path: '/users/all',
      icon: 'account_circle'
    }
  ],
  [
    MenuId.user_groups,
    {
      id: MenuId.user_groups,
      name: 'user.groups',
      fullName: 'entity-group.user-groups',
      type: 'link',
      path: '/users/groups',
      icon: 'account_circle'
    }
  ],
  [
    MenuId.integrations_center,
    {
      id: MenuId.integrations_center,
      name: 'integration.integrations-center',
      type: 'toggle',
      path: '/integrationsCenter',
      icon: 'integration_instructions'
    }
  ],
  [
    MenuId.integrations,
    {
      id: MenuId.integrations,
      name: 'integration.integrations',
      type: 'link',
      path: '/integrationsCenter/integrations',
      icon: 'input'
    }
  ],
  [
    MenuId.converters,
    {
      id: MenuId.converters,
      name: 'converter.converters',
      type: 'link',
      path: '/integrationsCenter/converters',
      icon: 'transform'
    }
  ],
  [
    MenuId.edge_all,
    {
      id: MenuId.edge_all,
      name: 'edge.all',
      fullName: 'edge.all-edges',
      type: 'link',
      path: '/edgeManagement/instances/all',
      icon: 'router'
    }
  ],
  [
    MenuId.edge_groups,
    {
      id: MenuId.edge_groups,
      name: 'edge.groups',
      fullName: 'entity-group.edge-groups',
      type: 'link',
      path: '/edgeManagement/instances/groups',
      icon: 'router'
    }
  ],
  [
    MenuId.edge_shared,
    {
      id: MenuId.edge_shared,
      name: 'edge.shared',
      fullName: 'entity-group.shared-edge-groups',
      type: 'link',
      path: '/edgeManagement/instances/shared',
      icon: 'router',
      rootOnly: true
    }
  ],
  [
    MenuId.integration_templates,
    {
      id: MenuId.integration_templates,
      name: 'edge.integration-templates',
      fullName: 'edge.edge-integration-templates',
      type: 'link',
      path: '/edgeManagement/integrations',
      icon: 'input'
    }
  ],
  [
    MenuId.converter_templates,
    {
      id: MenuId.converter_templates,
      name: 'edge.converter-templates',
      fullName: 'edge.edge-converter-templates',
      type: 'link',
      path: '/edgeManagement/converters',
      icon: 'transform'
    }
  ],
  [
    MenuId.scheduler,
    {
      id: MenuId.scheduler,
      name: 'scheduler.scheduler',
      type: 'link',
      path: '/features/scheduler',
      icon: 'schedule'
    }
  ],
  [
    MenuId.roles,
    {
      id: MenuId.roles,
      name: 'role.roles',
      type: 'link',
      path: '/security-settings/roles',
      icon: 'security'
    }
  ],
  [
    MenuId.self_registration,
    {
      id: MenuId.self_registration,
      name: 'self-registration.self-registration',
      type: 'link',
      path: '/security-settings/selfRegistration',
      icon: 'group_add'
    }
  ],
  [
    MenuId.trendz_settings,
    {
      id: MenuId.trendz_settings,
      name: 'admin.trendz',
      fullName: 'admin.trendz-settings',
      type: 'link',
      path: '/settings/trendz',
      icon: 'trendz-settings'
    }
  ]
]);

const menuFilters = new Map<MenuId, MenuFilter>([
  [
    MenuId.alarms, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ALARM)
  ],
  [
    MenuId.dashboard_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)
  ],
  [
    MenuId.dashboard_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)
  ],
  [
    MenuId.dashboard_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)
  ],
  [
    MenuId.solution_templates, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)
  ],
  [
    MenuId.device_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.DEVICE)
  ],
  [
    MenuId.device_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)
  ],
  [
    MenuId.device_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)
  ],
  [
    MenuId.asset_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ASSET)
  ],
  [
    MenuId.asset_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)
  ],
  [
    MenuId.asset_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)
  ],
  [
    MenuId.entity_view_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)
  ],
  [
    MenuId.entity_view_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)
  ],
  [
    MenuId.entity_view_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)
  ],
  [
    MenuId.gateways, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE) &&
          (userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD) ||
            userPermissionsService.hasReadGenericPermission(Resource.WIDGET_TYPE))
  ],
  [
    MenuId.device_profiles, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)
  ],
  [
    MenuId.asset_profiles, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)
  ],
  [
    MenuId.customer_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.CUSTOMER)
  ],
  [
    MenuId.customer_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.customer_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.customers_hierarchy, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.user_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.USER)
  ],
  [
    MenuId.user_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)
  ],
  [
    MenuId.integrations, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)
  ],
  [
    MenuId.converters, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)
  ],
  [
    MenuId.rule_chains, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)
  ],
  [
    MenuId.edge_all, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasReadGenericPermission(Resource.EDGE)
  ],
  [
    MenuId.edge_groups, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)
  ],
  [
    MenuId.edge_shared, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)
  ],
  [
    MenuId.rulechain_templates, (authState, userPermissionsService) =>
      authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
      userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)
  ],
  [
    MenuId.integration_templates, (authState, userPermissionsService) =>
      authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
      userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)
  ],
  [
    MenuId.converter_templates, (authState, userPermissionsService) =>
           authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
           userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)
  ],
  [
    MenuId.otaUpdates, (authState, userPermissionsService) =>
           authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)
  ],
  [
    MenuId.version_control, (authState, userPermissionsService) =>
           authState.authUser.authority === Authority.TENANT_ADMIN &&
           userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.scheduler, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)
  ],
  [
    MenuId.widget_types, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.WIDGET_TYPE)
  ],
  [
    MenuId.widgets_bundles, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)
  ],
  [
    MenuId.resources_library, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)
  ],
  [
    MenuId.javascript_library, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)
  ],
  [
    MenuId.notification_sent, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_recipients, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_templates, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_rules, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.api_usage, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
            userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY)
  ],
  [
    MenuId.white_labeling, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.white_labeling_general, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.login_white_labeling, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.mail_templates, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.custom_translation, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.custom_menu, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.home_settings, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.mail_server, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.notification_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.repository_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.auto_commit_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.mobile_bundles, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP_BUNDLE)
  ],
  [
    MenuId.mobile_apps, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP)
  ],
  [
    MenuId.mobile_qr_code_widget, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP_SETTINGS)
  ],
  [
    MenuId.two_fa, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.roles, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.ROLE)
  ],
  [
    MenuId.clients, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.OAUTH2_CLIENT)
  ],
  [
    MenuId.domains, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.DOMAIN)
  ],
  [
    MenuId.self_registration, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.audit_log, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)
  ]
]);

export const defaultUserMenuMap = new Map<Authority, MenuReference[]>([
  [
    Authority.SYS_ADMIN,
    [
      {id: MenuId.home},
      {id: MenuId.tenants},
      {id: MenuId.tenant_profiles},
      {
        id: MenuId.resources,
        pages: [
          {
            id: MenuId.widget_library,
            pages: [
              {id: MenuId.widget_types},
              {id: MenuId.widgets_bundles}
            ]
          },
          {id: MenuId.images},
          {id: MenuId.scada_symbols},
          {id: MenuId.javascript_library},
          {id: MenuId.resources_library}
        ]
      },
      {
        id: MenuId.notifications_center,
        pages: [
          {id: MenuId.notification_inbox},
          {id: MenuId.notification_sent},
          {id: MenuId.notification_recipients},
          {id: MenuId.notification_templates},
          {id: MenuId.notification_rules}
        ]
      },
      {
        id: MenuId.mobile_center,
        pages: [
          {id: MenuId.mobile_bundles},
          {id: MenuId.mobile_apps},
          {id: MenuId.mobile_qr_code_widget}
        ]
      },
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.mail_templates},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.settings,
        pages: [
          {id: MenuId.general},
          {id: MenuId.mail_server},
          {id: MenuId.notification_settings},
          {id: MenuId.queues}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {id: MenuId.security_settings_general},
          {id: MenuId.two_fa},
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          }
        ]
      }
    ]
  ],
  [
    Authority.TENANT_ADMIN,
    [
      {id: MenuId.home},
      {id: MenuId.alarms},
      {
        id: MenuId.dashboards,
        pages: [
          {id: MenuId.dashboard_all},
          {id: MenuId.dashboard_groups},
          {id: MenuId.dashboard_shared}
        ]
      },
      {id: MenuId.solution_templates},
      {
        id: MenuId.entities,
        pages: [
          {
            id: MenuId.devices,
            pages: [
              {id: MenuId.device_all},
              {id: MenuId.device_groups},
              {id: MenuId.device_shared}
            ]
          },
          {
            id: MenuId.assets,
            pages: [
              {id: MenuId.asset_all},
              {id: MenuId.asset_groups},
              {id: MenuId.asset_shared}
            ]
          },
          {
            id: MenuId.entity_views,
            pages: [
              {id: MenuId.entity_view_all},
              {id: MenuId.entity_view_groups},
              {id: MenuId.entity_view_shared}
            ]
          },
          {id: MenuId.gateways}
        ]
      },
      {
        id: MenuId.profiles,
        pages: [
          {id: MenuId.device_profiles},
          {id: MenuId.asset_profiles}
        ]
      },
      {
        id: MenuId.customers,
        pages: [
          {id: MenuId.customer_all},
          {id: MenuId.customer_groups},
          {id: MenuId.customer_shared},
          {id: MenuId.customers_hierarchy}
        ]
      },
      {
        id: MenuId.users,
        pages: [
          {id: MenuId.user_all},
          {id: MenuId.user_groups}
        ]
      },
      {
        id: MenuId.integrations_center,
        pages: [
          {id: MenuId.integrations},
          {id: MenuId.converters}
        ]
      },
      {id: MenuId.rule_chains},
      {
        id: MenuId.edge_management,
        pages: [
          {
            id: MenuId.edges,
            pages: [
              {id: MenuId.edge_all},
              {id: MenuId.edge_groups},
              {id: MenuId.edge_shared},
            ]
          },
          {id: MenuId.rulechain_templates},
          {id: MenuId.integration_templates},
          {id: MenuId.converter_templates}
        ]
      },
      {
        id: MenuId.features,
        pages: [
          {id: MenuId.otaUpdates},
          {id: MenuId.version_control},
          {id: MenuId.scheduler}
        ]
      },
      {
        id: MenuId.resources,
        pages: [
          {
            id: MenuId.widget_library,
            pages: [
              {id: MenuId.widget_types},
              {id: MenuId.widgets_bundles}
            ]
          },
          {id: MenuId.images},
          {id: MenuId.scada_symbols},
          {id: MenuId.javascript_library},
          {id: MenuId.resources_library}
        ]
      },
      {
        id: MenuId.notifications_center,
        pages: [
          {id: MenuId.notification_inbox},
          {id: MenuId.notification_sent},
          {id: MenuId.notification_recipients},
          {id: MenuId.notification_templates},
          {id: MenuId.notification_rules}
        ]
      },
      {
        id: MenuId.mobile_center,
        pages: [
          {id: MenuId.mobile_bundles},
          {id: MenuId.mobile_apps},
          {id: MenuId.mobile_qr_code_widget}
        ]
      },
      {id: MenuId.api_usage},
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.mail_templates},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.settings,
        pages: [
          {id: MenuId.home_settings},
          {id: MenuId.mail_server},
          {id: MenuId.notification_settings},
          {id: MenuId.repository_settings},
          {id: MenuId.auto_commit_settings},
          {id: MenuId.trendz_settings}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {id: MenuId.two_fa},
          {id: MenuId.roles},
          {id: MenuId.self_registration},
          {id: MenuId.audit_log},
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          }
        ]
      }
    ]
  ],
  [
    Authority.CUSTOMER_USER,
    [
      {id: MenuId.home},
      {id: MenuId.alarms},
      {
        id: MenuId.dashboards,
        pages: [
          {id: MenuId.dashboard_all},
          {id: MenuId.dashboard_groups},
          {id: MenuId.dashboard_shared}
        ]
      },
      {
        id: MenuId.entities,
        pages: [
          {
            id: MenuId.devices,
            pages: [
              {id: MenuId.device_all},
              {id: MenuId.device_groups},
              {id: MenuId.device_shared}
            ]
          },
          {
            id: MenuId.assets,
            pages: [
              {id: MenuId.asset_all},
              {id: MenuId.asset_groups},
              {id: MenuId.asset_shared}
            ]
          },
          {
            id: MenuId.entity_views,
            pages: [
              {id: MenuId.entity_view_all},
              {id: MenuId.entity_view_groups},
              {id: MenuId.entity_view_shared}
            ]
          }
        ]
      },
      {
        id: MenuId.customers,
        pages: [
          {id: MenuId.customer_all},
          {id: MenuId.customer_groups},
          {id: MenuId.customer_shared},
          {id: MenuId.customers_hierarchy}
        ]
      },
      {
        id: MenuId.users,
        pages: [
          {id: MenuId.user_all},
          {id: MenuId.user_groups}
        ]
      },
      {
        id: MenuId.edge_instances,
        pages: [
          {id: MenuId.edge_all},
          {id: MenuId.edge_groups},
          {id: MenuId.edge_shared},
        ]
      },
      {
        id: MenuId.resources,
        pages: [
          {id: MenuId.images},
          {id: MenuId.scada_symbols}
        ]
      },
      {
        id: MenuId.notifications_center,
        pages: [
          {id: MenuId.notification_inbox}
        ]
      },
      {id: MenuId.scheduler},
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.settings,
        pages: [
          {id: MenuId.home_settings}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {id: MenuId.roles},
          {id: MenuId.audit_log},
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          }
        ]
      }
    ]
  ]
]);

const defaultHomeSectionMap = new Map<Authority, HomeSectionReference[]>([
  [
    Authority.SYS_ADMIN,
    [
      {
        name: 'tenant.management',
        places: [MenuId.tenants, MenuId.tenant_profiles]
      },
      {
        name: 'widget.management',
        places: [MenuId.widget_library]
      },
      {
        name: 'admin.system-settings',
        places: [MenuId.general, MenuId.mail_server,
          MenuId.notification_settings, MenuId.security_settings, MenuId.oauth2, MenuId.domains, MenuId.mobile_apps,
          MenuId.clients, MenuId.two_fa, MenuId.resources_library, MenuId.queues]
      },
      {
        name: 'white-labeling.white-labeling',
        places: [MenuId.white_labeling_general, MenuId.login_white_labeling, MenuId.mail_templates]
      },
      {
        name: 'custom-translation.custom-translation',
        places: [MenuId.custom_translation]
      },
      {
        name: 'custom-menu.custom-menu',
        places: [MenuId.custom_menu]
      }
    ]
  ],
  [
    Authority.TENANT_ADMIN,
    [
      {
        name: 'solution-template.management',
        places: [MenuId.solution_templates]
      },
      {
        name: 'rulechain.management',
        places: [MenuId.rule_chains]
      },
      {
        name: 'converter.management',
        places: [MenuId.converters]
      },
      {
        name: 'integration.management',
        places: [MenuId.integrations]
      },
      {
        name: 'role.management',
        places: [MenuId.roles]
      },
      {
        name: 'user.management',
        places: [MenuId.users]
      },
      {
        name: 'customer.management',
        places: [MenuId.customers, MenuId.customers_hierarchy]
      },
      {
        name: 'asset.management',
        places: [MenuId.assets, MenuId.asset_profiles]
      },
      {
        name: 'device.management',
        places: [MenuId.devices, MenuId.device_profiles, MenuId.otaUpdates]
      },
      {
        name: 'entity-view.management',
        places: [MenuId.entity_views]
      },
      {
        name: 'edge.management',
        places: [MenuId.edges, MenuId.rulechain_templates, MenuId.converter_templates, MenuId.integration_templates]
      },
      {
        name: 'dashboard.management',
        places: [MenuId.widget_library, MenuId.dashboards]
      },
      {
        name: 'scheduler.management',
        places: [MenuId.scheduler]
      },
      {
        name: 'white-labeling.white-labeling',
        places: [MenuId.white_labeling_general, MenuId.login_white_labeling, MenuId.mail_templates]
      },
      {
        name: 'custom-translation.custom-translation',
        places: [MenuId.custom_translation]
      },
      {
        name: 'custom-menu.custom-menu',
        places: [MenuId.custom_menu]
      },
      {
        name: 'version-control.management',
        places: [MenuId.version_control]
      },
      {
        name: 'audit-log.audit',
        places: [MenuId.audit_log, MenuId.api_usage]
      },
      {
        name: 'admin.system-settings',
        places: [MenuId.home_settings, MenuId.mail_server, MenuId.notification_settings, MenuId.self_registration,
          MenuId.two_fa, MenuId.resources_library, MenuId.repository_settings, MenuId.auto_commit_settings, MenuId.trendz_settings]
      }
    ]
  ],
  [
    Authority.CUSTOMER_USER,
    [
      {
        name: 'role.management',
        places: [MenuId.roles]
      },
      {
        name: 'user.management',
        places: [MenuId.users]
      },
      {
        name: 'customer.management',
        places: [MenuId.customers, MenuId.customers_hierarchy]
      },
      {
        name: 'asset.management',
        places: [MenuId.assets]
      },
      {
        name: 'device.management',
        places: [MenuId.devices]
      },
      {
        name: 'entity-view.management',
        places: [MenuId.entity_views]
      },
      {
        name: 'edge.management',
        places: [MenuId.edge_instances]
      },
      {
        name: 'dashboard.management',
        places: [MenuId.dashboards]
      },
      {
        name: 'scheduler.management',
        places: [MenuId.scheduler]
      },
      {
        name: 'white-labeling.white-labeling',
        places: [MenuId.white_labeling_general, MenuId.login_white_labeling]
      },
      {
        name: 'custom-translation.custom-translation',
        places: [MenuId.custom_translation]
      },
      {
        name: 'custom-menu.custom-menu',
        places: [MenuId.custom_menu]
      },
      {
        name: 'audit-log.audit',
        places: [MenuId.audit_log]
      },
      {
        name: 'admin.system-settings',
        places: [MenuId.home_settings]
      }
    ]
  ]
]);

const referencesToMenuIdList = (references: MenuReference[]): Array<MenuId | string> => {
  const result: Array<MenuId | string> = [];
  for (const ref of references) {
    result.push(ref.id);
    if (ref.pages?.length) {
      result.push(...referencesToMenuIdList(ref.pages));
    }
  }
  return result;
};

export const buildUserMenu = (authState: AuthState, userPermissionsService: UserPermissionsService,
                              customMenuConfig: CustomMenuConfig): Array<MenuSection> => {
  if (customMenuConfig?.items?.length) {
    const customStateIds: {[stateId: string]: boolean} = {};
    const allowedMenuIds = referencesToMenuIdList(defaultUserMenuMap.get(authState.authUser.authority));
    return customMenuConfig.items.map(item =>
      menuItemToMenuSection(authState, userPermissionsService, allowedMenuIds, customStateIds, item)).filter(section => !!section);
  } else {
    const references = defaultUserMenuMap.get(authState.authUser.authority);
    return (references || []).map(ref =>
      referenceToMenuSection(authState, userPermissionsService, ref)).filter(section => !!section);
  }
};

export const buildUserHome = (authState: AuthState, availableMenuSections: MenuSection[]): Array<HomeSection> => {
  const references = defaultHomeSectionMap.get(authState.authUser.authority);
  return (references || []).map(ref =>
    homeReferenceToHomeSection(availableMenuSections, ref)).filter(section => !!section);
};

export const menuItemToMenuSection = (authState: AuthState,
                                      userPermissionsService: UserPermissionsService,
                                      allowedMenuIds: Array<MenuId | string>,
                                      customStateIds: {[stateId: string]: boolean},
                                      item: MenuItem): MenuSection | undefined => {
  if (isDefaultMenuItem(item)) {
    if (!filterMenuReference(authState, userPermissionsService, allowedMenuIds, item)) {
      return undefined;
    }
    if (isDefinedAndNotNull(item.visible) && !item.visible) {
      return undefined;
    }
    const section = menuSectionMap.get(item.id);
    const result = deepClone(section);
    if (isNotEmptyStr(item.icon)) {
      result.icon = item.icon;
    }
    if (isNotEmptyStr(item.name)) {
      result.name = item.name;
      result.customTranslate = true;
    }
    if (isHomeMenuItem(item)) {
      const type = item.homeType;
      switch (type) {
        case HomeMenuItemType.DEFAULT:
          break;
        case HomeMenuItemType.DASHBOARD:
          result.homeDashboardId = item.dashboardId;
          result.homeHideDashboardToolbar = item.hideDashboardToolbar;
          break;
      }
    }
    if (item.pages?.length) {
      result.pages = item.pages.map(page =>
        menuItemToMenuSection(authState, userPermissionsService, allowedMenuIds, customStateIds, page)).filter(page => !!page);
    }
    if (result.type === 'toggle' && !result.pages?.length) {
      return undefined;
    }
    return result;
  } else if (isCustomMenuItem(item)) {
    return buildCustomMenuSection(customStateIds, item);
  }
};

const referenceToMenuSection = (authState: AuthState, userPermissionsService: UserPermissionsService,
                                reference: MenuReference): MenuSection | undefined => {
  if (filterMenuReference(authState, userPermissionsService, [], reference)) {
    const section = menuSectionMap.get(MenuId[reference.id]);
    if (section) {
      const result = deepClone(section);
      if (reference.pages?.length) {
        result.pages = reference.pages.map(page =>
          referenceToMenuSection(authState, userPermissionsService, page)).filter(page => !!page);
      }
      return result;
    } else {
      return undefined;
    }
  } else {
    return undefined;
  }
};

const filterMenuReference = (authState: AuthState, userPermissionsService: UserPermissionsService,
                             allowedMenuIds: Array<MenuId | string>,
                             reference: MenuReference): boolean => {
  if (allowedMenuIds?.length && !allowedMenuIds.includes(reference.id)) {
    return false;
  }
  if (authState.authUser.authority === Authority.SYS_ADMIN) {
    return true;
  }
  const filter = menuFilters.get(MenuId[reference.id]);
  if (filter) {
    if (!filter(authState, userPermissionsService)) {
      return false;
    }
  }
  if (reference.pages?.length) {
    if (reference.pages.every(page => !filterMenuReference(authState, userPermissionsService, allowedMenuIds, page))) {
      return false;
    }
  }
  return true;
};

const buildCustomMenuSection = (stateIds: {[stateId: string]: boolean}, customMenuItem: CustomMenuItem): MenuSection | undefined => {
  if (isDefinedAndNotNull(customMenuItem.visible) && !customMenuItem.visible) {
    return undefined;
  }
  if (customMenuItem.menuItemType === CMItemType.SECTION &&
      !customMenuItem.pages?.length) {
    return undefined;
  }
  const stateId = getCustomMenuStateId(customMenuItem.name, stateIds);
  const customMenuSection = {
    id: stateId,
    isCustom: true,
    customTranslate: true,
    stateId,
    name: customMenuItem.name,
    icon: customMenuItem.icon
  } as MenuSection;
  if (customMenuItem.menuItemType === CMItemType.SECTION) {
    customMenuSection.type = 'toggle';
    const pages: MenuSection[] = [];
    const childStateIds: {[stateId: string]: boolean} = {};
    for (const customMenuChildItem of customMenuItem.pages) {
      if (isDefinedAndNotNull(customMenuChildItem.visible) && !customMenuChildItem.visible) {
        continue;
      }
      const childStateId = getCustomMenuStateId(customMenuChildItem.name, stateIds);
      const customMenuChildSection: MenuSection = {
        id: childStateId,
        isCustom: true,
        customTranslate: true,
        stateId: childStateId,
        name: customMenuChildItem.name,
        type: 'link',
        icon: customMenuChildItem.icon,
        path: '/iframeView/child'
      };
      customMenuChildSection.queryParams = {
        ...customMenuItemQueryParams(customMenuItem, stateId),
        ...customMenuItemQueryParams(customMenuChildItem, childStateId, true)
      };
      pages.push(customMenuChildSection);
      childStateIds[childStateId] = true;
    }
    if (!pages.length) {
      return undefined;
    }
    customMenuSection.pages = pages;
    customMenuSection.childStateIds = childStateIds;
  } else {
    customMenuSection.path = '/iframeView';
    customMenuSection.type = 'link';
    customMenuSection.queryParams = customMenuItemQueryParams(customMenuItem, stateId);
  }
  return customMenuSection;
};

const customMenuItemQueryParams = (item: CustomMenuItem, stateId: string, child = false):  {[k: string]: any} => {
  const queryParams: {[k: string]: any} = {};
  if (child) {
    queryParams.childStateId = stateId;
  } else {
    queryParams.stateId = stateId;
  }
  if (item.linkType === CMItemLinkType.URL) {
    if (child) {
      queryParams.childIframeUrl = item.url;
      queryParams.childSetAccessToken = item.setAccessToken;
    } else {
      queryParams.iframeUrl = item.url;
      queryParams.setAccessToken = item.setAccessToken;
    }
  } else if (item.linkType === CMItemLinkType.DASHBOARD) {
    if (child) {
      queryParams.childDashboardId = item.dashboardId;
      queryParams.childHideDashboardToolbar = item.hideDashboardToolbar;
    } else {
      queryParams.dashboardId = item.dashboardId;
      queryParams.hideDashboardToolbar = item.hideDashboardToolbar;
    }
  }
  return queryParams;
};

const getCustomMenuStateId = (name: string, stateIds: {[stateId: string]: boolean}): string => {
  const origName = (' ' + name).slice(1);
  let stateId = origName;
  let inc = 1;
  while (stateIds[stateId]) {
    stateId = origName + inc;
    inc++;
  }
  stateIds[stateId] = true;
  return stateId;
};

const homeReferenceToHomeSection = (availableMenuSections: MenuSection[], reference: HomeSectionReference): HomeSection | undefined => {
  const places = reference.places.map(id => availableMenuSections.find(m => m.id === id)).filter(p => !!p);
  if (places.length) {
    return {
      name: reference.name,
      places
    };
  } else {
    return undefined;
  }
};
