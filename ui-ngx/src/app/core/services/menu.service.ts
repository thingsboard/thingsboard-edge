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

import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, take } from 'rxjs/operators';
import { buildUserMenu, filterOpenedMenuSection, HomeSection, MenuId, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, ReplaySubject, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { EntityType } from '@shared/models/entity-type.models';
import { ActivationEnd, NavigationEnd, Params, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AuthState } from '@core/auth/auth.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  private menuSections$: Subject<Array<MenuSection>> = new ReplaySubject<Array<MenuSection>>(1);
  private homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);
  private availableMenuLinks$ = this.menuSections$.pipe(
    map((items) => this.allMenuLinks(items))
  );
  availableMenuSections$ = this.menuSections$.pipe(
    map((items) => this.allMenuSections(items))
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
          let disabledItems: MenuId[] = [];
          if (customMenu && customMenu.disabledMenuItems) {
            disabledItems = customMenu.disabledMenuItems;
          }
          const index = disabledItems.indexOf('sms_provider' as MenuId);
          if (index !== -1) {
            disabledItems[index] = MenuId.notification_settings;
          }
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              this.currentHomeSections = this.buildSysAdminHome(disabledItems);
              break;
            case Authority.TENANT_ADMIN:
              this.currentHomeSections = this.buildTenantAdminHome(authState, disabledItems);
              break;
            case Authority.CUSTOMER_USER:
              this.currentHomeSections = this.buildCustomerUserHome(authState, disabledItems);
              break;
          }
          this.currentMenuSections = buildUserMenu(authState, this.userPermissionsService, customMenu);
          this.updateCurrentCustomSection();
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
    this.currentMenuSections.filter(section => filterOpenedMenuSection(section, url, openedMenuSections)).forEach(
      section => section.opened = true
    );
  }

  private buildSysAdminHome(disabledItems: MenuId[]): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'tenant.management',
        places: [
          {
            name: 'tenant.tenants',
            icon: 'supervisor_account',
            path: '/tenants',
            disabled: disabledItems.indexOf(MenuId.tenants) > -1
          },
          {
            name: 'tenant-profile.tenant-profiles',
            icon: 'mdi:alpha-t-box',
            path: '/tenantProfiles',
            disabled: disabledItems.indexOf(MenuId.tenant_profiles) > -1
          }
        ]
      },
      {
        name: 'widget.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/resources/widgets-library',
            disabled: disabledItems.indexOf(MenuId.widget_library) > -1
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
            disabled: disabledItems.indexOf(MenuId.general) > -1
          },
          {
            name: 'admin.outgoing-mail',
            icon: 'mail',
            path: '/settings/outgoing-mail',
            disabled: disabledItems.indexOf(MenuId.mail_server) > -1
          },
          {
            name: 'admin.sms-provider',
            icon: 'sms',
            path: '/settings/sms-provider',
            disabled: disabledItems.indexOf(MenuId.notification_settings) > -1
          },
          {
            name: 'admin.security-settings',
            icon: 'security',
            path: '/settings/security-settings',
            disabled: disabledItems.indexOf(MenuId.security_settings) > -1
          },
          {
            name: 'admin.oauth2.oauth2',
            icon: 'security',
            path: '/settings/oauth2',
            disabled: disabledItems.indexOf(MenuId.oauth2) > -1
          },
          {
            name: 'admin.2fa.2fa',
            icon: 'mdi:two-factor-authentication',
            path: '/settings/2fa',
            disabled: disabledItems.indexOf(MenuId.two_fa) > -1
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library',
            disabled: disabledItems.indexOf(MenuId.resources_library) > -1
          },
          {
            name: 'admin.queues',
            icon: 'swap_calls',
            path: '/settings/queues',
            disabled: disabledItems.indexOf(MenuId.queues) > -1
          },
          {
            name: 'admin.mobile-app.mobile-app',
            icon: 'smartphone',
            path: '/settings/mobile-app',
            disabled: disabledItems.indexOf(MenuId.mobile_app_settings) > -1
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
            disabled: disabledItems.indexOf(MenuId.white_labeling) > -1
          },
          {
            name: 'white-labeling.login-white-labeling',
            icon: 'format_paint',
            path: '/white-labeling/loginWhiteLabel',
            disabled: disabledItems.indexOf(MenuId.login_white_labeling) > -1
          },
          {
            name: 'admin.mail-templates',
            icon: 'format_shapes',
            path: '/white-labeling/mail-template',
            disabled: disabledItems.indexOf(MenuId.mail_templates) > -1
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
            disabled: disabledItems.indexOf(MenuId.custom_translation) > -1
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
            disabled: disabledItems.indexOf(MenuId.custom_menu) > -1
          }
        ]
      }
    );
    return homeSections;
  }

  private buildTenantAdminHome(authState: AuthState, disabledItems: MenuId[]): Array<HomeSection> {
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
              disabled: disabledItems.indexOf(MenuId.solution_templates) > -1
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
              disabled: disabledItems.indexOf(MenuId.rule_chains) > -1
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
              disabled: disabledItems.indexOf(MenuId.converters) > -1
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
              disabled: disabledItems.indexOf(MenuId.integrations) > -1
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
              disabled: disabledItems.indexOf(MenuId.roles) > -1
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
              disabled: disabledItems.indexOf(MenuId.user_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.customer_groups) > -1
            },
            {
              name: 'customers-hierarchy.customers-hierarchy',
              icon: 'sort',
              path: '/customersHierarchy',
              disabled: disabledItems.indexOf(MenuId.customers_hierarchy) > -1
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
            disabled: disabledItems.indexOf(MenuId.asset_groups) > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)) {
        assetManagementSection.places.push(
          {
            name: 'asset-profile.asset-profiles',
            icon: 'mdi:alpha-a-box',
            path: '/profiles/assetProfiles',
            disabled: disabledItems.indexOf(MenuId.asset_profiles) > -1
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
            disabled: disabledItems.indexOf(MenuId.device_groups) > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)) {
        deviceManagementSection.places.push(
          {
            name: 'device-profile.device-profiles',
            icon: 'mdi:alpha-d-box',
            path: '/profiles/deviceProfiles',
            disabled: disabledItems.indexOf(MenuId.device_profiles) > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)) {
        deviceManagementSection.places.push(
          {
            name: 'ota-update.ota-updates',
            icon: 'memory',
            path: '/otaUpdates',
            disabled: disabledItems.indexOf(MenuId.otaUpdates) > -1
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
              disabled: disabledItems.indexOf(MenuId.entity_view_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.edge_groups) > -1
            },
            {
              name: 'edge.rulechain-templates',
              icon: 'settings_ethernet',
              path: '/edgeManagement/ruleChains',
              disabled: disabledItems.indexOf(MenuId.rulechain_templates) > -1
            },
            {
              name: 'edge.converter-templates',
              icon: 'transform',
              path: '/edgeManagement/converters',
              disabled: disabledItems.indexOf(MenuId.converter_templates) > -1
            },
            {
              name: 'edge.integration-templates',
              icon: 'input',
              path: '/edgeManagement/integrations',
              disabled: disabledItems.indexOf(MenuId.integration_templates) > -1
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
            disabled: disabledItems.indexOf(MenuId.widget_library) > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD)) {
        dashboardManagement.places.push(
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboards',
            disabled: disabledItems.indexOf(MenuId.dashboard_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.scheduler) > -1
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
              disabled: disabledItems.indexOf(MenuId.white_labeling) > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/loginWhiteLabel',
              disabled: disabledItems.indexOf(MenuId.login_white_labeling) > -1
            },
            {
              name: 'admin.mail-templates',
              icon: 'format_shapes',
              path: '/white-labeling/mail-template',
              disabled: disabledItems.indexOf(MenuId.mail_templates) > -1
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
              disabled: disabledItems.indexOf(MenuId.custom_translation) > -1
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
              disabled: disabledItems.indexOf(MenuId.custom_menu) > -1
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
              disabled: disabledItems.indexOf(MenuId.version_control) > -1
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
            disabled: disabledItems.indexOf(MenuId.audit_log) > -1
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
            disabled: disabledItems.indexOf(MenuId.api_usage) > -1
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
            disabled: disabledItems.indexOf(MenuId.home_settings) > -1
          },
          {
            name: 'admin.outgoing-mail',
            path: '/settings/outgoing-mail',
            icon: 'mail',
            disabled: disabledItems.indexOf(MenuId.mail_server) > -1
          },
          {
            name: 'admin.sms-provider',
            path: '/settings/sms-provider',
            icon: 'sms',
            disabled: disabledItems.indexOf(MenuId.notification_settings) > -1
          },
          {
            name: 'self-registration.self-registration',
            path: '/settings/selfRegistration',
            icon: 'group_add',
            disabled: disabledItems.indexOf(MenuId.self_registration) > -1
          },
          {
            name: 'admin.2fa.2fa',
            path: '/settings/2fa',
            icon: 'mdi:two-factor-authentication',
            disabled: disabledItems.indexOf(MenuId.two_fa) > -1
          }
        );
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)) {
        settings.places.push({
          name: 'resource.resources-library',
          path: '/settings/resources-library',
          icon: 'folder',
          disabled: disabledItems.indexOf(MenuId.resources_library) > -1
        });
      }
      if (this.userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)) {
        settings.places.push({
          name: 'admin.repository-settings',
          path: '/settings/repository',
          icon: 'manage_history',
          disabled: disabledItems.indexOf(MenuId.repository_settings) > -1
        });
        settings.places.push({
          name: 'admin.auto-commit-settings',
          path: '/settings/auto-commit',
          icon: 'settings_backup_restore',
          disabled: disabledItems.indexOf(MenuId.auto_commit_settings) > -1
        });
      }
    }
    return homeSections;
  }

  private buildCustomerUserHome(authState: AuthState, disabledItems: MenuId[]): Array<HomeSection> {
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
              disabled: disabledItems.indexOf(MenuId.roles) > -1
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
              disabled: disabledItems.indexOf(MenuId.user_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.customer_groups) > -1
            },
            {
              name: 'customers-hierarchy.customers-hierarchy',
              icon: 'sort',
              path: '/customersHierarchy',
              disabled: disabledItems.indexOf(MenuId.customers_hierarchy) > -1
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
              disabled: disabledItems.indexOf(MenuId.asset_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.device_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.entity_view_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.edge_groups) > -1
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
            path: '/dashboards',
            disabled: disabledItems.indexOf(MenuId.dashboard_groups) > -1
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
              disabled: disabledItems.indexOf(MenuId.scheduler) > -1
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
              disabled: disabledItems.indexOf(MenuId.white_labeling) > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/white-labeling/loginWhiteLabel',
              disabled: disabledItems.indexOf(MenuId.login_white_labeling) > -1
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
              disabled: disabledItems.indexOf(MenuId.custom_translation) > -1
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
              disabled: disabledItems.indexOf(MenuId.custom_menu) > -1
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
              disabled: disabledItems.indexOf(MenuId.audit_log) > -1
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
              disabled: disabledItems.indexOf(MenuId.home_settings) > -1
            }
          ]
        }
      );
    }
    return homeSections;
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

  private allMenuSections(sections: Array<MenuSection>): Array<MenuSection> {
    const result: Array<MenuSection> = [];
    for (const section of sections) {
      result.push(section);
      if (section.pages && section.pages.length) {
        result.push(...this.allMenuSections(section.pages));
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

  public availableMenuSections(): Observable<Array<MenuSection>> {
    return this.availableMenuSections$;
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
