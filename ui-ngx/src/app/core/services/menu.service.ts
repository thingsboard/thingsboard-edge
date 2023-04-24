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
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { EntityType } from '@shared/models/entity-type.models';
import { ActivationEnd, NavigationEnd, Params, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AuthState } from '@core/auth/auth.models';
import { CustomMenuItem } from '@shared/models/custom-menu.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  private homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );
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
          const index = disabledItems.indexOf('sms_provider');
          if (index !== -1) {
            disabledItems[index] = 'notification_settings';
          }
          let menuSections: MenuSection[] = [];
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              menuSections = this.buildSysAdminMenu();
              this.currentHomeSections = this.buildSysAdminHome(authState, disabledItems);
              break;
            case Authority.TENANT_ADMIN:
              menuSections = this.buildTenantAdminMenu(authState);
              this.currentHomeSections = this.buildTenantAdminHome(authState, disabledItems);
              break;
            case Authority.CUSTOMER_USER:
              menuSections = this.buildCustomerUserMenu(authState);
              this.currentHomeSections = this.buildCustomerUserHome(authState, disabledItems);
              break;
          }
          this.currentMenuSections = this.updateDisabledItems(menuSections, disabledItems);
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

  private updateDisabledItems(sections: Array<MenuSection>, disabledItems: string[]): Array<MenuSection> {
    for (const section of sections) {
      section.disabled = disabledItems.indexOf(section.id) > -1;
      if (section.pages && section.pages.length) {
        this.updateDisabledItems(section.pages, disabledItems);
      }
    }
    return sections;
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    this.currentMenuSections.filter(section => section.type === 'toggle' &&
      (url.startsWith(section.path) || openedMenuSections.includes(section.path))).forEach(
      section => section.opened = true
    );
  }

  private buildSysAdminMenu(): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: 'home',
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      },
      {
        id: 'tenants',
        name: 'tenant.tenants',
        type: 'link',
        path: '/tenants',
        icon: 'supervisor_account'
      },
      {
        id: 'tenant_profiles',
        name: 'tenant-profile.tenant-profiles',
        type: 'link',
        path: '/tenantProfiles',
        icon: 'mdi:alpha-t-box',
        isMdiIcon: true
      },
      {
        id: 'resources',
        name: 'admin.resources',
        type: 'toggle',
        path: '/resources',
        icon: 'folder',
        pages: [
          {
            id: 'widget_library',
            name: 'widget.widget-library',
            type: 'link',
            path: '/resources/widgets-bundles',
            icon: 'now_widgets'
          },
          {
            id: 'resources_library',
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split',
            isMdiIcon: true
          }
        ]
      }
    );

    const notificationPages: Array<MenuSection> = [{
        id: 'notification_inbox',
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox'
      },
      {
        id: 'notification_sent',
        name: 'notification.sent',
        type: 'link',
        path: '/notification/sent',
        icon: 'outbox'
      },
      {
        id: 'notification_recipients',
        name: 'notification.recipients',
        type: 'link',
        path: '/notification/recipients',
        icon: 'contacts'
      },
      {
        id: 'notification_templates',
        name: 'notification.templates',
        type: 'link',
        path: '/notification/templates',
        icon: 'mdi:message-draw',
        isMdiIcon: true
      },
      {
        id: 'notification_rules',
        name: 'notification.rules',
        type: 'link',
        path: '/notification/rules',
        icon: 'mdi:message-cog',
        isMdiIcon: true
      }
    ];
    sections.push(
      {
        id: 'notifications_center',
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        isMdiIcon: true,
        pages: notificationPages
      }
    );

    const whiteLabelPages: Array<MenuSection> = [
      {
        id: 'white_labeling_general',
        name: 'white-labeling.general',
        type: 'link',
        path: '/white-labeling/whiteLabel',
        icon: 'format_paint'
      },
      {
        id: 'login_white_labeling',
        name: 'white-labeling.login',
        type: 'link',
        path: '/white-labeling/loginWhiteLabel',
        icon: 'format_paint'
      },
      {
        id: 'mail_templates',
        name: 'admin.mail-templates',
        type: 'link',
        path: '/white-labeling/mail-template',
        icon: 'format_shapes'
      },
      {
        id: 'custom_translation',
        name: 'custom-translation.custom-translation',
        type: 'link',
        path: '/white-labeling/customTranslation',
        icon: 'language'
      },
      {
        id: 'custom_menu',
        name: 'custom-menu.custom-menu',
        type: 'link',
        path: '/white-labeling/customMenu',
        icon: 'list'
      }
    ];

    const whiteLabelSection: MenuSection = {
      id: 'white_labeling',
      name: 'white-labeling.white-labeling',
      type: 'link',
      path: '/white-labeling',
      icon: 'format_paint',
      pages: whiteLabelPages
    };
    sections.push(whiteLabelSection);

    const settingPages: Array<MenuSection> = [
      {
        id: 'general',
        name: 'admin.general',
        type: 'link',
        path: '/settings/general',
        icon: 'settings_applications'
      },
      {
        id: 'mail_server',
        name: 'admin.outgoing-mail',
        type: 'link',
        path: '/settings/outgoing-mail',
        icon: 'mail'
      },
      {
        id: 'notification_settings',
        name: 'admin.notifications',
        type: 'link',
        path: '/settings/notifications',
        icon: 'sms'
      },
      {
        id: 'queues',
        name: 'admin.queues',
        type: 'link',
        path: '/settings/queues',
        icon: 'swap_calls'
      }
    ];

    const settingSection: MenuSection = {
      id: 'settings',
      name: 'admin.settings',
      type: 'link',
      path: '/settings',
      icon: 'settings',
      pages: settingPages
    };
    sections.push(settingSection);

    const securitySettingPages: Array<MenuSection> = [
      {
        id: 'security_settings',
        name: 'admin.general',
        type: 'link',
        path: '/security-settings/general',
        icon: 'settings_applications'
      },
      {
        id: '2fa',
        name: 'admin.2fa.2fa',
        type: 'link',
        path: '/security-settings/2fa',
        icon: 'mdi:two-factor-authentication',
        isMdiIcon: true
      },
      {
        id: 'oauth2',
        name: 'admin.oauth2.oauth2',
        type: 'link',
        path: '/security-settings/oauth2',
        icon: 'mdi:shield-account',
        isMdiIcon: true
      }
    ];

    const securitySettingSection: MenuSection = {
      id: 'security_settings',
      name: 'security.security',
      type: 'toggle',
      path: '/security-settings',
      icon: 'security',
      pages: securitySettingPages
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

  private buildTenantAdminMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: 'home',
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ALARM)) {
      sections.push(
        {
          id: 'alarms',
          name: 'alarm.alarms',
          type: 'link',
          path: '/alarms',
          icon: 'notifications'
        }
      );
    }
    const dashboardPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_all',
          name: 'dashboard.all',
          fullName: 'dashboard.all-dashboards',
          type: 'link',
          path: '/dashboards/all',
          icon: 'dashboards'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_groups',
          name: 'dashboard.groups',
          fullName: 'entity-group.dashboard-groups',
          type: 'link',
          path: '/dashboards/groups',
          icon: 'dashboard'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_shared',
          name: 'dashboard.shared',
          fullName: 'entity-group.shared-dashboard-groups',
          type: 'link',
          path: '/dashboards/shared',
          icon: 'dashboard',
          rootOnly: true
        }
      );
    }
    if (dashboardPages.length) {
      sections.push(
        {
          id: 'dashboards',
          name: 'dashboard.dashboards',
          type: 'link',
          path: '/dashboards',
          icon: 'dashboards',
          pages: dashboardPages
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
      sections.push(
        {
          id: 'solution_templates',
          name: 'solution-template.solution-templates',
          type: 'link',
          path: '/solutionTemplates',
          icon: 'apps',
          isNew: true
        }
      );
    }
    const entityPages: Array<MenuSection> = [];
    const devicesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_all',
          name: 'device.all',
          fullName: 'device.all-devices',
          type: 'link',
          path: '/entities/devices/all',
          icon: 'devices_other'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_groups',
          name: 'device.groups',
          fullName: 'entity-group.device-groups',
          type: 'link',
          path: '/entities/devices/groups',
          icon: 'devices_other'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_shared',
          name: 'device.shared',
          fullName: 'entity-group.shared-device-groups',
          type: 'link',
          path: '/entities/devices/shared',
          icon: 'devices_other',
          rootOnly: true
        }
      );
    }
    if (devicesPages.length) {
      entityPages.push(
        {
          id: 'devices',
          name: 'device.devices',
          type: 'link',
          path: '/entities/devices',
          icon: 'devices_other',
          pages: devicesPages
        }
      );
    }
    const assetsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_all',
          name: 'asset.all',
          fullName: 'asset.all-assets',
          type: 'link',
          path: '/entities/assets/all',
          icon: 'domain'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_groups',
          name: 'asset.groups',
          fullName: 'entity-group.asset-groups',
          type: 'link',
          path: '/entities/assets/groups',
          icon: 'domain'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_shared',
          name: 'asset.shared',
          fullName: 'entity-group.shared-asset-groups',
          type: 'link',
          path: '/entities/assets/shared',
          icon: 'domain',
          rootOnly: true
        }
      );
    }
    if (assetsPages.length) {
      entityPages.push(
        {
          id: 'assets',
          name: 'asset.assets',
          type: 'link',
          path: '/entities/assets',
          icon: 'domain',
          pages: assetsPages
        }
      );
    }
    const entityViewsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_all',
          name: 'entity-view.all',
          fullName: 'entity-view.all-entity-views',
          type: 'link',
          path: '/entities/entityViews/all',
          icon: 'view_quilt'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_groups',
          name: 'entity-view.groups',
          fullName: 'entity-group.entity-view-groups',
          type: 'link',
          path: '/entities/entityViews/groups',
          icon: 'view_quilt'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_shared',
          name: 'entity-view.shared',
          fullName: 'entity-group.shared-entity-view-groups',
          type: 'link',
          path: '/entities/entityViews/shared',
          icon: 'view_quilt',
          rootOnly: true
        }
      );
    }
    if (entityViewsPages.length) {
      entityPages.push(
        {
          id: 'entity_views',
          name: 'entity-view.entity-views',
          type: 'link',
          path: '/entities/entityViews',
          icon: 'view_quilt',
          pages: entityViewsPages
        }
      );
    }
    if (entityPages.length) {
      sections.push(
        {
          id: 'entities',
          name: 'entity.entities',
          type: 'toggle',
          path: '/entities',
          icon: 'category',
          pages: entityPages
        }
      );
    }
    const profilePages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)) {
      profilePages.push(
        {
          id: 'device_profiles',
          name: 'device-profile.device-profiles',
          type: 'link',
          path: '/profiles/deviceProfiles',
          icon: 'mdi:alpha-d-box',
          isMdiIcon: true
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)) {
      profilePages.push(
        {
          id: 'asset_profiles',
          name: 'asset-profile.asset-profiles',
          type: 'link',
          path: '/profiles/assetProfiles',
          icon: 'mdi:alpha-a-box',
          isMdiIcon: true
        }
      );
    }
    if (profilePages.length) {
      sections.push(
        {
          id: 'profiles',
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
          id: 'customer_all',
          name: 'customer.all',
          fullName: 'customer.all-customers',
          type: 'link',
          path: '/customers/all',
          icon: 'supervisor_account'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customer_groups',
          name: 'customer.groups',
          fullName: 'entity-group.customer-groups',
          type: 'link',
          path: '/customers/groups',
          icon: 'supervisor_account'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customer_shared',
          name: 'customer.shared',
          fullName: 'entity-group.shared-customer-groups',
          type: 'link',
          path: '/customers/shared',
          icon: 'supervisor_account',
          rootOnly: true
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customers_hierarchy',
          name: 'customer.hierarchy',
          fullName: 'customers-hierarchy.customers-hierarchy',
          type: 'link',
          path: '/customers/hierarchy',
          icon: 'sort',
          rootOnly: true
        }
      );
    }
    if (customerPages.length) {
      sections.push(
        {
          id: 'customers',
          name: 'customer.customers',
          type: 'link',
          path: '/customers',
          icon: 'supervisor_account',
          pages: customerPages
        }
      );
    }
    const userPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.USER)) {
      userPages.push(
        {
          id: 'user_all',
          name: 'user.all',
          fullName: 'user.all-users',
          type: 'link',
          path: '/users/all',
          icon: 'account_circle'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)) {
      userPages.push(
        {
          id: 'user_groups',
          name: 'user.groups',
          fullName: 'entity-group.user-groups',
          type: 'link',
          path: '/users/groups',
          icon: 'account_circle'
        }
      );
    }
    if (userPages.length) {
      sections.push(
        {
          id: 'users',
          name: 'user.users',
          type: 'link',
          path: '/users',
          icon: 'account_circle',
          pages: userPages
        }
      );
    }
    const integrationPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
      integrationPages.push(
        {
          id: 'integrations',
          name: 'integration.integrations',
          type: 'link',
          path: '/integrationsCenter/integrations',
          icon: 'input'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
      integrationPages.push(
        {
          id: 'converters',
          name: 'converter.converters',
          type: 'link',
          path: '/integrationsCenter/converters',
          icon: 'transform'
        }
      );
    }
    if (integrationPages.length) {
      sections.push(
        {
          id: 'integrations_center',
          name: 'integration.integrations-center',
          type: 'toggle',
          path: '/integrationsCenter',
          icon: 'integration_instructions',
          pages: integrationPages
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
      sections.push(
        {
          id: 'rule_chains',
          name: 'rulechain.rulechains',
          type: 'link',
          path: '/ruleChains',
          icon: 'settings_ethernet'
        }
      );
    }
    const edgeManagementPages: Array<MenuSection> = [];
    if (authState.edgesSupportEnabled) {
      const edgesPages: Array<MenuSection> = [];
      if (this.userPermissionsService.hasReadGenericPermission(Resource.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_all',
            name: 'edge.all',
            fullName: 'edge.all-edges',
            type: 'link',
            path: '/edgeManagement/instances/all',
            icon: 'router'
          }
        );
      }
      if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_groups',
            name: 'edge.groups',
            fullName: 'entity-group.edge-groups',
            type: 'link',
            path: '/edgeManagement/instances/groups',
            icon: 'router'
          }
        );
      }
      if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_shared',
            name: 'edge.shared',
            fullName: 'entity-group.shared-edge-groups',
            type: 'link',
            path: '/edgeManagement/instances/shared',
            icon: 'router',
            rootOnly: true
          }
        );
      }
      if (edgesPages.length) {
        edgeManagementPages.push(
          {
            id: 'edges',
            name: 'edge.instances',
            fullName: 'edge.edge-instances',
            type: 'link',
            path: '/edgeManagement/instances',
            icon: 'router',
            pages: edgesPages
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
        edgeManagementPages.push(
          {
            id: 'rulechain_templates',
            name: 'edge.rulechain-templates',
            type: 'link',
            path: '/edgeManagement/ruleChains',
            icon: 'settings_ethernet'
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
        edgeManagementPages.push(
          {
            id: 'integration_templates',
            name: 'edge.integration-templates',
            type: 'link',
            path: '/edgeManagement/integrations',
            icon: 'input'
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
        edgeManagementPages.push(
          {
            id: 'converter_templates',
            name: 'edge.converter-templates',
            type: 'link',
            path: '/edgeManagement/converters',
            icon: 'transform'
          }
        );
      }
    }
    if (edgeManagementPages.length) {
      sections.push(
        {
          id: 'edge_management',
          name: 'edge.management',
          type: 'toggle',
          path: '/edgeManagement',
          icon: 'settings_input_antenna',
          pages: edgeManagementPages
        }
      );
    }
    const advancedFeaturesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)) {
      advancedFeaturesPages.push(
        {
          id: 'otaUpdates',
          name: 'ota-update.ota-updates',
          type: 'link',
          path: '/features/otaUpdates',
          icon: 'memory'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      advancedFeaturesPages.push(
        {
          id: 'version_control',
          name: 'version-control.version-control',
          type: 'link',
          path: '/features/vc',
          icon: 'history'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      advancedFeaturesPages.push(
        {
          id: 'scheduler',
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/features/scheduler',
          icon: 'schedule'
        }
      );
    }
    if (advancedFeaturesPages.length) {
      sections.push(
        {
          id: 'features',
          name: 'feature.advanced-features',
          type: 'toggle',
          path: '/features',
          icon: 'construction',
          pages: advancedFeaturesPages
        }
      );
    }
    const resourcesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)) {
      resourcesPages.push(
        {
          id: 'widget_library',
          name: 'widget.widget-library',
          type: 'link',
          path: '/resources/widgets-bundles',
          icon: 'now_widgets'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)) {
      resourcesPages.push({
        id: 'resources_library',
        name: 'resource.resources-library',
        type: 'link',
        path: '/resources/resources-library',
        icon: 'mdi:rhombus-split',
        isMdiIcon: true
      });
    }
    if (resourcesPages.length) {
      sections.push(
        {
          id: 'resources',
          name: 'admin.resources',
          type: 'toggle',
          path: '/resources',
          icon: 'folder',
          pages: resourcesPages
        }
      );
    }
    const notificationPages: Array<MenuSection> = [];
    notificationPages.push(
      {
        id: 'notification_inbox',
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox'
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)) {
      notificationPages.push(
        {
          id: 'notification_sent',
          name: 'notification.sent',
          type: 'link',
          path: '/notification/sent',
          icon: 'outbox'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)) {
      notificationPages.push(
        {
          id: 'notification_recipients',
          name: 'notification.recipients',
          type: 'link',
          path: '/notification/recipients',
          icon: 'contacts'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)) {
      notificationPages.push(
        {
          id: 'notification_templates',
          name: 'notification.templates',
          type: 'link',
          path: '/notification/templates',
          icon: 'mdi:message-draw',
          isMdiIcon: true
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)) {
      notificationPages.push(
        {
          id: 'notification_rules',
          name: 'notification.rules',
          type: 'link',
          path: '/notification/rules',
          icon: 'mdi:message-cog',
          isMdiIcon: true
        }
      );
    }
    if (notificationPages.length) {
      sections.push(
        {
          id: 'notifications_center',
          name: 'notification.notification-center',
          type: 'link',
          path: '/notification',
          icon: 'mdi:message-badge',
          isMdiIcon: true,
          pages: notificationPages
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
      this.userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY)) {
      sections.push(
        {
          id: 'api_usage',
          name: 'api-usage.api-usage',
          type: 'link',
          path: '/usage',
          icon: 'insert_chart'
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      const whiteLabelPages: Array<MenuSection> = [
        {
          id: 'white_labeling_general',
          name: 'white-labeling.general',
          type: 'link',
          path: '/white-labeling/whiteLabel',
          icon: 'format_paint'
        },
        {
          id: 'login_white_labeling',
          name: 'white-labeling.login',
          type: 'link',
          path: '/white-labeling/loginWhiteLabel',
          icon: 'format_paint'
        },
        {
          id: 'mail_templates',
          name: 'admin.mail-templates',
          type: 'link',
          path: '/white-labeling/mail-template',
          icon: 'format_shapes'
        },
        {
          id: 'custom_translation',
          name: 'custom-translation.custom-translation',
          type: 'link',
          path: '/white-labeling/customTranslation',
          icon: 'language'
        },
        {
          id: 'custom_menu',
          name: 'custom-menu.custom-menu',
          type: 'link',
          path: '/white-labeling/customMenu',
          icon: 'list'
        }
      ];
      sections.push(
        {
          id: 'white_labeling',
          name: 'white-labeling.white-labeling',
          type: 'link',
          path: '/white-labeling',
          icon: 'format_paint',
          pages: whiteLabelPages
        }
      );
    }
    const settingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      settingPages.push(
        {
          id: 'home_settings',
          name: 'admin.home-settings',
          type: 'link',
          path: '/settings/home',
          icon: 'settings_applications'
        },
        {
          id: 'mail_server',
          name: 'admin.outgoing-mail',
          type: 'link',
          path: '/settings/outgoing-mail',
          icon: 'mail'
        },
        {
          id: 'notification_settings',
          name: 'admin.notifications',
          type: 'link',
          path: '/settings/notifications',
          icon: 'sms'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
      settingPages.push({
        id: 'repository_settings',
        name: 'admin.repository-settings',
        type: 'link',
        path: '/settings/repository',
        icon: 'manage_history'
      });
      settingPages.push({
        id: 'auto_commit_settings',
        name: 'admin.auto-commit-settings',
        type: 'link',
        path: '/settings/auto-commit',
        icon: 'settings_backup_restore'
      });
    }
    if (settingPages.length) {
      sections.push({
        id: 'settings',
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: settingPages
      });
    }

    const securitySettingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      securitySettingPages.push({
        id: '2fa',
        name: 'admin.2fa.2fa',
        type: 'link',
        path: '/security-settings/2fa',
        icon: 'mdi:two-factor-authentication',
        isMdiIcon: true
      });
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      securitySettingPages.push(
        {
          id: 'roles',
          name: 'role.roles',
          type: 'link',
          path: '/security-settings/roles',
          icon: 'security'
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      securitySettingPages.push(
        {
          id: 'self_registration',
          name: 'self-registration.self-registration',
          type: 'link',
          path: '/security-settings/selfRegistration',
          icon: 'group_add'
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      securitySettingPages.push(
        {
          id: 'audit_log',
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/security-settings/auditLogs',
          icon: 'track_changes'
        }
      );
    }
    if (securitySettingPages.length) {
      sections.push({
        id: 'security_settings',
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: securitySettingPages
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

  private buildCustomerUserMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: 'home',
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ALARM)) {
      sections.push(
        {
          id: 'alarms',
          name: 'alarm.alarms',
          type: 'link',
          path: '/alarms',
          icon: 'notifications'
        }
      );
    }
    const dashboardPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_all',
          name: 'dashboard.all',
          fullName: 'dashboard.all-dashboards',
          type: 'link',
          path: '/dashboards/all',
          icon: 'dashboards'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_groups',
          name: 'dashboard.groups',
          fullName: 'entity-group.dashboard-groups',
          type: 'link',
          path: '/dashboards/groups',
          icon: 'dashboard'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)) {
      dashboardPages.push(
        {
          id: 'dashboard_shared',
          name: 'dashboard.shared',
          fullName: 'entity-group.shared-dashboard-groups',
          type: 'link',
          path: '/dashboards/shared',
          icon: 'dashboard',
          rootOnly: true
        }
      );
    }
    if (dashboardPages.length) {
      sections.push(
        {
          id: 'dashboards',
          name: 'dashboard.dashboards',
          type: 'link',
          path: '/dashboards',
          icon: 'dashboards',
          pages: dashboardPages
        }
      );
    }
    const entityPages: Array<MenuSection> = [];
    const devicesPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_all',
          name: 'device.all',
          fullName: 'device.all-devices',
          type: 'link',
          path: '/entities/devices/all',
          icon: 'devices_other'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_groups',
          name: 'device.groups',
          fullName: 'entity-group.device-groups',
          type: 'link',
          path: '/entities/devices/groups',
          icon: 'devices_other'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)) {
      devicesPages.push(
        {
          id: 'device_shared',
          name: 'device.shared',
          fullName: 'entity-group.shared-device-groups',
          type: 'link',
          path: '/entities/devices/shared',
          icon: 'devices_other',
          rootOnly: true
        }
      );
    }
    if (devicesPages.length) {
      entityPages.push(
        {
          id: 'devices',
          name: 'device.devices',
          type: 'link',
          path: '/entities/devices',
          icon: 'devices_other',
          pages: devicesPages
        }
      );
    }
    const assetsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_all',
          name: 'asset.all',
          fullName: 'asset.all-assets',
          type: 'link',
          path: '/entities/assets/all',
          icon: 'domain'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_groups',
          name: 'asset.groups',
          fullName: 'entity-group.asset-groups',
          type: 'link',
          path: '/entities/assets/groups',
          icon: 'domain'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)) {
      assetsPages.push(
        {
          id: 'asset_shared',
          name: 'asset.shared',
          fullName: 'entity-group.shared-asset-groups',
          type: 'link',
          path: '/entities/assets/shared',
          icon: 'domain',
          rootOnly: true
        }
      );
    }
    if (assetsPages.length) {
      entityPages.push(
        {
          id: 'assets',
          name: 'asset.assets',
          type: 'link',
          path: '/entities/assets',
          icon: 'domain',
          pages: assetsPages
        }
      );
    }
    const entityViewsPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_all',
          name: 'entity-view.all',
          fullName: 'entity-view.all-entity-views',
          type: 'link',
          path: '/entities/entityViews/all',
          icon: 'view_quilt'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_groups',
          name: 'entity-view.groups',
          fullName: 'entity-group.entity-view-groups',
          type: 'link',
          path: '/entities/entityViews/groups',
          icon: 'view_quilt'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)) {
      entityViewsPages.push(
        {
          id: 'entity_view_shared',
          name: 'entity-view.shared',
          fullName: 'entity-group.shared-entity-view-groups',
          type: 'link',
          path: '/entities/entityViews/shared',
          icon: 'view_quilt',
          rootOnly: true
        }
      );
    }
    if (entityViewsPages.length) {
      entityPages.push(
        {
          id: 'entity_views',
          name: 'entity-view.entity-views',
          type: 'link',
          path: '/entities/entityViews',
          icon: 'view_quilt',
          pages: entityViewsPages
        }
      );
    }
    if (entityPages.length) {
      sections.push(
        {
          id: 'entities',
          name: 'entity.entities',
          type: 'toggle',
          path: '/entities',
          icon: 'category',
          pages: entityPages
        }
      );
    }
    const customerPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customer_all',
          name: 'customer.all',
          fullName: 'customer.all-customers',
          type: 'link',
          path: '/customers/all',
          icon: 'supervisor_account'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customer_groups',
          name: 'customer.groups',
          fullName: 'entity-group.customer-groups',
          type: 'link',
          path: '/customers/groups',
          icon: 'supervisor_account'
        }
      );
    }
    if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customer_shared',
          name: 'customer.shared',
          fullName: 'entity-group.shared-customer-groups',
          type: 'link',
          path: '/customers/shared',
          icon: 'supervisor_account',
          rootOnly: true
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      customerPages.push(
        {
          id: 'customers_hierarchy',
          name: 'customer.hierarchy',
          fullName: 'customers-hierarchy.customers-hierarchy',
          type: 'link',
          path: '/customers/hierarchy',
          icon: 'sort',
          rootOnly: true
        }
      );
    }
    if (customerPages.length) {
      sections.push(
        {
          id: 'customers',
          name: 'customer.customers',
          type: 'link',
          path: '/customers',
          icon: 'supervisor_account',
          pages: customerPages
        }
      );
    }
    const userPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.USER)) {
      userPages.push(
        {
          id: 'user_all',
          name: 'user.all',
          fullName: 'user.all-users',
          type: 'link',
          path: '/users/all',
          icon: 'account_circle'
        }
      );
    }
    if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)) {
      userPages.push(
        {
          id: 'user_groups',
          name: 'user.groups',
          fullName: 'entity-group.user-groups',
          type: 'link',
          path: '/users/groups',
          icon: 'account_circle'
        }
      );
    }
    if (userPages.length) {
      sections.push(
        {
          id: 'users',
          name: 'user.users',
          type: 'link',
          path: '/users',
          icon: 'account_circle',
          pages: userPages
        }
      );
    }
    if (authState.edgesSupportEnabled) {
      const edgesPages: Array<MenuSection> = [];
      if (this.userPermissionsService.hasReadGenericPermission(Resource.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_all',
            name: 'edge.all',
            fullName: 'edge.all-edges',
            type: 'link',
            path: '/edgeManagement/instances/all',
            icon: 'router'
          }
        );
      }
      if (this.userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_groups',
            name: 'edge.groups',
            fullName: 'entity-group.edge-groups',
            type: 'link',
            path: '/edgeManagement/instances/groups',
            icon: 'router'
          }
        );
      }
      if (this.userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)) {
        edgesPages.push(
          {
            id: 'edge_shared',
            name: 'edge.shared',
            fullName: 'entity-group.shared-edge-groups',
            type: 'link',
            path: '/edgeManagement/instances/shared',
            icon: 'router',
            rootOnly: true
          }
        );
      }
      if (edgesPages.length) {
        sections.push(
          {
            id: 'edges',
            name: 'edge.edge-instances',
            fullName: 'edge.edge-instances',
            type: 'link',
            path: '/edgeManagement/instances',
            icon: 'router',
            pages: edgesPages
          }
        );
      }
    }
    const notificationPages: Array<MenuSection> = [];
    // TODO: permission check
    notificationPages.push(
      {
        id: 'notification_inbox',
        name: 'notification.inbox',
        type: 'link',
        path: '/notification/inbox',
        icon: 'inbox'
      }
    );
    if (notificationPages.length) {
      sections.push(
        {
          id: 'notifications_center',
          name: 'notification.notification-center',
          type: 'link',
          path: '/notification',
          icon: 'mdi:message-badge',
          isMdiIcon: true,
          pages: notificationPages
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      sections.push(
        {
          id: 'scheduler',
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/features/scheduler',
          icon: 'schedule'
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      const whiteLabelPages: Array<MenuSection> = [
        {
          id: 'white_labeling_general',
          name: 'white-labeling.general',
          type: 'link',
          path: '/white-labeling/whiteLabel',
          icon: 'format_paint'
        },
        {
          id: 'login_white_labeling',
          name: 'white-labeling.login',
          type: 'link',
          path: '/white-labeling/loginWhiteLabel',
          icon: 'format_paint'
        },
        {
          id: 'custom_translation',
          name: 'custom-translation.custom-translation',
          type: 'link',
          path: '/white-labeling/customTranslation',
          icon: 'language'
        },
        {
          id: 'custom_menu',
          name: 'custom-menu.custom-menu',
          type: 'link',
          path: '/white-labeling/customMenu',
          icon: 'list'
        }
      ];
      sections.push(
        {
          id: 'white_labeling',
          name: 'white-labeling.white-labeling',
          type: 'link',
          path: '/white-labeling',
          icon: 'format_paint',
          pages: whiteLabelPages
        }
      );
    }
    const settingPages: Array<MenuSection> = [];
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      settingPages.push(
        {
          id: 'home_settings',
          name: 'admin.home-settings',
          type: 'link',
          path: '/settings/home',
          icon: 'settings_applications'
        }
      );
    }
    if (settingPages.length) {
      sections.push({
        id: 'settings',
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: settingPages
      });
    }
    const securitySettingPages: Array<MenuSection> = [];
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      securitySettingPages.push(
        {
          id: 'roles',
          name: 'role.roles',
          type: 'link',
          path: '/security-settings/roles',
          icon: 'security'
        }
      );
    }
   if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      securitySettingPages.push(
        {
          id: 'audit_log',
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/security-settings/auditLogs',
          icon: 'track_changes'
        }
      );
    }
    if (securitySettingPages.length) {
      sections.push({
        id: 'security_settings',
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: securitySettingPages
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
        id: stateId,
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
            id: childStateId,
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

  private allMenuLinks(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      if (section.type === 'link' && !section.disabled) {
        result.push(section);
      }
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuLinks(section.pages));
      }
    }
    return result;
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

  public availableMenuLinks(): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$;
  }

  public menuLinkById(id: string): Observable<MenuSection | undefined> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.find(link => link.id === id))
    );
  }

  public menuLinksByIds(ids: string[]): Observable<Array<MenuSection>> {
    return this.availableMenuLinks$.pipe(
      map((links) => links.filter(link => ids.includes(link.id)).sort((a, b) => {
        const i1 = ids.indexOf(a.id);
        const i2 = ids.indexOf(b.id);
        return i1 - i2;
      }))
    );
  }
}
