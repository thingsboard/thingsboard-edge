///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import { AuthService } from '../auth/auth.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, map, mergeMap, publishReplay, refCount, take } from 'rxjs/operators';
import { HomeSection, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, of, Subject, Subscription } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityType } from '@shared/models/entity-type.models';
import { BroadcastService } from '@core/services/broadcast.service';
import { ActivationEnd, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Resource } from '@shared/models/security.models';
import { AuthState } from '@core/auth/auth.models';
import { CustomMenuItem } from '@shared/models/custom-menu.models';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);

  entityGroupSections: Array<EntityGroupSection> = [];

  constructor(private store: Store<AppState>,
              private router: Router,
              private customMenuService: CustomMenuService,
              private entityGroupService: EntityGroupService,
              private broadcast: BroadcastService,
              private userPermissionsService: UserPermissionsService,
              private authService: AuthService) {
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
  }

  private buildMenu() {
    for (const entityGroupSection of this.entityGroupSections) {
      entityGroupSection.destroy();
    }
    this.entityGroupSections.length = 0;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          let menuSections: Array<MenuSection>;
          let homeSections: Array<HomeSection>;
          const customMenu = this.customMenuService.getCustomMenu();
          let disabledItems: string[] = [];
          if (customMenu && customMenu.disabledMenuItems) {
            disabledItems = customMenu.disabledMenuItems;
          }
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              menuSections = this.buildSysAdminMenu(authState, disabledItems);
              homeSections = this.buildSysAdminHome(authState, disabledItems);
              break;
            case Authority.TENANT_ADMIN:
              menuSections = this.buildTenantAdminMenu(authState, disabledItems);
              homeSections = this.buildTenantAdminHome(authState, disabledItems);
              break;
            case Authority.CUSTOMER_USER:
              menuSections = this.buildCustomerUserMenu(authState, disabledItems);
              homeSections = this.buildCustomerUserHome(authState, disabledItems);
              break;
          }
          if (authState.authUser.authority === Authority.TENANT_ADMIN ||
              authState.authUser.authority === Authority.CUSTOMER_USER) {
            let customMenuItems: CustomMenuItem[] = [];
            if (customMenu && customMenu.menuItems) {
              customMenuItems = customMenu.menuItems;
            }
            this.buildCustomMenu(customMenuItems, menuSections);
          }
          this.menuSections$.next(menuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private createEntityGroupSection(groupType: EntityType): MenuSection {
    const entityGroupSection = new EntityGroupSection(this.router, groupType, this.broadcast, this.entityGroupService);
    this.entityGroupSections.push(entityGroupSection);
    return entityGroupSection.getMenuSection();
  }

  private buildSysAdminMenu(authState: AuthState, disabledItems: string[]): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      },
      {
        name: 'tenant.tenants',
        type: 'link',
        path: '/tenants',
        icon: 'supervisor_account',
        disabled: disabledItems.indexOf('tenants') > -1
      },
      {
        name: 'widget.widget-library',
        type: 'link',
        path: '/widgets-bundles',
        icon: 'now_widgets',
        disabled: disabledItems.indexOf('widget_library') > -1
      },
      {
        name: 'admin.system-settings',
        type: 'toggle',
        path: '/settings',
        icon: 'settings',
        pages: of([
          {
            name: 'admin.outgoing-mail',
            type: 'link',
            path: '/settings/outgoing-mail',
            icon: 'mail',
            disabled: disabledItems.indexOf('mail_server') > -1
          },
          {
            name: 'admin.mail-templates',
            type: 'link',
            path: '/settings/mail-template',
            icon: 'format_shapes',
            disabled: disabledItems.indexOf('mail_templates') > -1
          },
          {
            name: 'white-labeling.white-labeling',
            type: 'link',
            path: '/settings/whiteLabel',
            icon: 'format_paint',
            disabled: disabledItems.indexOf('white_labeling') > -1
          },
          {
            name: 'white-labeling.login-white-labeling',
            type: 'link',
            path: '/settings/loginWhiteLabel',
            icon: 'format_paint',
            disabled: disabledItems.indexOf('login_white_labeling') > -1
          },
          {
            name: 'custom-translation.custom-translation',
            type: 'link',
            path: '/settings/customTranslation',
            icon: 'language',
            disabled: disabledItems.indexOf('custom_translation') > -1
          },
          {
            name: 'custom-menu.custom-menu',
            type: 'link',
            path: '/settings/customMenu',
            icon: 'list',
            disabled: disabledItems.indexOf('custom_menu') > -1
          },
          {
            name: 'admin.security-settings',
            type: 'link',
            path: '/settings/security-settings',
            icon: 'security',
            disabled: disabledItems.indexOf('security_settings') > -1
          }
        ])
      }
    );
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
            name: 'admin.outgoing-mail',
            icon: 'mail',
            path: '/settings/outgoing-mail',
            disabled: disabledItems.indexOf('mail_server') > -1
          },
          {
            name: 'admin.mail-templates',
            icon: 'format_shapes',
            path: '/settings/mail-template',
            disabled: disabledItems.indexOf('mail_templates') > -1
          },
          {
            name: 'admin.security-settings',
            icon: 'security',
            path: '/settings/security-settings',
            disabled: disabledItems.indexOf('security_settings') > -1
          }
        ]
      },
      {
        name: 'white-labeling.white-labeling',
        places: [
          {
            name: 'white-labeling.white-labeling',
            icon: 'format_paint',
            path: '/settings/whiteLabel',
            disabled: disabledItems.indexOf('white_labeling') > -1
          },
          {
            name: 'white-labeling.login-white-labeling',
            icon: 'format_paint',
            path: '/settings/loginWhiteLabel',
            disabled: disabledItems.indexOf('login_white_labeling') > -1
          }
        ]
      },
      {
        name: 'custom-translation.custom-translation',
        places: [
          {
            name: 'custom-translation.custom-translation',
            icon: 'language',
            path: '/settings/customTranslation',
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
            path: '/settings/customMenu',
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
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)) {
      sections.push(
        {
          name: 'rulechain.rulechains',
          type: 'link',
          path: '/ruleChains',
          icon: 'settings_ethernet',
          disabled: disabledItems.indexOf('rule_chains') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)) {
      sections.push(
        {
          name: 'converter.converters',
          type: 'link',
          path: '/converters',
          icon: 'transform',
          disabled: disabledItems.indexOf('converters') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)) {
      sections.push(
        {
          name: 'integration.integrations',
          type: 'link',
          path: '/integrations',
          icon: 'input',
          disabled: disabledItems.indexOf('integrations') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      sections.push(
        {
          name: 'role.roles',
          type: 'link',
          path: '/roles',
          icon: 'security',
          disabled: disabledItems.indexOf('roles') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      sections.push(
        {
          name: 'customers-hierarchy.customers-hierarchy',
          type: 'link',
          path: '/customers-hierarchy',
          icon: 'sort',
          disabled: disabledItems.indexOf('customers_hierarchy') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.USER) && disabledItems.indexOf('user_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.USER));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER) && disabledItems.indexOf('customer_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.CUSTOMER));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ASSET) && disabledItems.indexOf('asset_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.ASSET));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DEVICE) && disabledItems.indexOf('device_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.DEVICE));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ENTITY_VIEW) && disabledItems.indexOf('entity_view_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.ENTITY_VIEW));
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)) {
      sections.push(
        {
          name: 'widget.widget-library',
          type: 'link',
          path: '/widgets-bundles',
          icon: 'now_widgets',
          disabled: disabledItems.indexOf('widget_library') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD) && disabledItems.indexOf('dashboard_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.DASHBOARD));
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      sections.push(
        {
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/scheduler',
          icon: 'schedule',
          disabled: disabledItems.indexOf('scheduler') > -1
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      sections.push(
        {
          name: 'white-labeling.white-labeling',
          type: 'toggle',
          path: '/settings',
          icon: 'format_paint',
          pages: of([
            {
              name: 'admin.outgoing-mail',
              type: 'link',
              path: '/settings/outgoing-mail',
              icon: 'mail',
              disabled: disabledItems.indexOf('mail_server') > -1
            },
            {
              name: 'admin.mail-templates',
              type: 'link',
              path: '/settings/mail-template',
              icon: 'format_shapes',
              disabled: disabledItems.indexOf('mail_templates') > -1
            },
            {
              name: 'custom-translation.custom-translation',
              type: 'link',
              path: '/settings/customTranslation',
              icon: 'language',
              disabled: disabledItems.indexOf('custom_translation') > -1
            },
            {
              name: 'custom-menu.custom-menu',
              type: 'link',
              path: '/settings/customMenu',
              icon: 'list',
              disabled: disabledItems.indexOf('custom_menu') > -1
            },
            {
              name: 'white-labeling.white-labeling',
              type: 'link',
              path: '/settings/whiteLabel',
              icon: 'format_paint',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              type: 'link',
              path: '/settings/loginWhiteLabel',
              icon: 'format_paint',
              disabled: disabledItems.indexOf('login_white_labeling') > -1
            },
            {
              name: 'self-registration.self-registration',
              type: 'link',
              path: '/settings/selfRegistration',
              icon: 'group_add',
              disabled: disabledItems.indexOf('self_registration') > -1
            }
          ])
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      sections.push(
        {
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/auditLogs',
          icon: 'track_changes',
          disabled: disabledItems.indexOf('audit_log') > -1
        }
      );
    }
    return sections;
  }

  private buildTenantAdminHome(authState: AuthState, disabledItems: string[]): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
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
              path: '/customers-hierarchy',
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
              name: 'admin.outgoing-mail',
              icon: 'mail',
              path: '/settings/outgoing-mail',
              disabled: disabledItems.indexOf('mail_server') > -1
            },
            {
              name: 'admin.mail-templates',
              icon: 'format_shapes',
              path: '/settings/mail-template',
              disabled: disabledItems.indexOf('mail_templates') > -1
            },
            {
              name: 'white-labeling.white-labeling',
              icon: 'format_paint',
              path: '/settings/whiteLabel',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/settings/loginWhiteLabel',
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
              path: '/settings/customTranslation',
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
              path: '/settings/customMenu',
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
    return homeSections;
  }

  private buildCustomerUserMenu(authState: AuthState, disabledItems: string[]): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home',
        disabled: disabledItems.indexOf('home') > -1
      }
    );
    if (this.userPermissionsService.hasReadGenericPermission(Resource.ROLE)) {
      sections.push(
        {
          name: 'role.roles',
          type: 'link',
          path: '/roles',
          icon: 'security',
          disabled: disabledItems.indexOf('roles') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)) {
      sections.push(
        {
          name: 'customers-hierarchy.customers-hierarchy',
          type: 'link',
          path: '/customers-hierarchy',
          icon: 'sort',
          disabled: disabledItems.indexOf('customers_hierarchy') > -1
        }
      );
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.USER) && disabledItems.indexOf('user_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.USER));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER) && disabledItems.indexOf('customer_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.CUSTOMER));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ASSET) && disabledItems.indexOf('asset_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.ASSET));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DEVICE) && disabledItems.indexOf('device_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.DEVICE));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.ENTITY_VIEW) && disabledItems.indexOf('entity_view_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.ENTITY_VIEW));
    }
    if (this.userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD) && disabledItems.indexOf('dashboard_groups') === -1) {
      sections.push(this.createEntityGroupSection(EntityType.DASHBOARD));
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)) {
      sections.push(
        {
          name: 'scheduler.scheduler',
          type: 'link',
          path: '/scheduler',
          icon: 'schedule',
          disabled: disabledItems.indexOf('scheduler') > -1
        }
      );
    }
    if (authState.whiteLabelingAllowed && this.userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)) {
      sections.push(
        {
          name: 'white-labeling.white-labeling',
          type: 'toggle',
          path: '/settings',
          icon: 'format_paint',
          pages: of([
            {
              name: 'custom-translation.custom-translation',
              type: 'link',
              path: '/settings/customTranslation',
              icon: 'language',
              disabled: disabledItems.indexOf('custom_translation') > -1
            },
            {
              name: 'custom-menu.custom-menu',
              type: 'link',
              path: '/settings/customMenu',
              icon: 'list',
              disabled: disabledItems.indexOf('custom_menu') > -1
            },
            {
              name: 'white-labeling.white-labeling',
              type: 'link',
              path: '/settings/whiteLabel',
              icon: 'format_paint',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              type: 'link',
              path: '/settings/loginWhiteLabel',
              icon: 'format_paint',
              disabled: disabledItems.indexOf('login_white_labeling') > -1
            }
          ])
        }
      );
    }
    if (this.userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)) {
      sections.push(
        {
          name: 'audit-log.audit-logs',
          type: 'link',
          path: '/auditLogs',
          icon: 'track_changes',
          disabled: disabledItems.indexOf('audit_log') > -1
        }
      );
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
              path: '/customers-hierarchy',
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
              path: '/settings/whiteLabel',
              disabled: disabledItems.indexOf('white_labeling') > -1
            },
            {
              name: 'white-labeling.login-white-labeling',
              icon: 'format_paint',
              path: '/settings/loginWhiteLabel',
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
              path: '/settings/customTranslation',
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
              path: '/settings/customMenu',
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
    return homeSections;
  }

  private buildCustomMenu(customMenuItems: CustomMenuItem[], menuSections: MenuSection[]) {
    // TODO: Custom Menu
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

}

