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
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import { HomeSection, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { EntityType } from '@shared/models/entity-type.models';
import { ActivationEnd, NavigationEnd, Params, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AuthState } from '@core/auth/auth.models';
import { CustomMenuItem } from '@shared/models/custom-menu.models';
import { guid } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  private homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);

  private currentMenuSections: Array<MenuSection> = [];
  private currentHomeSections: Array<HomeSection> = [];

  private currentCustomSection: MenuSection = null;
  private currentCustomChildSection: MenuSection = null;

  constructor(private store: Store<AppState>,
              private router: Router,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.customMenuService.customMenuChanged$.subscribe(() => {
      this.buildMenu();
    });
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(() => {
      this.updateCurrentCustomSection();
    });
  }

  private buildMenu() {
    this.currentMenuSections.length = 0;
    this.currentHomeSections.length = 0;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          const customMenu = this.customMenuService.getCustomMenu();
          let disabledItems: string[] = [];
          if (customMenu && customMenu.disabledMenuItems) {
            disabledItems = customMenu.disabledMenuItems;
          }
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              this.currentMenuSections = this.buildSysAdminMenu(authState, disabledItems);
              this.currentHomeSections = this.buildSysAdminHome(authState, disabledItems);
              break;
            case Authority.TENANT_ADMIN:
              this.currentMenuSections = this.buildTenantAdminMenu(authState, disabledItems);
              this.currentHomeSections = this.buildTenantAdminHome(authState, disabledItems);
              break;
            case Authority.CUSTOMER_USER:
              this.currentMenuSections = this.buildCustomerUserMenu(authState, disabledItems);
              this.currentHomeSections = this.buildCustomerUserHome(authState, disabledItems);
              break;
          }
          let customMenuItems: CustomMenuItem[] = [];
          if (customMenu && customMenu.menuItems) {
            customMenuItems = customMenu.menuItems;
          }
          this.buildCustomMenu(customMenuItems);
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          this.homeSections$.next(this.currentHomeSections);
        }
      }
    );
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    this.currentMenuSections.filter(section => section.type === 'toggle' &&
      (url.startsWith(section.path) || openedMenuSections.includes(section.path))).forEach(
      section => section.opened = true
    );
  }

  private buildSysAdminMenu(authState: AuthState, disabledItems: string[]): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      },
      {
        id: guid(),
        name: 'tenant.tenants',
        type: 'link',
        path: '/tenants',
        icon: 'supervisor_account',
        disabled: disabledItems.indexOf('tenants') > -1
      },
      {
        id: guid(),
        name: 'tenant-profile.tenant-profiles',
        type: 'link',
        path: '/tenantProfiles',
        icon: 'mdi:alpha-t-box',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('tenant_profiles') > -1
      },
      {
        id: guid(),
        name: 'admin.resources',
        type: 'toggle',
        path: '/resources',
        icon: 'folder',
        pages: [
          {
            id: guid(),
            name: 'widget.widget-library',
            type: 'link',
            path: '/resources/widgets-bundles',
            icon: 'now_widgets',
            disabled: disabledItems.indexOf('widget_library') > -1
          },
          {
            id: guid(),
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split',
            isMdiIcon: true,
            disabled: disabledItems.indexOf('resources_library') > -1
          }
        ]
      }
    );

    const notificationPages: Array<MenuSection> = [{
        id: guid(),
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox',
        disabled: disabledItems.indexOf('notification_inbox') > -1
      },
      {
        id: guid(),
        name: 'notification.sent',
        type: 'link',
        path: '/notification/sent',
        icon: 'outbox',
        disabled: disabledItems.indexOf('notification_sent') > -1
      },
      {
        id: guid(),
        name: 'notification.templates',
        type: 'link',
        path: '/notification/templates',
        icon: 'mdi:message-draw',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('notification_templates') > -1
      },
      {
        id: guid(),
        name: 'notification.recipients',
        type: 'link',
        path: '/notification/recipients',
        icon: 'contacts',
        disabled: disabledItems.indexOf('notification_recipients') > -1
      },
      {
        id: guid(),
        name: 'notification.rules',
        type: 'link',
        path: '/notification/rules',
        icon: 'mdi:message-cog',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('notification_rules') > -1
      }
    ];
    sections.push(
      {
        id: guid(),
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        isMdiIcon: true,
        pages: notificationPages,
        disabled: disabledItems.indexOf('notifications_center') > -1
      }
    );

    const whiteLabelPages: Array<MenuSection> = [
      {
        id: guid(),
        name: 'white-labeling.general',
        type: 'link',
        path: '/white-labeling/whiteLabel',
        icon: 'format_paint',
        disabled: disabledItems.indexOf('white_labeling_general') > -1
      },
      {
        id: guid(),
        name: 'white-labeling.login',
        type: 'link',
        path: '/white-labeling/loginWhiteLabel',
        icon: 'format_paint',
        disabled: disabledItems.indexOf('login_white_labeling') > -1
      },
      {
        id: guid(),
        name: 'admin.mail-templates',
        type: 'link',
        path: '/white-labeling/mail-template',
        icon: 'format_shapes',
        disabled: disabledItems.indexOf('mail_templates') > -1
      },
      {
        id: guid(),
        name: 'custom-translation.custom-translation',
        type: 'link',
        path: '/white-labeling/customTranslation',
        icon: 'language',
        disabled: disabledItems.indexOf('custom_translation') > -1
      },
      {
        id: guid(),
        name: 'custom-menu.custom-menu',
        type: 'link',
        path: '/white-labeling/customMenu',
        icon: 'list',
        disabled: disabledItems.indexOf('custom_menu') > -1
      }
    ];

    const whiteLabelSection: MenuSection = {
      id: guid(),
      name: 'white-labeling.white-labeling',
      type: 'link',
      path: '/white-labeling',
      icon: 'format_paint',
      pages: whiteLabelPages,
      disabled: disabledItems.indexOf('white_labeling') > -1
    };
    sections.push(whiteLabelSection);

    const settingPages: Array<MenuSection> = [
      {
        id: guid(),
        name: 'admin.general',
        type: 'link',
        path: '/settings/general',
        icon: 'settings_applications',
        disabled: disabledItems.indexOf('general') > -1
      },
      {
        id: guid(),
        name: 'admin.outgoing-mail',
        type: 'link',
        path: '/settings/outgoing-mail',
        icon: 'mail',
        disabled: disabledItems.indexOf('mail_server') > -1
      },
      {
        id: guid(),
        name: 'admin.notifications',
        type: 'link',
        path: '/settings/notifications',
        icon: 'sms',
        disabled: disabledItems.indexOf('sms_provider') > -1 || disabledItems.indexOf('notification_settings') > -1
      },
      {
        id: guid(),
        name: 'notification.notification',
        type: 'link',
        path: '/settings/notification',
        icon: 'notifications',
        disabled: disabledItems.indexOf('notification') > -1
      },
      {
        id: guid(),
        name: 'admin.queues',
        type: 'link',
        path: '/settings/queues',
        icon: 'swap_calls',
        disabled: disabledItems.indexOf('queues') > -1
      }
    ];

    const settingSection: MenuSection = {
      id: guid(),
      name: 'admin.settings',
      type: 'link',
      path: '/settings',
      icon: 'settings',
      pages: settingPages,
      disabled: disabledItems.indexOf('settings') > -1
    };
    sections.push(settingSection);

    const securitySettingPages: Array<MenuSection> = [
      {
        id: guid(),
        name: 'admin.general',
        type: 'link',
        path: '/security-settings/general',
        icon: 'settings_applications',
        disabled: disabledItems.indexOf('security_settings') > -1
      },
      {
        id: guid(),
        name: 'admin.2fa.2fa',
        type: 'link',
        path: '/security-settings/2fa',
        icon: 'mdi:two-factor-authentication',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('2fa') > -1
      },
      {
        id: guid(),
        name: 'admin.oauth2.oauth2',
        type: 'link',
        path: '/security-settings/oauth2',
        icon: 'mdi:shield-account',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('oauth2') > -1
      }
    ];

    const securitySettingSection: MenuSection = {
      id: guid(),
      name: 'security.security',
      type: 'toggle',
      path: '/security-settings',
      icon: 'security',
      pages: securitySettingPages,
      disabled: disabledItems.indexOf('security_settings') > -1
    };
    sections.push(securitySettingSection);

    return sections;
  }

  private buildSysAdminHome(authState: AuthState, disabledItems: string[]): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'tenant.management',
        places: [
          {
            name: 'tenant.tenants',
            icon: 'supervisor_account',
            path: '/tenants',
            disabled: disabledItems.indexOf('tenants') > -1
          },
          {
            name: 'tenant-profile.tenant-profiles',
            icon: 'mdi:alpha-t-box',
            isMdiIcon: true,
            path: '/tenantProfiles',
            disabled: disabledItems.indexOf('tenant_profiles') > -1
          }
        ]
      },
      {
        name: 'widget.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/widgets-bundles',
            disabled: disabledItems.indexOf('widget_library') > -1
          }
        ]
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.general',
            icon: 'settings_applications',
            path: '/settings/general',
            disabled: disabledItems.indexOf('general') > -1
          },
          {
            name: 'admin.outgoing-mail',
            icon: 'mail',
            path: '/settings/outgoing-mail',
            disabled: disabledItems.indexOf('mail_server') > -1
          },
          {
            name: 'admin.sms-provider',
            icon: 'sms',
            path: '/settings/sms-provider',
            disabled: disabledItems.indexOf('sms_provider') > -1
          },
          {
            name: 'notification.notification',
            icon: 'notifications',
            path: '/settings/notification',
            disabled: disabledItems.indexOf('notification') > -1
          },
          {
            name: 'admin.security-settings',
            icon: 'security',
            path: '/settings/security-settings',
            disabled: disabledItems.indexOf('security_settings') > -1
          },
          {
            name: 'admin.oauth2.oauth2',
            icon: 'security',
            path: '/settings/oauth2',
            disabled: disabledItems.indexOf('oauth2') > -1
          },
          {
            name: 'admin.2fa.2fa',
            icon: 'mdi:two-factor-authentication',
            isMdiIcon: true,
            path: '/settings/2fa',
            disabled: disabledItems.indexOf('2fa') > -1
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library',
            disabled: disabledItems.indexOf('resources_library') > -1
          },
          {
            name: 'admin.queues',
            icon: 'swap_calls',
            path: '/settings/queues',
            disabled: disabledItems.indexOf('resources_library') > -1
          }
        ]
      },
      {
        name: 'white-labeling.white-labeling',
        places: [
          {
            name: 'white-labeling.white-labeling',
            icon: 'format_paint',
            path: '/white-labeling/whiteLabel',
            disabled: disabledItems.indexOf('white_labeling') > -1
          },
          {
            name: 'white-labeling.login-white-labeling',
            icon: 'format_paint',
            path: '/white-labeling/loginWhiteLabel',
            disabled: disabledItems.indexOf('login_white_labeling') > -1
          },
          {
            name: 'admin.mail-templates',
            icon: 'format_shapes',
            path: '/white-labeling/mail-template',
            disabled: disabledItems.indexOf('mail_templates') > -1
          }
        ]
      },
      {
        name: 'custom-translation.custom-translation',
        places: [
          {
            name: 'custom-translation.custom-translation',
            icon: 'language',
            path: '/white-labeling/customTranslation',
            disabled: disabledItems.indexOf('custom_translation') > -1
          }
        ]
      },
      {
        name: 'custom-menu.custom-menu',
        places: [
          {
            name: 'custom-menu.custom-menu',
            icon: 'list',
            path: '/white-labeling/customMenu',
            disabled: disabledItems.indexOf('custom_menu') > -1
          }
        ]
      }
    );
    return homeSections;
  }

  private buildTenantAdminMenu(authState: AuthState, disabledItems: string[]): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ALARM)) {
      sections.push(
        {
          id: guid(),
          name: 'alarm.alarms',
          type: 'link',
          path: '/alarms',
          icon: 'notifications',
          disabled: disabledItems.indexOf('alarms') > -1
        }
      );
    }
    const dashboardPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.all',
          type: 'link',
          path: '/dashboards/all',
          icon: 'dashboards',
          disabled: disabledItems.indexOf('dashboard_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.groups',
          type: 'link',
          path: '/dashboards/groups',
          icon: 'dashboard',
          disabled: disabledItems.indexOf('dashboard_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.shared',
          type: 'link',
          path: '/dashboards/shared',
          icon: 'dashboard',
          rootOnly: true,
          disabled: disabledItems.indexOf('dashboard_shared') > -1
        }
      );
    }
    if (dashboardPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'dashboard.dashboards',
          type: 'link',
          path: '/dashboards',
          icon: 'dashboards',
          pages: dashboardPages,
          disabled: disabledItems.indexOf('dashboards') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
      sections.push(
        {
          id: guid(),
          name: 'solution-template.solution-templates',
          type: 'link',
          path: '/solutionTemplates',
          icon: 'apps',
          disabled: disabledItems.indexOf('solution_templates') > -1,
          isNew: true
        }
      );
    }
    const entityPages: Array<MenuSection> = [];
    const devicesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.all',
          type: 'link',
          path: '/entities/devices/all',
          icon: 'devices_other',
          disabled: disabledItems.indexOf('device_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.groups',
          type: 'link',
          path: '/entities/devices/groups',
          icon: 'devices_other',
          disabled: disabledItems.indexOf('device_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.shared',
          type: 'link',
          path: '/entities/devices/shared',
          icon: 'devices_other',
          rootOnly: true,
          disabled: disabledItems.indexOf('device_shared') > -1
        }
      );
    }
    if (devicesPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'device.devices',
          type: 'link',
          path: '/entities/devices',
          icon: 'devices_other',
          pages: devicesPages,
          disabled: disabledItems.indexOf('devices') > -1
        }
      );
    }
    const assetsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.all',
          type: 'link',
          path: '/entities/assets/all',
          icon: 'domain',
          disabled: disabledItems.indexOf('asset_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.groups',
          type: 'link',
          path: '/entities/assets/groups',
          icon: 'domain',
          disabled: disabledItems.indexOf('asset_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.shared',
          type: 'link',
          path: '/entities/assets/shared',
          icon: 'domain',
          rootOnly: true,
          disabled: disabledItems.indexOf('asset_shared') > -1
        }
      );
    }
    if (assetsPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'asset.assets',
          type: 'link',
          path: '/entities/assets',
          icon: 'domain',
          pages: assetsPages,
          disabled: disabledItems.indexOf('assets') > -1
        }
      );
    }
    const entityViewsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.all',
          type: 'link',
          path: '/entities/entityViews/all',
          icon: 'view_quilt',
          disabled: disabledItems.indexOf('entity_view_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.groups',
          type: 'link',
          path: '/entities/entityViews/groups',
          icon: 'view_quilt',
          disabled: disabledItems.indexOf('entity_view_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.shared',
          type: 'link',
          path: '/entities/entityViews/shared',
          icon: 'view_quilt',
          rootOnly: true,
          disabled: disabledItems.indexOf('entity_view_shared') > -1
        }
      );
    }
    if (entityViewsPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'entity-view.entity-views',
          type: 'link',
          path: '/entities/entityViews',
          icon: 'view_quilt',
          pages: entityViewsPages,
          disabled: disabledItems.indexOf('assets') > -1
        }
      );
    }
    if (entityPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'entity.entities',
          type: 'toggle',
          path: '/entities',
          icon: 'category',
          pages: entityPages,
          disabled: disabledItems.indexOf('entities') > -1
        }
      );
    }
    const profilePages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)) {
      profilePages.push(
        {
          id: guid(),
          name: 'device-profile.device-profiles',
          type: 'link',
          path: '/profiles/deviceProfiles',
          icon: 'mdi:alpha-d-box',
          isMdiIcon: true,
          disabled: disabledItems.indexOf('device_profiles') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)) {
      profilePages.push(
        {
          id: guid(),
          name: 'asset-profile.asset-profiles',
          type: 'link',
          path: '/profiles/assetProfiles',
          icon: 'mdi:alpha-a-box',
          isMdiIcon: true,
          disabled: disabledItems.indexOf('asset_profiles') > -1
        }
      );
    }
    if (profilePages.length) {
      sections.push(
        {
          id: guid(),
          name: 'profiles.profiles',
          type: 'toggle',
          path: '/profiles',
          icon: 'badge',
          pages: profilePages
        }
      );
    }

    const customerPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.all',
          type: 'link',
          path: '/customers/all',
          icon: 'supervisor_account',
          disabled: disabledItems.indexOf('customer_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.groups',
          type: 'link',
          path: '/customers/groups',
          icon: 'supervisor_account',
          disabled: disabledItems.indexOf('customer_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.shared',
          type: 'link',
          path: '/customers/shared',
          icon: 'supervisor_account',
          rootOnly: true,
          disabled: disabledItems.indexOf('customer_shared') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.hierarchy',
          type: 'link',
          path: '/customers/hierarchy',
          icon: 'sort',
          rootOnly: true,
          disabled: disabledItems.indexOf('customers_hierarchy') > -1
        }
      );
    }
    if (customerPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'customer.customers',
          type: 'link',
          path: '/customers',
          icon: 'supervisor_account',
          pages: customerPages,
          disabled: disabledItems.indexOf('customers') > -1
        }
      );
    }
    const userPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.USER)) {
      userPages.push(
        {
          id: guid(),
          name: 'user.all',
          type: 'link',
          path: '/users/all',
          icon: 'account_circle',
          disabled: disabledItems.indexOf('user_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)) {
      userPages.push(
        {
          id: guid(),
          name: 'user.groups',
          type: 'link',
          path: '/users/groups',
          icon: 'account_circle',
          disabled: disabledItems.indexOf('user_groups') > -1
        }
      );
    }
    if (userPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'user.users',
          type: 'link',
          path: '/users',
          icon: 'account_circle',
          pages: userPages,
          disabled: disabledItems.indexOf('users') > -1
        }
      );
    }
    const integrationPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
      integrationPages.push(
        {
          id: guid(),
          name: 'integration.integrations',
          type: 'link',
          path: '/integrationsCenter/integrations',
          icon: 'input',
          disabled: disabledItems.indexOf('integrations') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
      integrationPages.push(
        {
          id: guid(),
          name: 'converter.converters',
          type: 'link',
          path: '/integrationsCenter/converters',
          icon: 'transform',
          disabled: disabledItems.indexOf('converters') > -1
        }
      );
    }
    if (integrationPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'integration.integrations-center',
          type: 'toggle',
          path: '/integrationsCenter',
          icon: 'integration_instructions',
          pages: integrationPages,
          disabled: disabledItems.indexOf('integrations_center') > -1
        }
      );
    }
    const edgeManagementPages: Array<MenuSection> = [];
    if (authState.edgesSupportEnabled) {
      const edgesPages: Array<MenuSection> = [];
      if (this.userPermissionsService.hasReadGenericPermission(Resource.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.all',
            type: 'link',
            path: '/edgeManagement/instances/all',
            icon: 'router',
            disabled: disabledItems.indexOf('edge_all') > -1
          }
        );
      }
      if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.groups',
            type: 'link',
            path: '/edgeManagement/instances/groups',
            icon: 'router',
            disabled: disabledItems.indexOf('edge_groups') > -1
          }
        );
      }
      if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.shared',
            type: 'link',
            path: '/edgeManagement/instances/shared',
            icon: 'router',
            rootOnly: true,
            disabled: disabledItems.indexOf('edge_shared') > -1
          }
        );
      }
      if (edgesPages.length) {
        edgeManagementPages.push(
          {
            id: guid(),
            name: 'edge.instances',
            type: 'link',
            path: '/edgeManagement/instances',
            icon: 'router',
            pages: edgesPages,
            disabled: disabledItems.indexOf('edges') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
        edgeManagementPages.push(
          {
            id: guid(),
            name: 'edge.rulechain-templates',
            type: 'link',
            path: '/edgeManagement/ruleChains',
            icon: 'settings_ethernet',
            disabled: disabledItems.indexOf('rulechain_templates') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
        edgeManagementPages.push(
          {
            id: guid(),
            name: 'edge.integration-templates',
            type: 'link',
            path: '/edgeManagement/integrations',
            icon: 'input',
            disabled: disabledItems.indexOf('integration_templates') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
        edgeManagementPages.push(
          {
            id: guid(),
            name: 'edge.converter-templates',
            type: 'link',
            path: '/edgeManagement/converters',
            icon: 'transform',
            disabled: disabledItems.indexOf('converter_templates') > -1
          }
        );
      }
    }
    if (edgeManagementPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'edge.management',
          type: 'toggle',
          path: '/edgeManagement',
          icon: 'settings_input_antenna',
          pages: edgeManagementPages,
          disabled: disabledItems.indexOf('edge_management') > -1
        }
      );
    }
    const advancedFeaturesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
      advancedFeaturesPages.push(
        {
          id: guid(),
          name: 'rulechain.rulechains',
          type: 'link',
          path: '/features/ruleChains',
          icon: 'settings_ethernet',
          disabled: disabledItems.indexOf('rule_chains') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)) {
      advancedFeaturesPages.push(
        {
          id: guid(),
          name: 'ota-update.ota-updates',
          type: 'link',
          path: '/features/otaUpdates',
          icon: 'memory',
          disabled: disabledItems.indexOf('otaUpdates') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      advancedFeaturesPages.push(
        {
          id: guid(),
          name: 'version-control.version-control',
          type: 'link',
          path: '/features/vc',
          icon: 'history',
          disabled: disabledItems.indexOf('version_control') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      advancedFeaturesPages.push(
        {
          id: guid(),
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/features/scheduler',
          icon: 'schedule',
          disabled: disabledItems.indexOf('scheduler') > -1
        }
      );
    }
    if (advancedFeaturesPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'feature.advanced-features',
          type: 'toggle',
          path: '/features',
          icon: 'construction',
          pages: advancedFeaturesPages,
          disabled: disabledItems.indexOf('features') > -1
        }
      );
    }
    const resourcesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)) {
      resourcesPages.push(
        {
          id: guid(),
          name: 'widget.widget-library',
          type: 'link',
          path: '/resources/widgets-bundles',
          icon: 'now_widgets',
          disabled: disabledItems.indexOf('widget_library') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)) {
      resourcesPages.push({
        id: guid(),
        name: 'resource.resources-library',
        type: 'link',
        path: '/resources/resources-library',
        icon: 'mdi:rhombus-split',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('resources_library') > -1
      });
    }
    if (resourcesPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'admin.resources',
          type: 'toggle',
          path: '/resources',
          icon: 'folder',
          pages: resourcesPages,
          disabled: disabledItems.indexOf('resources') > -1
        }
      );
    }
    const notificationPages: Array<MenuSection> = [];
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox',
        disabled: disabledItems.indexOf('notification_inbox') > -1
      }
    );
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.sent',
        type: 'link',
        path: '/notification/sent',
        icon: 'outbox',
        disabled: disabledItems.indexOf('notification_sent') > -1
      }
    );
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.templates',
        type: 'link',
        path: '/notification/templates',
        icon: 'mdi:message-draw',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('notification_templates') > -1
      }
    );
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.recipients',
        type: 'link',
        path: '/notification/recipients',
        icon: 'contacts',
        disabled: disabledItems.indexOf('notification_recipients') > -1
      }
    );
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.rules',
        type: 'link',
        path: '/notification/rules',
        icon: 'mdi:message-cog',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('notification_rules') > -1
      }
    );
    if (notificationPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'notification.notification-center',
          type: 'link',
          path: '/notification',
          icon: 'mdi:message-badge',
          isMdiIcon: true,
          pages: notificationPages,
          disabled: disabledItems.indexOf('notifications_center') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
      this.userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY)) {
      sections.push(
        {
          id: guid(),
          name: 'api-usage.api-usage',
          type: 'link',
          path: '/usage',
          icon: 'insert_chart',
          disabled: disabledItems.indexOf('api_usage') > -1
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      const whiteLabelPages: Array<MenuSection> = [
        {
          id: guid(),
          name: 'white-labeling.general',
          type: 'link',
          path: '/white-labeling/whiteLabel',
          icon: 'format_paint',
          disabled: disabledItems.indexOf('white_labeling_general') > -1
        },
        {
          id: guid(),
          name: 'white-labeling.login',
          type: 'link',
          path: '/white-labeling/loginWhiteLabel',
          icon: 'format_paint',
          disabled: disabledItems.indexOf('login_white_labeling') > -1
        },
        {
          id: guid(),
          name: 'admin.mail-templates',
          type: 'link',
          path: '/white-labeling/mail-template',
          icon: 'format_shapes',
          disabled: disabledItems.indexOf('mail_templates') > -1
        },
        {
          id: guid(),
          name: 'custom-translation.custom-translation',
          type: 'link',
          path: '/white-labeling/customTranslation',
          icon: 'language',
          disabled: disabledItems.indexOf('custom_translation') > -1
        },
        {
          id: guid(),
          name: 'custom-menu.custom-menu',
          type: 'link',
          path: '/white-labeling/customMenu',
          icon: 'list',
          disabled: disabledItems.indexOf('custom_menu') > -1
        }
      ];
      sections.push(
        {
          id: guid(),
          name: 'white-labeling.white-labeling',
          type: 'link',
          path: '/white-labeling',
          icon: 'format_paint',
          pages: whiteLabelPages,
          disabled: disabledItems.indexOf('white_labeling') > -1
        }
      );
    }
    const settingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      settingPages.push(
        {
          id: guid(),
          name: 'admin.home-settings',
          type: 'link',
          path: '/settings/home',
          icon: 'settings_applications',
          disabled: disabledItems.indexOf('home_settings') > -1
        },
        {
          id: guid(),
          name: 'admin.outgoing-mail',
          type: 'link',
          path: '/settings/outgoing-mail',
          icon: 'mail',
          disabled: disabledItems.indexOf('mail_server') > -1
        },
        {
          id: guid(),
          name: 'admin.notifications',
          type: 'link',
          path: '/settings/notifications',
          icon: 'sms',
          disabled: disabledItems.indexOf('sms_provider') > -1 || disabledItems.indexOf('notification_settings') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      settingPages.push({
        id: guid(),
        name: 'admin.repository-settings',
        type: 'link',
        path: '/settings/repository',
        icon: 'manage_history',
        disabled: disabledItems.indexOf('repository_settings') > -1
      });
      settingPages.push({
        id: guid(),
        name: 'admin.auto-commit-settings',
        type: 'link',
        path: '/settings/auto-commit',
        icon: 'settings_backup_restore',
        disabled: disabledItems.indexOf('auto_commit_settings') > -1
      });
    }
    if (settingPages.length) {
      sections.push({
        id: guid(),
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: settingPages,
        disabled: disabledItems.indexOf('settings') > -1
      });
    }

    const securitySettingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      securitySettingPages.push({
        id: guid(),
        name: 'admin.2fa.2fa',
        type: 'link',
        path: '/security-settings/2fa',
        icon: 'mdi:two-factor-authentication',
        isMdiIcon: true,
        disabled: disabledItems.indexOf('2fa') > -1
      });
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      securitySettingPages.push(
        {
          id: guid(),
          name: 'role.roles',
          type: 'link',
          path: '/security-settings/roles',
          icon: 'security',
          disabled: disabledItems.indexOf('roles') > -1
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      securitySettingPages.push(
        {
          id: guid(),
          name: 'self-registration.self-registration',
          type: 'link',
          path: '/security-settings/selfRegistration',
          icon: 'group_add',
          disabled: disabledItems.indexOf('self_registration') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      securitySettingPages.push(
        {
          id: guid(),
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/security-settings/auditLogs',
          icon: 'track_changes',
          disabled: disabledItems.indexOf('audit_log') > -1
        }
      );
    }
    if (securitySettingPages.length) {
      sections.push({
        id: guid(),
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: securitySettingPages,
        disabled: disabledItems.indexOf('security_settings') > -1
      });
    }
    return sections;
  }

  private buildTenantAdminHome(authState: AuthState, disabledItems: string[]): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
      homeSections.push(
        {
          name: 'solution-template.management',
          places: [
            {
              name: 'solution-template.solution-templates',
              icon: 'apps',
              path: '/solutionTemplates',
              disabled: disabledItems.indexOf('solution_templates') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
      homeSections.push(
        {
          name: 'rulechain.management',
          places: [
            {
              name: 'rulechain.rulechains',
              icon: 'settings_ethernet',
              path: '/ruleChains',
              disabled: disabledItems.indexOf('rule_chains') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
      homeSections.push(
        {
          name: 'converter.management',
          places: [
            {
              name: 'converter.converters',
              icon: 'transform',
              path: '/converters',
              disabled: disabledItems.indexOf('converters') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
      homeSections.push(
        {
          name: 'integration.management',
          places: [
            {
              name: 'integration.integrations',
              icon: 'input',
              path: '/integrations',
              disabled: disabledItems.indexOf('integrations') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      homeSections.push(
        {
          name: 'role.management',
          places: [
            {
              name: 'role.roles',
              icon: 'security',
              path: '/roles',
              disabled: disabledItems.indexOf('roles') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.USER)) {
      homeSections.push(
        {
          name: 'user.management',
          places: [
            {
              name: 'user.users',
              icon: 'account_circle',
              path: '/userGroups',
              disabled: disabledItems.indexOf('user_groups') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      homeSections.push(
        {
          name: 'customer.management',
          places: [
            {
              name: 'customer.customers',
              icon: 'supervisor_account',
              path: '/customerGroups',
              disabled: disabledItems.indexOf('customer_groups') > -1
            },
            {
              name: 'customers-hierarchy.customers-hierarchy',
              icon: 'sort',
              path: '/customersHierarchy',
              disabled: disabledItems.indexOf('customers_hierarchy') > -1
            }
          ]
        }
      );
    }

    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ASSET) ||
      this.userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)) {
      const assetManagementSection: HomeSection = {
        name: 'asset.management',
        places: []
      };
      homeSections.push(assetManagementSection);
      if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ASSET)) {
        assetManagementSection.places.push(
          {
            name: 'asset.assets',
            icon: 'domain',
            path: '/assetGroups',
            disabled: disabledItems.indexOf('asset_groups') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)) {
        assetManagementSection.places.push(
          {
            name: 'asset-profile.asset-profiles',
            icon: 'mdi:alpha-a-box',
            isMdiIcon: true,
            path: '/profiles/assetProfiles',
            disabled: disabledItems.indexOf('asset_profiles') > -1
          }
        );
      }
    }

    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DEVICE) ||
      this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)) {
      const deviceManagementSection: HomeSection = {
        name: 'device.management',
        places: []
      };
      homeSections.push(deviceManagementSection);
      if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DEVICE)) {
        deviceManagementSection.places.push(
          {
            name: 'device.devices',
            icon: 'devices_other',
            path: '/deviceGroups',
            disabled: disabledItems.indexOf('device_groups') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)) {
        deviceManagementSection.places.push(
          {
            name: 'device-profile.device-profiles',
            icon: 'mdi:alpha-d-box',
            isMdiIcon: true,
            path: '/profiles/deviceProfiles',
            disabled: disabledItems.indexOf('device_profiles') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)) {
        deviceManagementSection.places.push(
          {
            name: 'ota-update.ota-updates',
            icon: 'memory',
            path: '/otaUpdates',
            disabled: disabledItems.indexOf('otaUpdates') > -1
          }
        );
      }
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      homeSections.push(
        {
          name: 'entity-view.management',
          places: [
            {
              name: 'entity-view.entity-views',
              icon: 'view_quilt',
              path: '/entityViewGroups',
              disabled: disabledItems.indexOf('entity_view_groups') > -1
            }
          ]
        }
      );
    }
    if (authState.edgesSupportEnabled && this.userPermissionsService.hasReadGroupsPermission(EntityType.EDGE)) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'router',
              path: '/edgeGroups',
              disabled: disabledItems.indexOf('edge_groups') > -1
            },
            {
              name: 'edge.rulechain-templates',
              icon: 'settings_ethernet',
              path: '/edgeManagement/ruleChains',
              disabled: disabledItems.indexOf('rulechain_templates') > -1
            },
            {
              name: 'edge.converter-templates',
              icon: 'transform',
              path: '/edgeManagement/converters',
              disabled: disabledItems.indexOf('converter_templates') > -1
            },
            {
              name: 'edge.integration-templates',
              icon: 'input',
              path: '/edgeManagement/integrations',
              disabled: disabledItems.indexOf('integration_templates') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD) ||
      this.userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)) {
      const dashboardManagement: HomeSection = {
        name: 'dashboard.management',
        places: []
      };
      homeSections.push(
        dashboardManagement
      );
      if (this.userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)) {
        dashboardManagement.places.push(
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/widgets-bundles',
            disabled: disabledItems.indexOf('widget_library') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD)) {
        dashboardManagement.places.push(
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboardGroups',
            disabled: disabledItems.indexOf('dashboard_groups') > -1
          }
        );
      }
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      homeSections.push(
        {
          name: 'scheduler.management',
          places: [
            {
              name: 'scheduler.scheduler',
              icon: 'schedule',
              path: '/scheduler',
              disabled: disabledItems.indexOf('scheduler') > -1
            }
          ]
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      homeSections.push(
        {
          name: 'white-labeling.white-labeling',
          places: [
            {
              name: 'white-labeling.white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/whiteLabel',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/loginWhiteLabel',
              disabled: disabledItems.indexOf('login_white_labeling') > -1
            },
            {
              name: 'admin.mail-templates',
              icon: 'format_shapes',
              path: '/white-labeling/mail-template',
              disabled: disabledItems.indexOf('mail_templates') > -1
            }
          ]
        }
      );
      homeSections.push(
        {
          name: 'custom-translation.custom-translation',
          places: [
            {
              name: 'custom-translation.custom-translation',
              icon: 'language',
              path: '/white-labeling/customTranslation',
              disabled: disabledItems.indexOf('custom_translation') > -1
            }
          ]
        }
      );
      homeSections.push(
        {
          name: 'custom-menu.custom-menu',
          places: [
            {
              name: 'custom-menu.custom-menu',
              icon: 'list',
              path: '/white-labeling/customMenu',
              disabled: disabledItems.indexOf('custom_menu') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      homeSections.push(
        {
          name: 'version-control.management',
          places: [
            {
              name: 'version-control.version-control',
              icon: 'history',
              path: '/vc',
              disabled: disabledItems.indexOf('version_control') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG) ||
      (this.userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
      this.userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY))) {

      const audit: HomeSection = {
        name: 'audit-log.audit',
        places: []
      };
      homeSections.push(
        audit
      );
      if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
        audit.places.push(
          {
            name: 'audit-log.audit-logs',
            icon: 'track_changes',
            path: '/auditLogs',
            disabled: disabledItems.indexOf('audit_log') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
        this.userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY)) {
        audit.places.push(
          {
            name: 'api-usage.api-usage',
            icon: 'insert_chart',
            path: '/usage',
            disabled: disabledItems.indexOf('api_usage') > -1
          }
        );
      }
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING) ||
      this.userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)) {
      const settings: HomeSection = {
        name: 'admin.system-settings',
        places: []
      };
      homeSections.push(
        settings
      );
      if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
        settings.places.push(
          {
            name: 'admin.home-settings',
            path: '/settings/home',
            icon: 'settings_applications',
            disabled: disabledItems.indexOf('home_settings') > -1
          },
          {
            name: 'admin.outgoing-mail',
            path: '/settings/outgoing-mail',
            icon: 'mail',
            disabled: disabledItems.indexOf('mail_server') > -1
          },
          {
            name: 'admin.sms-provider',
            path: '/settings/sms-provider',
            icon: 'sms',
            disabled: disabledItems.indexOf('sms_provider') > -1
          },
          {
            name: 'notification.notification',
            icon: 'notifications',
            path: '/settings/notification',
            disabled: disabledItems.indexOf('notification') > -1
          },
          {
            name: 'self-registration.self-registration',
            path: '/settings/selfRegistration',
            icon: 'group_add',
            disabled: disabledItems.indexOf('self_registration') > -1
          },
          {
            name: 'admin.2fa.2fa',
            path: '/settings/2fa',
            icon: 'mdi:two-factor-authentication',
            isMdiIcon: true,
            disabled: disabledItems.indexOf('2fa') > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)) {
        settings.places.push({
          name: 'resource.resources-library',
          path: '/settings/resources-library',
          icon: 'folder',
          disabled: disabledItems.indexOf('resources_library') > -1
        });
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
        settings.places.push({
          name: 'admin.repository-settings',
          path: '/settings/repository',
          icon: 'manage_history',
          disabled: disabledItems.indexOf('repository_settings') > -1
        });
        settings.places.push({
          name: 'admin.auto-commit-settings',
          path: '/settings/auto-commit',
          icon: 'settings_backup_restore',
          disabled: disabledItems.indexOf('auto_commit_settings') > -1
        });
      }
    }
    return homeSections;
  }

  private buildCustomerUserMenu(authState: AuthState, disabledItems: string[]): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ALARM)) {
      sections.push(
        {
          id: guid(),
          name: 'alarm.alarms',
          type: 'link',
          path: '/alarms',
          icon: 'notifications',
          disabled: disabledItems.indexOf('alarms') > -1
        }
      );
    }
    const dashboardPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.all',
          type: 'link',
          path: '/dashboards/all',
          icon: 'dashboards',
          disabled: disabledItems.indexOf('dashboard_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.groups',
          type: 'link',
          path: '/dashboards/groups',
          icon: 'dashboard',
          disabled: disabledItems.indexOf('dashboard_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: guid(),
          name: 'dashboard.shared',
          type: 'link',
          path: '/dashboards/shared',
          icon: 'dashboard',
          rootOnly: true,
          disabled: disabledItems.indexOf('dashboard_shared') > -1
        }
      );
    }
    if (dashboardPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'dashboard.dashboards',
          type: 'link',
          path: '/dashboards',
          icon: 'dashboards',
          pages: dashboardPages,
          disabled: disabledItems.indexOf('dashboards') > -1
        }
      );
    }
    const entityPages: Array<MenuSection> = [];
    const devicesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.all',
          type: 'link',
          path: '/entities/devices/all',
          icon: 'devices_other',
          disabled: disabledItems.indexOf('device_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.groups',
          type: 'link',
          path: '/entities/devices/groups',
          icon: 'devices_other',
          disabled: disabledItems.indexOf('device_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: guid(),
          name: 'device.shared',
          type: 'link',
          path: '/entities/devices/shared',
          icon: 'devices_other',
          rootOnly: true,
          disabled: disabledItems.indexOf('device_shared') > -1
        }
      );
    }
    if (devicesPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'device.devices',
          type: 'link',
          path: '/entities/devices',
          icon: 'devices_other',
          pages: devicesPages,
          disabled: disabledItems.indexOf('devices') > -1
        }
      );
    }
    const assetsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.all',
          type: 'link',
          path: '/entities/assets/all',
          icon: 'domain',
          disabled: disabledItems.indexOf('asset_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.groups',
          type: 'link',
          path: '/entities/assets/groups',
          icon: 'domain',
          disabled: disabledItems.indexOf('asset_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: guid(),
          name: 'asset.shared',
          type: 'link',
          path: '/entities/assets/shared',
          icon: 'domain',
          rootOnly: true,
          disabled: disabledItems.indexOf('asset_shared') > -1
        }
      );
    }
    if (assetsPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'asset.assets',
          type: 'link',
          path: '/entities/assets',
          icon: 'domain',
          pages: assetsPages,
          disabled: disabledItems.indexOf('assets') > -1
        }
      );
    }
    const entityViewsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.all',
          type: 'link',
          path: '/entities/entityViews/all',
          icon: 'view_quilt',
          disabled: disabledItems.indexOf('entity_view_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.groups',
          type: 'link',
          path: '/entities/entityViews/groups',
          icon: 'view_quilt',
          disabled: disabledItems.indexOf('entity_view_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: guid(),
          name: 'entity-view.shared',
          type: 'link',
          path: '/entities/entityViews/shared',
          icon: 'view_quilt',
          rootOnly: true,
          disabled: disabledItems.indexOf('entity_view_shared') > -1
        }
      );
    }
    if (entityViewsPages.length) {
      entityPages.push(
        {
          id: guid(),
          name: 'entity-view.entity-views',
          type: 'link',
          path: '/entities/entityViews',
          icon: 'view_quilt',
          pages: entityViewsPages,
          disabled: disabledItems.indexOf('assets') > -1
        }
      );
    }
    if (entityPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'entity.entities',
          type: 'toggle',
          path: '/entities',
          icon: 'category',
          pages: entityPages,
          disabled: disabledItems.indexOf('entities') > -1
        }
      );
    }
    const customerPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.all',
          type: 'link',
          path: '/customers/all',
          icon: 'supervisor_account',
          disabled: disabledItems.indexOf('customer_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.groups',
          type: 'link',
          path: '/customers/groups',
          icon: 'supervisor_account',
          disabled: disabledItems.indexOf('customer_groups') > -1
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.shared',
          type: 'link',
          path: '/customers/shared',
          icon: 'supervisor_account',
          rootOnly: true,
          disabled: disabledItems.indexOf('customer_shared') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: guid(),
          name: 'customer.hierarchy',
          type: 'link',
          path: '/customers/hierarchy',
          icon: 'sort',
          rootOnly: true,
          disabled: disabledItems.indexOf('customers_hierarchy') > -1
        }
      );
    }
    if (customerPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'customer.customers',
          type: 'link',
          path: '/customers',
          icon: 'supervisor_account',
          pages: customerPages,
          disabled: disabledItems.indexOf('customers') > -1
        }
      );
    }
    const userPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.USER)) {
      userPages.push(
        {
          id: guid(),
          name: 'user.all',
          type: 'link',
          path: '/users/all',
          icon: 'account_circle',
          disabled: disabledItems.indexOf('user_all') > -1
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)) {
      userPages.push(
        {
          id: guid(),
          name: 'user.groups',
          type: 'link',
          path: '/users/groups',
          icon: 'account_circle',
          disabled: disabledItems.indexOf('user_groups') > -1
        }
      );
    }
    if (userPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'user.users',
          type: 'link',
          path: '/users',
          icon: 'account_circle',
          pages: userPages,
          disabled: disabledItems.indexOf('users') > -1
        }
      );
    }
    if (authState.edgesSupportEnabled) {
      const edgesPages: Array<MenuSection> = [];
      if (this.userPermissionsService.hasReadGenericPermission(Resource.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.all',
            type: 'link',
            path: '/edgeManagement/instances/all',
            icon: 'router',
            disabled: disabledItems.indexOf('edge_all') > -1
          }
        );
      }
      if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.groups',
            type: 'link',
            path: '/edgeManagement/instances/groups',
            icon: 'router',
            disabled: disabledItems.indexOf('edge_groups') > -1
          }
        );
      }
      if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: guid(),
            name: 'edge.shared',
            type: 'link',
            path: '/edgeManagement/instances/shared',
            icon: 'router',
            rootOnly: true,
            disabled: disabledItems.indexOf('edge_shared') > -1
          }
        );
      }
      if (edgesPages.length) {
        sections.push(
          {
            id: guid(),
            name: 'edge.edge-instances',
            type: 'link',
            path: '/edgeManagement/instances',
            icon: 'router',
            pages: edgesPages,
            disabled: disabledItems.indexOf('edges') > -1
          }
        );
      }
    }
    const notificationPages: Array<MenuSection> = [];
    // TODO: permission check
    notificationPages.push(
      {
        id: guid(),
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox',
        disabled: disabledItems.indexOf('notification_inbox') > -1
      }
    );
    if (notificationPages.length) {
      sections.push(
        {
          id: guid(),
          name: 'notification.notification-center',
          type: 'link',
          path: '/notification',
          icon: 'mdi:message-badge',
          isMdiIcon: true,
          pages: notificationPages,
          disabled: disabledItems.indexOf('notifications_center') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      sections.push(
        {
          id: guid(),
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/features/scheduler',
          icon: 'schedule',
          disabled: disabledItems.indexOf('scheduler') > -1
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      const whiteLabelPages: Array<MenuSection> = [
        {
          id: guid(),
          name: 'white-labeling.general',
          type: 'link',
          path: '/white-labeling/whiteLabel',
          icon: 'format_paint',
          disabled: disabledItems.indexOf('white_labeling_general') > -1
        },
        {
          id: guid(),
          name: 'white-labeling.login',
          type: 'link',
          path: '/white-labeling/loginWhiteLabel',
          icon: 'format_paint',
          disabled: disabledItems.indexOf('login_white_labeling') > -1
        },
        {
          id: guid(),
          name: 'custom-translation.custom-translation',
          type: 'link',
          path: '/white-labeling/customTranslation',
          icon: 'language',
          disabled: disabledItems.indexOf('custom_translation') > -1
        },
        {
          id: guid(),
          name: 'custom-menu.custom-menu',
          type: 'link',
          path: '/white-labeling/customMenu',
          icon: 'list',
          disabled: disabledItems.indexOf('custom_menu') > -1
        }
      ];
      sections.push(
        {
          id: guid(),
          name: 'white-labeling.white-labeling',
          type: 'link',
          path: '/white-labeling',
          icon: 'format_paint',
          pages: whiteLabelPages,
          disabled: disabledItems.indexOf('white_labeling') > -1
        }
      );
    }
    const settingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      settingPages.push(
        {
          id: guid(),
          name: 'admin.home-settings',
          type: 'link',
          path: '/settings/home',
          icon: 'settings_applications',
          disabled: disabledItems.indexOf('home_settings') > -1
        }
      );
    }
    if (settingPages.length) {
      sections.push({
        id: guid(),
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: settingPages,
        disabled: disabledItems.indexOf('settings') > -1
      });
    }
    const securitySettingPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      securitySettingPages.push(
        {
          id: guid(),
          name: 'role.roles',
          type: 'link',
          path: '/security-settings/roles',
          icon: 'security',
          disabled: disabledItems.indexOf('roles') > -1
        }
      );
    }
   if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      securitySettingPages.push(
        {
          id: guid(),
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/security-settings/auditLogs',
          icon: 'track_changes',
          disabled: disabledItems.indexOf('audit_log') > -1
        }
      );
    }
    if (securitySettingPages.length) {
      sections.push({
        id: guid(),
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: securitySettingPages,
        disabled: disabledItems.indexOf('security_settings') > -1
      });
    }
    return sections;
  }

  private buildCustomerUserHome(authState: AuthState, disabledItems: string[]): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      homeSections.push(
        {
          name: 'role.management',
          places: [
            {
              name: 'role.roles',
              icon: 'security',
              path: '/roles',
              disabled: disabledItems.indexOf('roles') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.USER)) {
      homeSections.push(
        {
          name: 'user.management',
          places: [
            {
              name: 'user.users',
              icon: 'account_circle',
              path: '/userGroups',
              disabled: disabledItems.indexOf('user_groups') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      homeSections.push(
        {
          name: 'customer.management',
          places: [
            {
              name: 'customer.customers',
              icon: 'supervisor_account',
              path: '/customerGroups',
              disabled: disabledItems.indexOf('customer_groups') > -1
            },
            {
              name: 'customers-hierarchy.customers-hierarchy',
              icon: 'sort',
              path: '/customersHierarchy',
              disabled: disabledItems.indexOf('customers_hierarchy') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ASSET)) {
      homeSections.push(
        {
          name: 'asset.management',
          places: [
            {
              name: 'asset.assets',
              icon: 'domain',
              path: '/assetGroups',
              disabled: disabledItems.indexOf('asset_groups') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DEVICE)) {
      homeSections.push(
        {
          name: 'device.management',
          places: [
            {
              name: 'device.devices',
              icon: 'devices_other',
              path: '/deviceGroups',
              disabled: disabledItems.indexOf('device_groups') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      homeSections.push(
        {
          name: 'entity-view.management',
          places: [
            {
              name: 'entity-view.entity-views',
              icon: 'view_quilt',
              path: '/entityViewGroups',
              disabled: disabledItems.indexOf('entity_view_groups') > -1
            }
          ]
        }
      );
    }
    if (authState.edgesSupportEnabled && this.userPermissionsService.hasReadGroupsPermission(EntityType.EDGE)) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'router',
              path: '/edgeGroups',
              disabled: disabledItems.indexOf('edge_groups') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD)) {
      homeSections.push({
        name: 'dashboard.management',
        places: [
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboardGroups',
            disabled: disabledItems.indexOf('dashboard_groups') > -1
          }
        ]
      });
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      homeSections.push(
        {
          name: 'scheduler.management',
          places: [
            {
              name: 'scheduler.scheduler',
              icon: 'schedule',
              path: '/scheduler',
              disabled: disabledItems.indexOf('scheduler') > -1
            }
          ]
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      homeSections.push(
        {
          name: 'white-labeling.white-labeling',
          places: [
            {
              name: 'white-labeling.white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/whiteLabel',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/loginWhiteLabel',
              disabled: disabledItems.indexOf('login_white_labeling') > -1
            }
          ]
        }
      );
      homeSections.push(
        {
          name: 'custom-translation.custom-translation',
          places: [
            {
              name: 'custom-translation.custom-translation',
              icon: 'language',
              path: '/white-labeling/customTranslation',
              disabled: disabledItems.indexOf('custom_translation') > -1
            }
          ]
        }
      );
      homeSections.push(
        {
          name: 'custom-menu.custom-menu',
          places: [
            {
              name: 'custom-menu.custom-menu',
              icon: 'list',
              path: '/white-labeling/customMenu',
              disabled: disabledItems.indexOf('custom_menu') > -1
            }
          ]
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      homeSections.push(
        {
          name: 'audit-log.audit',
          places: [
            {
              name: 'audit-log.audit-logs',
              icon: 'track_changes',
              path: '/auditLogs',
              disabled: disabledItems.indexOf('audit_log') > -1
            }
          ]
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      homeSections.push(
        {
          name: 'admin.system-settings',
          places: [
            {
              name: 'admin.home-settings',
              icon: 'settings_applications',
              path: '/settings/home',
              disabled: disabledItems.indexOf('home_settings') > -1
            }
          ]
        }
      );
    }
    return homeSections;
  }

  private buildCustomMenu(customMenuItems: CustomMenuItem[]) {
    const stateIds: {[stateId: string]: boolean} = {};
    for (const customMenuItem of customMenuItems) {
      const stateId = this.getCustomMenuStateId(customMenuItem.name, stateIds);
      const customMenuSection = {
        isCustom: true,
        stateId,
        name: customMenuItem.name,
        icon: customMenuItem.materialIcon,
        iconUrl: customMenuItem.iconUrl,
        path: '/iframeView'
      } as MenuSection;
      customMenuSection.queryParams = {
        stateId,
        iframeUrl: customMenuItem.iframeUrl,
        dashboardId: customMenuItem.dashboardId,
        hideDashboardToolbar: customMenuItem.hideDashboardToolbar,
        setAccessToken: customMenuItem.setAccessToken
      };
      if (customMenuItem.childMenuItems && customMenuItem.childMenuItems.length) {
        customMenuSection.type = 'toggle';
        const pages: MenuSection[] = [];
        const childStateIds: {[stateId: string]: boolean} = {};
        for (const customMenuChildItem of customMenuItem.childMenuItems) {
          const childStateId = this.getCustomMenuStateId(customMenuChildItem.name, stateIds);
          const customMenuChildSection: MenuSection = {
            id: guid(),
            isCustom: true,
            stateId: childStateId,
            name: customMenuChildItem.name,
            type: 'link',
            icon: customMenuChildItem.materialIcon,
            iconUrl: customMenuChildItem.iconUrl,
            path: '/iframeView/child'
          };
          customMenuChildSection.queryParams = {
            stateId,
            iframeUrl: customMenuItem.iframeUrl,
            dashboardId: customMenuItem.dashboardId,
            hideDashboardToolbar: customMenuItem.hideDashboardToolbar,
            setAccessToken: customMenuItem.setAccessToken,
            childStateId,
            childIframeUrl: customMenuChildItem.iframeUrl,
            childDashboardId: customMenuChildItem.dashboardId,
            childHideDashboardToolbar: customMenuChildItem.hideDashboardToolbar,
            childSetAccessToken: customMenuChildItem.setAccessToken
          };
          pages.push(customMenuChildSection);
          childStateIds[childStateId] = true;
        }
        customMenuSection.pages = pages;
        customMenuSection.childStateIds = childStateIds;
      } else {
        customMenuSection.type = 'link';
      }
      this.currentMenuSections.push(customMenuSection);
    }
    this.updateCurrentCustomSection();
  }

  private getCustomMenuStateId(name: string, stateIds: {[stateId: string]: boolean}): string {
    const origName = (' ' + name).slice(1);
    let stateId = origName;
    let inc = 1;
    while (stateIds[stateId]) {
      stateId = origName + inc;
      inc++;
    }
    stateIds[stateId] = true;
    return stateId;
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

  public sectionActive(section: MenuSection): boolean {
    if (section.isCustom) {
      const queryParams = this.extractQueryParams();
      if (queryParams) {
        if (queryParams.childStateId) {
          return section.stateId === queryParams.childStateId ||
            (section.childStateIds && section.childStateIds[queryParams.childStateId]);
        } else if (queryParams.stateId) {
          return section.stateId === queryParams.stateId;
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return section.opened;
    }
  }

  public getCurrentCustomSection(): MenuSection {
    return this.currentCustomSection;
  }

  public getCurrentCustomChildSection(): MenuSection {
    return this.currentCustomChildSection;
  }

  private updateCurrentCustomSection() {
    const queryParams = this.extractQueryParams();
    this.currentCustomSection = this.detectCurrentCustomSection(queryParams);
    this.currentCustomChildSection = this.detectCurrentCustomChildSection(queryParams);
  }

  private detectCurrentCustomSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.stateId) {
      const stateId: string = queryParams.stateId;
      const found =
        this.currentMenuSections.find((section) => section.isCustom && section.stateId === stateId);
      if (found) {
        return found;
      }
    }
    return null;
  }

  private detectCurrentCustomChildSection(queryParams: Params): MenuSection {
    if (queryParams && queryParams.childStateId) {
      const stateId = queryParams.childStateId;
      for (const section of this.currentMenuSections) {
        if (section.isCustom && section.pages && section.pages.length) {
          const found =
            section.pages.find((childSection) => childSection.stateId === stateId);
          if (found) {
            return found;
          }
        }
      }
    }
    return null;
  }

  private extractQueryParams(): Params {
    const state = this.router.routerState;
    const snapshot =  state.snapshot;
    let lastChild = snapshot.root;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    return lastChild.queryParams;
  }

  public getRedirectPath(parentPath: string, redirectPath: string): Observable<string> {
    parentPath = '/' + parentPath.replace(/\./g, '/');
    if (!redirectPath.startsWith('/')) {
      redirectPath = `${parentPath}/${redirectPath}`;
    }
    return this.menuSections$.pipe(
      map((sections) => {
        const parentSection = this.findSectionByPath(sections, parentPath);
        if (parentSection) {
          if (parentSection.pages) {
            const childPages = parentSection.pages;
            const filteredPages = childPages.filter((page) => !page.disabled);
            if (filteredPages && filteredPages.length) {
              const redirectPage = filteredPages.filter((page) => page.path === redirectPath);
              if (!redirectPage || !redirectPage.length) {
                return filteredPages[0].path;
              }
            }
            return redirectPath;
          }
        }
        return redirectPath;
      })
    );
  }

  private findSectionByPath(sections: MenuSection[], sectionPath: string): MenuSection {
    for (const section of sections) {
      if (sectionPath === section.path && !section.disabled) {
        return section;
      }
      if (section.pages?.length) {
        const found = this.findSectionByPath(section.pages, sectionPath);
        if (found) {
          return found;
        }
      }
    }
    return null;
  }
}
