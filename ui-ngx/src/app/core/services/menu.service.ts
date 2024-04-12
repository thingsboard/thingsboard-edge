///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import { HomeSection, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { NavigationEnd, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  currentMenuSections: Array<MenuSection>;
  menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);
  availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );

  constructor(private store: Store<AppState>,
              private router: Router) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          let homeSections: Array<HomeSection>;
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              this.currentMenuSections = this.buildSysAdminMenu();
              homeSections = this.buildSysAdminHome();
              break;
            case Authority.TENANT_ADMIN:
              this.currentMenuSections = this.buildTenantAdminMenu(authState);
              homeSections = this.buildTenantAdminHome(authState);
              break;
            case Authority.CUSTOMER_USER:
              this.currentMenuSections = this.buildCustomerUserMenu(authState);
              homeSections = this.buildCustomerUserHome(authState);
              break;
          }
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          this.homeSections$.next(homeSections);
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
        icon: 'mdi:alpha-t-box'
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
            path: '/resources/widgets-library',
            icon: 'now_widgets',
            pages: [
              {
                id: 'widget_types',
                name: 'widget.widgets',
                type: 'link',
                path: '/resources/widgets-library/widget-types',
                icon: 'now_widgets'
              },
              {
                id: 'widgets_bundles',
                name: 'widgets-bundle.widgets-bundles',
                type: 'link',
                path: '/resources/widgets-library/widgets-bundles',
                icon: 'now_widgets'
              }
            ]
          },
          {
            id: 'images',
            name: 'image.gallery',
            type: 'link',
            path: '/resources/images',
            icon: 'filter'
          },
          {
            id: 'resources_library',
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split'
          }
        ]
      },
      {
        id: 'notifications_center',
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        pages: [
          {
            id: 'notification_inbox',
            name: 'notification.inbox',
            fullName: 'notification.notification-inbox',
            type: 'link',
            path: '/notification/inbox',
            icon: 'inbox'
          },
          {
            id: 'notification_sent',
            name: 'notification.sent',
            fullName: 'notification.notification-sent',
            type: 'link',
            path: '/notification/sent',
            icon: 'outbox'
          },
          {
            id: 'notification_recipients',
            name: 'notification.recipients',
            fullName: 'notification.notification-recipients',
            type: 'link',
            path: '/notification/recipients',
            icon: 'contacts'
          },
          {
            id: 'notification_templates',
            name: 'notification.templates',
            fullName: 'notification.notification-templates',
            type: 'link',
            path: '/notification/templates',
            icon: 'mdi:message-draw'
          },
          {
            id: 'notification_rules',
            name: 'notification.rules',
            fullName: 'notification.notification-rules',
            type: 'link',
            path: '/notification/rules',
            icon: 'mdi:message-cog'
          }
        ]
      },
      {
        id: 'settings',
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: [
          {
            id: 'general',
            name: 'admin.general',
            fullName: 'admin.general-settings',
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
            fullName: 'admin.notifications-settings',
            type: 'link',
            path: '/settings/notifications',
            icon: 'mdi:message-badge'
          },
          {
            id: 'queues',
            name: 'admin.queues',
            type: 'link',
            path: '/settings/queues',
            icon: 'swap_calls'
          },
        ]
      },
      {
        id: 'security_settings',
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: [
          {
            id: 'security_settings_general',
            name: 'admin.general',
            fullName: 'security.general-settings',
            type: 'link',
            path: '/security-settings/general',
            icon: 'settings_applications'
          },
          {
            id: '2fa',
            name: 'admin.2fa.2fa',
            type: 'link',
            path: '/security-settings/2fa',
            icon: 'mdi:two-factor-authentication'
          },
          {
            id: 'oauth2',
            name: 'admin.oauth2.oauth2',
            type: 'link',
            path: '/security-settings/oauth2',
            icon: 'mdi:shield-account'
          }
        ]
      }
    );
    return sections;
  }

  private buildSysAdminHome(): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'tenant.management',
        places: [
          {
            name: 'tenant.tenants',
            icon: 'supervisor_account',
            path: '/tenants'
          },
          {
            name: 'tenant-profile.tenant-profiles',
            icon: 'mdi:alpha-t-box',
            path: '/tenantProfiles'
          },
        ]
      },
      {
        name: 'widget.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/resources/widgets-library',
          }
        ]
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.general',
            icon: 'settings_applications',
            path: '/settings/general'
          },
          {
            name: 'admin.outgoing-mail',
            icon: 'mail',
            path: '/settings/outgoing-mail'
          },
          {
            name: 'admin.sms-provider',
            icon: 'sms',
            path: '/settings/sms-provider'
          },
          {
            name: 'admin.security-settings',
            icon: 'security',
            path: '/settings/security-settings'
          },
          {
            name: 'admin.oauth2.oauth2',
            icon: 'security',
            path: '/settings/oauth2'
          },
          {
            name: 'admin.2fa.2fa',
            icon: 'mdi:two-factor-authentication',
            path: '/settings/2fa'
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library'
          },
          {
            name: 'admin.queues',
            icon: 'swap_calls',
            path: '/settings/queues'
          },
        ]
      }
    );
    return homeSections;
  }

  private buildTenantAdminMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    const edgePages: Array<MenuSection> = [
      {
        id: 'edge_status',
        name: 'edge.status',
        type: 'link',
        path: '/edge/status',
        icon: 'info'
      },
      {
        id: 'edge_cloud_events',
        name: 'edge.cloud-events',
        type: 'link',
        path: '/edge/cloudEvents',
        icon: 'date_range'
      }
    ];
    sections.push(
      {
        id: 'home',
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      },
      {
        id: 'edge_info',
        name: 'edge.edge',
        type: 'toggle',
        path: '/edge',
        icon: 'router',
        pages: edgePages
      },
      {
        id: 'alarms',
        name: 'alarm.alarms',
        type: 'link',
        path: '/alarms',
        icon: 'mdi:alert-outline'
      },
      {
        id: 'dashboards',
        name: 'dashboard.dashboards',
        type: 'link',
        path: '/dashboards',
        icon: 'dashboards'
      },
      {
        id: 'entities',
        name: 'entity.entities',
        type: 'toggle',
        path: '/entities',
        icon: 'category',
        pages: [
          {
            id: 'devices',
            name: 'device.devices',
            type: 'link',
            path: '/entities/devices',
            icon: 'devices_other'
          },
          {
            id: 'assets',
            name: 'asset.assets',
            type: 'link',
            path: '/entities/assets',
            icon: 'domain'
          },
          {
            id: 'entity_views',
            name: 'entity-view.entity-views',
            type: 'link',
            path: '/entities/entityViews',
            icon: 'view_quilt'
          }
        ]
      },
      {
        id: 'profiles',
        name: 'profiles.profiles',
        type: 'toggle',
        path: '/profiles',
        icon: 'badge',
        pages: [
          {
            id: 'device_profiles',
            name: 'device-profile.device-profiles',
            type: 'link',
            path: '/profiles/deviceProfiles',
            icon: 'mdi:alpha-d-box'
          },
          {
            id: 'asset_profiles',
            name: 'asset-profile.asset-profiles',
            type: 'link',
            path: '/profiles/assetProfiles',
            icon: 'mdi:alpha-a-box'
          }
        ]
      },
      {
        id: 'customers',
        name: 'customer.customers',
        type: 'link',
        path: '/customers',
        icon: 'supervisor_account'
      },
      {
        id: 'rule_chains',
        name: 'rulechain.rulechains',
        type: 'link',
        path: '/ruleChains',
        icon: 'settings_ethernet'
      }
    );
    /* edge-only: merge comment - these sections should not be visible on edge
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: 'edge_management',
          name: 'edge.management',
          type: 'toggle',
          path: '/edgeManagement',
          icon: 'settings_input_antenna',
          pages: [
            {
              id: 'edges',
              name: 'edge.instances',
              fullName: 'edge.edge-instances',
              type: 'link',
              path: '/edgeManagement/instances',
              icon: 'router'
            },
            {
              id: 'rulechain_templates',
              name: 'edge.rulechain-templates',
              fullName: 'edge.edge-rulechain-templates',
              type: 'link',
              path: '/edgeManagement/ruleChains',
              icon: 'settings_ethernet'
            }
          ]
        }
      );
    }
     */
    sections.push(
      {
        id: 'features',
        name: 'feature.advanced-features',
        type: 'toggle',
        path: '/features',
        icon: 'construction',
        pages: [
          {
            id: 'otaUpdates',
            name: 'ota-update.ota-updates',
            type: 'link',
            path: '/features/otaUpdates',
            icon: 'memory'
          },
          /* edge-only: merge comment - these sections should not be visible on edge
          {
            id: 'version_control',
            name: 'version-control.version-control',
            type: 'link',
            path: '/features/vc',
            icon: 'history'
          }
           */
        ]
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
            path: '/resources/widgets-library',
            icon: 'now_widgets',
            pages: [
              {
                id: 'widget_types',
                name: 'widget.widgets',
                type: 'link',
                path: '/resources/widgets-library/widget-types',
                icon: 'now_widgets'
              },
              {
                id: 'widgets_bundles',
                name: 'widgets-bundle.widgets-bundles',
                type: 'link',
                path: '/resources/widgets-library/widgets-bundles',
                icon: 'now_widgets'
              }
            ]
          },
          {
            id: 'images',
            name: 'image.gallery',
            type: 'link',
            path: '/resources/images',
            icon: 'filter'
          },
          {
            id: 'resources_library',
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split'
          }
        ]
      },
      {
        id: 'notifications_center',
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        pages: [
          {
            id: 'notification_inbox',
            name: 'notification.inbox',
            fullName: 'notification.notification-inbox',
            type: 'link',
            path: '/notification/inbox',
            icon: 'inbox'
          },
          {
            id: 'notification_sent',
            name: 'notification.sent',
            fullName: 'notification.notification-sent',
            type: 'link',
            path: '/notification/sent',
            icon: 'outbox'
          },
          {
            id: 'notification_recipients',
            name: 'notification.recipients',
            fullName: 'notification.notification-recipients',
            type: 'link',
            path: '/notification/recipients',
            icon: 'contacts'
          },
          {
            id: 'notification_templates',
            name: 'notification.templates',
            fullName: 'notification.notification-templates',
            type: 'link',
            path: '/notification/templates',
            icon: 'mdi:message-draw'
          },
          {
            id: 'notification_rules',
            name: 'notification.rules',
            fullName: 'notification.notification-rules',
            type: 'link',
            path: '/notification/rules',
            icon: 'mdi:message-cog'
          }
        ]
      },
      {
        id: 'api_usage',
        name: 'api-usage.api-usage',
        type: 'link',
        path: '/usage',
        icon: 'insert_chart'
      },
      {
        id: 'settings',
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: [
          {
            id: 'home_settings',
            name: 'admin.home',
            fullName: 'admin.home-settings',
            type: 'link',
            path: '/settings/home',
            icon: 'settings_applications'
          },
          {
            id: 'notification_settings',
            name: 'admin.notifications',
            fullName: 'admin.notifications-settings',
            type: 'link',
            path: '/settings/notifications',
            icon: 'mdi:message-badge'
          },
          /* edge-only: merge comment - these sections should not be visible on edge
          {
            id: 'repository_settings',
            name: 'admin.repository',
            fullName: 'admin.repository-settings',
            type: 'link',
            path: '/settings/repository',
            icon: 'manage_history'
          },
          {
            id: 'auto_commit_settings',
            name: 'admin.auto-commit',
            fullName: 'admin.auto-commit-settings',
            type: 'link',
            path: '/settings/auto-commit',
            icon: 'settings_backup_restore'
          }
          */
        ]
      },
      {
        id: 'security_settings',
        name: 'security.security',
        type: 'toggle',
        path: '/security-settings',
        icon: 'security',
        pages: [
          {
            id: 'audit_log',
            name: 'audit-log.audit-logs',
            type: 'link',
            path: '/security-settings/auditLogs',
            icon: 'track_changes'
          }
        ]
      }
    );
    return sections;
  }

  private buildTenantAdminHome(authState: AuthState): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'edge.edge',
        places: [
          {
            name: 'edge.status',
            icon: 'info',
            path: '/edge/status'
          },
          {
            name: 'edge.cloud-events',
            icon: 'date_range',
            path: '/edge/cloudEvents'
          }
        ]
      }
    );
    homeSections.push(
      {
        name: 'rulechain.management',
        places: [
          {
            name: 'rulechain.rulechains',
            icon: 'settings_ethernet',
            path: '/ruleChains'
          }
        ]
      },
      {
        name: 'customer.management',
        places: [
          {
            name: 'customer.customers',
            icon: 'supervisor_account',
            path: '/customers'
          }
        ]
      },
      {
        name: 'asset.management',
        places: [
          {
            name: 'asset.assets',
            icon: 'domain',
            path: '/assets'
          },
          {
            name: 'asset-profile.asset-profiles',
            icon: 'mdi:alpha-a-box',
            path: '/profiles/assetProfiles'
          }
        ]
      },
      {
        name: 'device.management',
        places: [
          {
            name: 'device.devices',
            icon: 'devices_other',
            path: '/devices'
          },
          {
            name: 'device-profile.device-profiles',
            icon: 'mdi:alpha-d-box',
            path: '/profiles/deviceProfiles'
          },
          {
            name: 'ota-update.ota-updates',
            icon: 'memory',
            path: '/otaUpdates'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            icon: 'view_quilt',
            path: '/entityViews'
          }
        ]
      }
    );
    /* edge-only: merge comment - hide this on edge
    if (authState.edgesSupportEnabled) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'router',
              path: '/edgeInstances'
            },
            {
              name: 'edge.rulechain-templates',
              icon: 'settings_ethernet',
              path: '/edgeManagement/ruleChains'
            }
          ]
        }
      );
    }
     */
    homeSections.push(
      {
        name: 'dashboard.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/widgets-bundles'
          },
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboards'
          }
        ]
      },
      /* edge-only: merge comment - these sections should not be visible on edge
      {
        name: 'version-control.management',
        places: [
          {
            name: 'version-control.version-control',
            icon: 'history',
            path: '/vc'
          }
        ]
      },
      */
      {
        name: 'audit-log.audit',
        places: [
          {
            name: 'audit-log.audit-logs',
            icon: 'track_changes',
            path: '/auditLogs'
          },
          {
            name: 'api-usage.api-usage',
            icon: 'insert_chart',
            path: '/usage'
          }
        ]
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.home-settings',
            icon: 'settings_applications',
            path: '/settings/home'
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library'
          },
          /* edge-only: merge comment - these sections should not be visible on edge
          {
            name: 'admin.repository-settings',
            icon: 'manage_history',
            path: '/settings/repository',
          },
          {
            name: 'admin.auto-commit-settings',
            icon: 'settings_backup_restore',
            path: '/settings/auto-commit'
          }
          */
        ]
      }
    );
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
      },
      {
        id: 'alarms',
        name: 'alarm.alarms',
        type: 'link',
        path: '/alarms',
        icon: 'mdi:alert-outline'
      },
      {
        id: 'dashboards',
        name: 'dashboard.dashboards',
        type: 'link',
        path: '/dashboards',
        icon: 'dashboards'
      },
      {
        id: 'entities',
        name: 'entity.entities',
        type: 'toggle',
        path: '/entities',
        icon: 'category',
        pages: [
          {
            id: 'devices',
            name: 'device.devices',
            type: 'link',
            path: '/entities/devices',
            icon: 'devices_other'
          },
          {
            id: 'assets',
            name: 'asset.assets',
            type: 'link',
            path: '/entities/assets',
            icon: 'domain'
          },
          {
            id: 'entity_views',
            name: 'entity-view.entity-views',
            type: 'link',
            path: '/entities/entityViews',
            icon: 'view_quilt'
          }
        ]
      }
    );
    /* edge-only: merge comment - hide this on edge
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: 'edges',
          name: 'edge.edge-instances',
          fullName: 'edge.edge-instances',
          type: 'link',
          path: '/edgeManagement/instances',
          icon: 'router'
        }
      );
    }
    sections.push(
      {
        id: 'notifications_center',
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        pages: [
          {
            id: 'notification_inbox',
            name: 'notification.inbox',
            fullName: 'notification.notification-inbox',
            type: 'link',
            path: '/notification/inbox',
            icon: 'inbox'
          }
        ]
      }
    );
     */
    return sections;
  }

  private buildCustomerUserHome(authState: AuthState): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'asset.view-assets',
        places: [
          {
            name: 'asset.assets',
            icon: 'domain',
            path: '/assets'
          }
        ]
      },
      {
        name: 'device.view-devices',
        places: [
          {
            name: 'device.devices',
            icon: 'devices_other',
            path: '/devices'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            icon: 'view_quilt',
            path: '/entityViews'
          }
        ]
      }
    );
    /* edge-only: merge comment - hide this on edge
    if (authState.edgesSupportEnabled) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'settings_input_antenna',
              path: '/edgeInstances'
            }
          ]
        }
      );
    }
     */
    homeSections.push(
      {
        name: 'dashboard.view-dashboards',
        places: [
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboards'
          }
        ]
      }
    );
    return homeSections;
  }

  private allMenuLinks(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      if (section.type === 'link') {
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