class EntityGroupSection {

  private section: MenuSection;

  private loadedGroupPages: Observable<Array<MenuSection>> = null;

  private groupPages: Observable<Array<MenuSection>>;

  private subscriptions: Subscription[] = [];

  private groupsChangedSubject = new Subject();

  constructor(private router: Router,
              private groupType: EntityType,
              private broadcast: BroadcastService,
              private entityGroupService: EntityGroupService) {
    this.subscriptions.push(this.broadcast.on(this.groupType + 'changed', () => {
      this.reloadGroups();
    }));
    this.groupPages = this.groupsChangedSubject.asObservable().pipe(
      mergeMap(() => this.getPages())
    );
    this.subscriptions.push(this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(
      () => {
        this.loadGroups();
      }
    ));
    this.buildMenuSection();
    this.loadGroups();
  }

  public getMenuSection(): MenuSection {
    return this.section;
  }

  public destroy() {
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
    this.subscriptions.length = 0;
  }

  private reloadGroups() {
    this.loadedGroupPages = null;
    this.loadGroups();
  }

  private loadGroups() {
    if (this.router.isActive(this.section.path, false) && !this.loadedGroupPages) {
      this.groupsChangedSubject.next();
    }
  }

  private buildMenuSection() {
    let name: string;
    let path: string;
    let icon: string;
    switch (this.groupType) {
      case EntityType.DEVICE:
        name = 'entity-group.device-groups';
        path = '/deviceGroups';
        icon = 'devices_other';
        break;
      case EntityType.ASSET:
        name = 'entity-group.asset-groups';
        path = '/assetGroups';
        icon = 'domain';
        break;
      case EntityType.ENTITY_VIEW:
        name = 'entity-group.entity-view-groups';
        path = '/entityViewGroups';
        icon = 'view_quilt';
        break;
      case EntityType.DASHBOARD:
        name = 'entity-group.dashboard-groups';
        path = '/dashboardGroups';
        icon = 'dashboard';
        break;
      case EntityType.USER:
        name = 'entity-group.user-groups';
        path = '/userGroups';
        icon = 'account_circle';
        break;
      case EntityType.CUSTOMER:
        name = 'entity-group.customer-groups';
        path = '/customerGroups';
        icon = 'supervisor_account';
        break;
    }
    this.section = {
      name,
      type: 'toggle',
      path,
      icon,
      pages: this.groupPages
    };
  }

  private getPages(): Observable<Array<MenuSection>> {
    if (!this.loadedGroupPages) {
      this.loadedGroupPages = this.entityGroupService.getEntityGroups(this.groupType).pipe(
        map((groups) => {
          const pages: MenuSection[] = [];
          groups.forEach((entityGroup) => {
            pages.push(
              {
                name: entityGroup.name,
                path: `${this.section.path}/${entityGroup.id.id}`,
                type: 'link',
                icon: this.section.icon,
                ignoreTranslate: true
              }
            );
          });
          return pages;
        }),
        publishReplay(1),
        refCount()
      );
    } else {
      return this.loadedGroupPages;
    }
  }

}

