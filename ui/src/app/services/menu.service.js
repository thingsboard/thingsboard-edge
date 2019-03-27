/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import thingsboardApiUser from '../api/user.service';

export default angular.module('thingsboard.menu', [thingsboardApiUser])
    .factory('menu', Menu)
    .name;

/*@ngInject*/
function Menu(userService, $state, $rootScope, $q, types, securityTypes, userPermissionsService, entityGroupService, customMenuService) {

    var authority = '';
    var sections = [];
    var homeSections = [];
    var isMenuReady = false;
    var menuReadyTasks = [];

    var entityGroupSections = [];

    var currentCustomSection = null;
    var currentCustomChildSection = null;

    var customerGroups = {
        name: 'entity-group.customer-groups',
        type: 'toggle',
        state: 'home.customerGroups',
        height: '0px',
        icon: 'supervisor_account',
        pages: [],
        loaded: false,
        childState: 'home.customerGroups.customerGroup',
        groupType: types.entityType.customer
    };

    var assetGroups = {
        name: 'entity-group.asset-groups',
        type: 'toggle',
        state: 'home.assetGroups',
        height: '0px',
        icon: 'domain',
        pages: [],
        loaded: false,
        childState: 'home.assetGroups.assetGroup',
        groupType: types.entityType.asset
    };

    var deviceGroups = {
        name: 'entity-group.device-groups',
        type: 'toggle',
        state: 'home.deviceGroups',
        height: '0px',
        icon: 'devices_other',
        pages: [],
        loaded: false,
        childState: 'home.deviceGroups.deviceGroup',
        groupType: types.entityType.device
    };

    var userGroups = {
        name: 'entity-group.user-groups',
        type: 'toggle',
        state: 'home.userGroups',
        height: '0px',
        icon: 'account_circle',
        pages: [],
        loaded: false,
        childState: 'home.userGroups.userGroup',
        groupType: types.entityType.user
    };

    var entityViewGroups = {
        name: 'entity-group.entity-view-groups',
        type: 'toggle',
        state: 'home.entityViewGroups',
        height: '0px',
        icon: 'view_quilt',
        pages: [],
        loaded: false,
        childState: 'home.entityViewGroups.entityViewGroup',
        groupType: types.entityType.entityView
    };

    var dashboardGroups = {
        name: 'entity-group.dashboard-groups',
        type: 'toggle',
        state: 'home.dashboardGroups',
        height: '0px',
        icon: 'dashboard',
        pages: [],
        loaded: false,
        childState: 'home.dashboardGroups.dashboardGroup',
        groupType: types.entityType.dashboard
    };

    var service = {
        getHomeSections: getHomeSections,
        getSections: getSections,
        sectionHeight: sectionHeight,
        sectionActive: sectionActive,
        getCurrentCustomSection: getCurrentCustomSection,
        getCurrentCustomChildSection: getCurrentCustomChildSection,
        getRedirectState: getRedirectState
    };

    if (userService.isUserLoaded() === true) {
        buildMenu();
    } else {
        service['userLoadedHandle'] = $rootScope.$on('userLoaded', function () {
            buildMenu();
            service['userLoadedHandle']();
        });
    }

    service.authenticatedHandle = $rootScope.$on('authenticated', function () {
        buildMenu();
    });

    if (!service['customMenuChangeHandle']) {
        service['customMenuChangeHandle'] = $rootScope.$on('customMenuChanged', function () {
            buildMenu();
        });
    }

    service.stateChangeStartHandle = $rootScope.$on('$stateChangeStart', function (evt, to, params) {
        updateCurrentCustomSection(params);
    });

    return service;

    function getSections() {
        var deferred = $q.defer();
        if (isMenuReady) {
            deferred.resolve(sections);
        } else {
            menuReadyTasks.push(
                () => {
                    deferred.resolve(sections);
                }
            );
        }
        return deferred.promise;
    }

    function getHomeSections() {
        var deferred = $q.defer();
        if (isMenuReady) {
            deferred.resolve(homeSections);
        } else {
            menuReadyTasks.push(
                () => {
                    deferred.resolve(homeSections);
                }
            );
        }
        return deferred.promise;
    }

    // menu item names:
    //
    // "home", "tenants", "widget_library", "mail_server",
    // "mail_templates", "white_labeling", "login_white_labeling", "custom_translation", "custom_menu"
    // "rule_chains", "converters", "integrations", "roles", "customers_hierarchy", "user_groups",
    // "customer_groups", "asset_groups", "device_groups", "entity_view_groups", "dashboard_groups", "scheduler",
    // "audit_log"

    function buildMenu() {
        isMenuReady = false;
        var user = userService.getCurrentUser();
        if (user) {
            sections.length = 0;
            entityGroupSections.length = 0;
            homeSections.length = 0;
            authority = user.authority;
            var customMenu = customMenuService.getCustomMenu();
            var disabledItems = [];
            if (customMenu && angular.isArray(customMenu.disabledMenuItems)) {
                disabledItems = customMenu.disabledMenuItems;
            }
            if (authority === 'SYS_ADMIN') {
                [].push.apply(sections, [
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home',
                        disabled: disabledItems.indexOf('home') > -1
                    },
                    {
                        name: 'tenant.tenants',
                        type: 'link',
                        state: 'home.tenants',
                        icon: 'supervisor_account',
                        disabled: disabledItems.indexOf('tenants') > -1
                    },
                    {
                        name: 'widget.widget-library',
                        type: 'link',
                        state: 'home.widgets-bundles',
                        icon: 'now_widgets',
                        disabled: disabledItems.indexOf('widget_library') > -1
                    },
                    {
                        name: 'admin.system-settings',
                        type: 'toggle',
                        state: 'home.settings',
                        height: '240px',
                        icon: 'settings',
                        pages: [
                            {
                                name: 'admin.outgoing-mail',
                                type: 'link',
                                state: 'home.settings.outgoing-mail',
                                icon: 'mail',
                                disabled: disabledItems.indexOf('mail_server') > -1
                            },
                            {
                                name: 'admin.mail-templates',
                                type: 'link',
                                state: 'home.settings.mail-template',
                                icon: 'format_shapes',
                                disabled: disabledItems.indexOf('mail_templates') > -1
                            },
                            {
                                name: 'white-labeling.white-labeling',
                                type: 'link',
                                state: 'home.settings.whiteLabel',
                                icon: 'format_paint',
                                disabled: disabledItems.indexOf('white_labeling') > -1
                            },
                            {
                                name: 'white-labeling.login-white-labeling',
                                type: 'link',
                                state: 'home.settings.loginWhiteLabel',
                                icon: 'format_paint',
                                disabled: disabledItems.indexOf('login_white_labeling') > -1
                            },
                            {
                                name: 'custom-translation.custom-translation',
                                type: 'link',
                                state: 'home.settings.customTranslation',
                                icon: 'language',
                                disabled: disabledItems.indexOf('custom_translation') > -1
                            },
                            {
                                name: 'custom-menu.custom-menu',
                                type: 'link',
                                state: 'home.settings.customMenu',
                                icon: 'list',
                                disabled: disabledItems.indexOf('custom_menu') > -1
                            }
                        ]
                    }]);
                [].push.apply(homeSections,
                    [{
                        name: 'tenant.management',
                        places: [
                            {
                                name: 'tenant.tenants',
                                icon: 'supervisor_account',
                                state: 'home.tenants',
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
                                    state: 'home.widgets-bundles',
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
                                    state: 'home.settings.outgoing-mail',
                                    disabled: disabledItems.indexOf('mail_server') > -1
                                },
                                {
                                    name: 'admin.mail-templates',
                                    icon: 'format_shapes',
                                    state: 'home.settings.mail-template',
                                    disabled: disabledItems.indexOf('mail_templates') > -1
                                }
                            ]
                        },
                        {
                            name: 'white-labeling.white-labeling',
                            places: [
                                {
                                    name: 'white-labeling.white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.whiteLabel',
                                    disabled: disabledItems.indexOf('white_labeling') > -1
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel',
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
                                    state: 'home.settings.customTranslation',
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
                                    state: 'home.settings.customMenu',
                                    disabled: disabledItems.indexOf('custom_menu') > -1
                                }
                            ]
                        }]);
            } else if (authority === 'TENANT_ADMIN') {
                sections.push(
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home',
                        disabled: disabledItems.indexOf('home') > -1
                    }
                );
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.ruleChain)) {
                    sections.push(
                        {
                            name: 'rulechain.rulechains',
                            type: 'link',
                            state: 'home.ruleChains',
                            icon: 'settings_ethernet',
                            disabled: disabledItems.indexOf('rule_chains') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.converter)) {
                    sections.push(
                        {
                            name: 'converter.converters',
                            type: 'link',
                            state: 'home.converters',
                            icon: 'transform',
                            disabled: disabledItems.indexOf('converters') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.integration)) {
                    sections.push(
                        {
                            name: 'integration.integrations',
                            type: 'link',
                            state: 'home.integrations',
                            icon: 'input',
                            disabled: disabledItems.indexOf('integrations') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    sections.push(
                        {
                            name: 'role.roles',
                            type: 'link',
                            state: 'home.roles',
                            icon: 'security',
                            disabled: disabledItems.indexOf('roles') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(
                        {
                            name: 'customers-hierarchy.customers-hierarchy',
                            type: 'link',
                            state: 'home.customers-hierarchy',
                            icon: 'sort',
                            disabled: disabledItems.indexOf('customers_hierarchy') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user) && disabledItems.indexOf('user_groups') === -1) {
                    sections.push(userGroups);
                    entityGroupSections.push(userGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer) && disabledItems.indexOf('customer_groups') === -1) {
                    sections.push(customerGroups);
                    entityGroupSections.push(customerGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset) && disabledItems.indexOf('asset_groups') === -1) {
                    sections.push(assetGroups);
                    entityGroupSections.push(assetGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device) && disabledItems.indexOf('device_groups') === -1) {
                    sections.push(deviceGroups);
                    entityGroupSections.push(deviceGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView) && disabledItems.indexOf('entity_view_groups') === -1) {
                    sections.push(entityViewGroups);
                    entityGroupSections.push(entityViewGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.widgetsBundle)) {
                    sections.push(
                        {
                            name: 'widget.widget-library',
                            type: 'link',
                            state: 'home.widgets-bundles',
                            icon: 'now_widgets',
                            disabled: disabledItems.indexOf('widget_library') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard) && disabledItems.indexOf('dashboard_groups') === -1) {
                    sections.push(dashboardGroups);
                    entityGroupSections.push(dashboardGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    sections.push(
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule',
                            disabled: disabledItems.indexOf('scheduler') > -1
                        }
                    );
                }
                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    sections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '240px',
                            icon: 'format_paint',
                            pages: [
                                {
                                    name: 'admin.outgoing-mail',
                                    type: 'link',
                                    state: 'home.settings.outgoing-mail',
                                    icon: 'mail',
                                    disabled: disabledItems.indexOf('mail_server') > -1
                                },
                                {
                                    name: 'admin.mail-templates',
                                    type: 'link',
                                    state: 'home.settings.mail-template',
                                    icon: 'format_shapes',
                                    disabled: disabledItems.indexOf('mail_templates') > -1
                                },
                                {
                                    name: 'custom-translation.custom-translation',
                                    type: 'link',
                                    state: 'home.settings.customTranslation',
                                    icon: 'language',
                                    disabled: disabledItems.indexOf('custom_translation') > -1
                                },
                                {
                                    name: 'custom-menu.custom-menu',
                                    type: 'link',
                                    state: 'home.settings.customMenu',
                                    icon: 'list',
                                    disabled: disabledItems.indexOf('custom_menu') > -1
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint',
                                    disabled: disabledItems.indexOf('white_labeling') > -1
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    type: 'link',
                                    state: 'home.settings.loginWhiteLabel',
                                    icon: 'format_paint',
                                    disabled: disabledItems.indexOf('login_white_labeling') > -1
                                }
                            ]
                        }
                    );
                }

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.auditLog)) {
                    sections.push(
                        {
                            name: 'audit-log.audit-logs',
                            type: 'link',
                            state: 'home.auditLogs',
                            icon: 'track_changes',
                            disabled: disabledItems.indexOf('audit_log') > -1
                        }
                    );
                }

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.ruleChain)) {
                    homeSections.push(
                        {
                            name: 'rulechain.management',
                            places: [
                                {
                                    name: 'rulechain.rulechains',
                                    icon: 'settings_ethernet',
                                    state: 'home.ruleChains',
                                    disabled: disabledItems.indexOf('rule_chains') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.converter)) {
                    homeSections.push(
                        {
                            name: 'converter.management',
                            places: [
                                {
                                    name: 'converter.converters',
                                    icon: 'transform',
                                    state: 'home.converters',
                                    disabled: disabledItems.indexOf('converters') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.integration)) {
                    homeSections.push(
                        {
                            name: 'integration.management',
                            places: [
                                {
                                    name: 'integration.integrations',
                                    icon: 'input',
                                    state: 'home.integrations',
                                    disabled: disabledItems.indexOf('integrations') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    homeSections.push(
                        {
                            name: 'role.management',
                            places: [
                                {
                                    name: 'role.roles',
                                    icon: 'security',
                                    state: 'home.roles',
                                    disabled: disabledItems.indexOf('roles') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user)) {
                    homeSections.push(
                        {
                            name: 'user.management',
                            places: [
                                {
                                    name: 'user.users',
                                    icon: 'account_circle',
                                    //state: 'home.customers',
                                    state: 'home.userGroups',
                                    disabled: disabledItems.indexOf('user_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    homeSections.push(
                        {
                            name: 'customer.management',
                            places: [
                                {
                                    name: 'customer.customers',
                                    icon: 'supervisor_account',
                                    //state: 'home.customers',
                                    state: 'home.customerGroups',
                                    disabled: disabledItems.indexOf('customer_groups') > -1
                                },
                                {
                                    name: 'customers-hierarchy.customers-hierarchy',
                                    icon: 'sort',
                                    state: 'home.customers-hierarchy',
                                    disabled: disabledItems.indexOf('customers_hierarchy') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset)) {
                    homeSections.push(
                        {
                            name: 'asset.management',
                            places: [
                                {
                                    name: 'asset.assets',
                                    icon: 'domain',
                                    //state: 'home.assets'
                                    state: 'home.assetGroups',
                                    disabled: disabledItems.indexOf('asset_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device)) {
                    homeSections.push(
                        {
                            name: 'device.management',
                            places: [
                                {
                                    name: 'device.devices',
                                    icon: 'devices_other',
                                    //state: 'home.devices',
                                    state: 'home.deviceGroups',
                                    disabled: disabledItems.indexOf('device_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView)) {
                    homeSections.push(
                        {
                            name: 'entity-view.management',
                            places: [
                                {
                                    name: 'entity-view.entity-views',
                                    icon: 'view_quilt',
                                    //state: 'home.entityViews',
                                    state: 'home.entityViewGroups',
                                    disabled: disabledItems.indexOf('entity_view_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard) ||
                    userPermissionsService.hasReadGenericPermission(securityTypes.resource.widgetsBundle)) {

                    var dashboardManagement = {
                        name: 'dashboard.management',
                        places: []
                    };

                    homeSections.push(
                        dashboardManagement
                    );
                    if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.widgetsBundle)) {
                        dashboardManagement.places.push(
                            {
                                name: 'widget.widget-library',
                                icon: 'now_widgets',
                                state: 'home.widgets-bundles',
                                disabled: disabledItems.indexOf('widget_library') > -1
                            }
                        );
                    }
                    if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard)) {
                        dashboardManagement.places.push(
                            {
                                name: 'dashboard.dashboards',
                                icon: 'dashboard',
                                //state: 'home.dashboards',
                                state: 'home.dashboardGroups',
                                disabled: disabledItems.indexOf('dashboard_groups') > -1
                            }
                        );
                    }
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    homeSections.push(
                        {
                            name: 'scheduler.management',
                            places: [
                                {
                                    name: 'scheduler.scheduler',
                                    icon: 'schedule',
                                    state: 'home.scheduler',
                                    disabled: disabledItems.indexOf('scheduler') > -1
                                }
                            ]
                        }
                    );
                }

                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    homeSections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            places: [
                                {
                                    name: 'admin.outgoing-mail',
                                    icon: 'mail',
                                    state: 'home.settings.outgoing-mail',
                                    disabled: disabledItems.indexOf('mail_server') > -1
                                },
                                {
                                    name: 'admin.mail-templates',
                                    icon: 'format_shapes',
                                    state: 'home.settings.mail-template',
                                    disabled: disabledItems.indexOf('mail_templates') > -1
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.whiteLabel',
                                    disabled: disabledItems.indexOf('white_labeling') > -1
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel',
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
                                    state: 'home.settings.customTranslation',
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
                                    state: 'home.settings.customMenu',
                                    disabled: disabledItems.indexOf('custom_menu') > -1
                                }
                            ]
                        }
                    );
                }

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.auditLog)) {
                    homeSections.push(
                        {
                            name: 'audit-log.audit',
                            places: [
                                {
                                    name: 'audit-log.audit-logs',
                                    icon: 'track_changes',
                                    state: 'home.auditLogs',
                                    disabled: disabledItems.indexOf('audit_log') > -1
                                }
                            ]
                        }
                    );
                }

            } else if (authority === 'CUSTOMER_USER') {
                sections.push(
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home',
                        disabled: disabledItems.indexOf('home') > -1
                    }
                );
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    sections.push(
                        {
                            name: 'role.roles',
                            type: 'link',
                            state: 'home.roles',
                            icon: 'security',
                            disabled: disabledItems.indexOf('roles') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(
                        {
                            name: 'customers-hierarchy.customers-hierarchy',
                            type: 'link',
                            state: 'home.customers-hierarchy',
                            icon: 'sort',
                            disabled: disabledItems.indexOf('customers_hierarchy') > -1
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user) && disabledItems.indexOf('user_groups') === -1) {
                    sections.push(userGroups);
                    entityGroupSections.push(userGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer) && disabledItems.indexOf('customer_groups') === -1) {
                    sections.push(customerGroups);
                    entityGroupSections.push(customerGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset) && disabledItems.indexOf('asset_groups') === -1) {
                    sections.push(assetGroups);
                    entityGroupSections.push(assetGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device) && disabledItems.indexOf('device_groups') === -1) {
                    sections.push(deviceGroups);
                    entityGroupSections.push(deviceGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView) && disabledItems.indexOf('entity_view_groups') === -1) {
                    sections.push(entityViewGroups);
                    entityGroupSections.push(entityViewGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard) && disabledItems.indexOf('dashboard_groups') === -1) {
                    sections.push(dashboardGroups);
                    entityGroupSections.push(dashboardGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    sections.push(
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule',
                            disabled: disabledItems.indexOf('scheduler') > -1
                        }
                    );
                }
                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    sections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '160px',
                            icon: 'format_paint',
                            pages: [
                                {
                                    name: 'custom-translation.custom-translation',
                                    type: 'link',
                                    state: 'home.settings.customTranslation',
                                    icon: 'language',
                                    disabled: disabledItems.indexOf('custom_translation') > -1
                                },
                                {
                                    name: 'custom-menu.custom-menu',
                                    type: 'link',
                                    state: 'home.settings.customMenu',
                                    icon: 'list',
                                    disabled: disabledItems.indexOf('custom_menu') > -1
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint',
                                    disabled: disabledItems.indexOf('white_labeling') > -1
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    type: 'link',
                                    state: 'home.settings.loginWhiteLabel',
                                    icon: 'format_paint',
                                    disabled: disabledItems.indexOf('login_white_labeling') > -1
                                }
                            ]
                        }
                    );
                }

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.auditLog)) {
                    sections.push(
                        {
                            name: 'audit-log.audit-logs',
                            type: 'link',
                            state: 'home.auditLogs',
                            icon: 'track_changes',
                            disabled: disabledItems.indexOf('audit_log') > -1
                        }
                    );
                }

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    homeSections.push(
                        {
                            name: 'role.management',
                            places: [
                                {
                                    name: 'role.roles',
                                    icon: 'security',
                                    state: 'home.roles',
                                    disabled: disabledItems.indexOf('roles') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user)) {
                    homeSections.push(
                        {
                            name: 'user.management',
                            places: [
                                {
                                    name: 'user.users',
                                    icon: 'account_circle',
                                    //state: 'home.customers',
                                    state: 'home.userGroups',
                                    disabled: disabledItems.indexOf('user_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    homeSections.push(
                        {
                            name: 'customer.management',
                            places: [
                                {
                                    name: 'customer.customers',
                                    icon: 'supervisor_account',
                                    //state: 'home.customers',
                                    state: 'home.customerGroups',
                                    disabled: disabledItems.indexOf('customer_groups') > -1
                                },
                                {
                                    name: 'customers-hierarchy.customers-hierarchy',
                                    icon: 'sort',
                                    state: 'home.customers-hierarchy',
                                    disabled: disabledItems.indexOf('customers_hierarchy') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset)) {
                    homeSections.push(
                        {
                            name: 'asset.management',
                            places: [
                                {
                                    name: 'asset.assets',
                                    icon: 'domain',
                                    //state: 'home.assets'
                                    state: 'home.assetGroups',
                                    disabled: disabledItems.indexOf('asset_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device)) {
                    homeSections.push(
                        {
                            name: 'device.management',
                            places: [
                                {
                                    name: 'device.devices',
                                    icon: 'devices_other',
                                    //state: 'home.devices',
                                    state: 'home.deviceGroups',
                                    disabled: disabledItems.indexOf('device_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView)) {
                    homeSections.push(
                        {
                            name: 'entity-view.management',
                            places: [
                                {
                                    name: 'entity-view.entity-views',
                                    icon: 'view_quilt',
                                    //state: 'home.entityViews',
                                    state: 'home.entityViewGroups',
                                    disabled: disabledItems.indexOf('entity_view_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard)) {
                    homeSections.push(
                        {
                            name: 'dashboard.management',
                            places: [
                                {
                                    name: 'dashboard.dashboards',
                                    icon: 'dashboard',
                                    //state: 'home.dashboards',
                                    state: 'home.dashboardGroups',
                                    disabled: disabledItems.indexOf('dashboard_groups') > -1
                                }
                            ]
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    homeSections.push(
                        {
                            name: 'scheduler.management',
                            places: [
                                {
                                    name: 'scheduler.scheduler',
                                    icon: 'schedule',
                                    state: 'home.scheduler',
                                    disabled: disabledItems.indexOf('scheduler') > -1
                                }
                            ]
                        }
                    );
                }

                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    homeSections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            places: [
                                {
                                    name: 'white-labeling.white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.whiteLabel',
                                    disabled: disabledItems.indexOf('white_labeling') > -1
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel',
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
                                    state: 'home.settings.customTranslation',
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
                                    state: 'home.settings.customMenu',
                                    disabled: disabledItems.indexOf('custom_menu') > -1
                                }
                            ]
                        }
                    );
                }
                
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.auditLog)) {
                    homeSections.push(
                        {
                            name: 'audit-log.audit',
                            places: [
                                {
                                    name: 'audit-log.audit-logs',
                                    icon: 'track_changes',
                                    state: 'home.auditLogs',
                                    disabled: disabledItems.indexOf('audit_log') > -1
                                }
                            ]
                        }
                    );
                }

            }

            if (authority === 'TENANT_ADMIN' || authority === 'CUSTOMER_USER') {
                initGroups();
            }
            var customMenuItems = [];
            if (customMenu && angular.isArray(customMenu.menuItems)) {
                customMenuItems = customMenu.menuItems;
            }
            buildCustomMenu(customMenuItems);
            updateSectionsHeight();
            onMenuReady();
        }
    }

    function updateSectionsHeight() {
        for (var i=0;i<sections.length;i++) {
            var section = sections[i];
            if (section.type === 'toggle') {
                var height = section.pages.filter((page) => !page.disabled).length * 40;
                section.height = height + 'px';
            }
        }
    }


    function onMenuReady() {
        isMenuReady = true;
        if (menuReadyTasks.length) {
            for (var i=0;i<menuReadyTasks.length;i++) {
                menuReadyTasks[i]();
            }
            menuReadyTasks.length = 0;
        }
    }

    function initGroups() {
        var groupTypes = [ types.entityType.customer,
            types.entityType.asset,
            types.entityType.device,
            types.entityType.user,
            types.entityType.entityView,
            types.entityType.dashboard
        ];
        for (var i=0;i<groupTypes.length;i++) {
            if (service[groupTypes[i] + 'changeHandle']) {
                service[groupTypes[i] + 'changeHandle']();
                service[groupTypes[i] + 'changeHandle'] = null;
            }
        }
        for (i=0;i<entityGroupSections.length;i++) {
            initGroupsType(entityGroupSections[i]);
        }
        if (!service.stateChangeSuccessHandle) {
            service.stateChangeSuccessHandle = $rootScope.$on('$stateChangeSuccess', function () {
                updateGroups();
            });
        }
    }

    function buildCustomMenu(menuItems) {
        var stateIds = {};
        for (var i=0;i<menuItems.length;i++) {
            var customMenuItem = menuItems[i];
            var stateId = getCustomMenuStateId(customMenuItem.name, stateIds);
            var customMenuSection = {
                isCustom: true,
                stateId: stateId,
                name: customMenuItem.name,
                state: 'home.iframeView({stateId: \''+stateId+'\', iframeUrl: \''+customMenuItem.iframeUrl + '\', setAccessToken: '+customMenuItem.setAccessToken+'})',
                icon: customMenuItem.materialIcon,
                iconUrl: customMenuItem.iconUrl
            };
            if (customMenuItem.childMenuItems && customMenuItem.childMenuItems.length) {
                customMenuSection.type = 'toggle';
                var pages = [];
                var childStateIds = {};
                for (var c=0;c<customMenuItem.childMenuItems.length;c++) {
                    var customMenuChildItem = customMenuItem.childMenuItems[c];
                    var childStateId = getCustomMenuStateId(customMenuChildItem.name, stateIds);
                    var customMenuChildSection = {
                        isCustom: true,
                        stateId: childStateId,
                        name: customMenuChildItem.name,
                        type: 'link',
                        state: 'home.iframeView.child({stateId: \''+stateId+'\', iframeUrl: \''+customMenuItem.iframeUrl + '\', setAccessToken: '+customMenuItem.setAccessToken+', ' +
                               'childStateId: \''+childStateId+'\', childIframeUrl: \''+customMenuChildItem.iframeUrl + '\', childSetAccessToken: '+customMenuChildItem.setAccessToken+'})',
                        icon: customMenuChildItem.materialIcon,
                        iconUrl: customMenuChildItem.iconUrl
                    };
                    pages.push(customMenuChildSection);
                    childStateIds[childStateId] = true;
                }
                customMenuSection.pages = pages;
                customMenuSection.childStateIds = childStateIds;
                customMenuSection.height = (40 * customMenuItem.childMenuItems.length) + 'px';
            } else {
                customMenuSection.type = 'link';
            }
            sections.push(customMenuSection);
        }
        updateCurrentCustomSection($state.params);
    }

    function getCustomMenuStateId(name, stateIds) {
        var origName = (' ' + name).slice(1);
        var stateId = origName;
        var inc = 1;
        while (stateIds[stateId]) {
            stateId = origName + inc;
            inc++;
        }
        stateIds[stateId] = true;
        return stateId;
    }

    function updateGroups() {
        for (var i=0;i<entityGroupSections.length;i++) {
            var section = entityGroupSections[i];
            reloadGroupsType(section);
        }
    }

    function initGroupsType(section) {
        section.loaded = false;
        section.pages = [];
        section.height = '0px';
        service[section.groupType + 'changeHandle'] = $rootScope.$on(section.groupType + 'changed', function () {
            section.loaded = false;
            reloadGroupsType(section);
        });
        reloadGroupsType(section);
    }

    function reloadGroupsType(section) {
        if ($state.includes(section.state) && !section.loaded) {
            section.loaded = true;
            loadGroupsType(section);
        }
    }

    function loadGroupsType(section) {
        var deferred = $q.defer();
        entityGroupService.getEntityGroups(section.groupType).then(
            function success(entityGroups) {
                var pages = [];
                entityGroups.forEach(function(entityGroup) {
                    var page = {
                        name: entityGroup.name,
                        type: 'link',
                        state: section.childState + '({entityGroupId:\''+entityGroup.id.id+'\'})',
                        ignoreTranslate: true,
                        icon: section.icon
                    };
                    pages.push(page);
                });
                section.height = (40 * pages.length) + 'px';
                section.pages = pages;
                deferred.resolve();
            }
        );
        return deferred.promise;
    }

    function sectionHeight(section) {
        if (stateIncludes(section)) {
            return section.height;
        } else {
            return '0px';
        }
    }

    function sectionActive(section) {
        return stateIncludes(section);
    }

    function stateIncludes(section) {
        if (section.isCustom) {
            if ($state.params) {
                if ($state.params.childStateId) {
                    return section.stateId === $state.params.childStateId || (section.childStateIds && section.childStateIds[$state.params.childStateId]);
                } else if ($state.params.stateId) {
                    return section.stateId === $state.params.stateId;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return $state.includes(section.state);
        }
    }

    function getCurrentCustomSection() {
        return currentCustomSection;
    }

    function getCurrentCustomChildSection() {
        return currentCustomChildSection;
    }

    function getRedirectState(parentState, redirectState) {
        var filtered = sections.filter((section) => section.state === parentState);
        if (filtered && filtered.length) {
            var parentSection = filtered[0];
            if (parentSection.pages) {
                var filteredPages = parentSection.pages.filter((page) => !page.disabled);
                if (filteredPages && filteredPages.length) {
                    var redirectPage = filteredPages.filter((page) => page.state === redirectState);
                    if (!redirectPage || !redirectPage.length) {
                        return filteredPages[0].state;
                    }
                }
            }
        }
        return redirectState;
    }

    function updateCurrentCustomSection(params) {
        currentCustomSection = detectCurrentCustomSection(params);
        currentCustomChildSection = detectCurrentCustomChildSection(params);
    }

    function detectCurrentCustomSection(params) {
        if (params && params.stateId) {
            var stateId = params.stateId;
            for (var i=0;i<sections.length;i++) {
                var section = sections[i];
                if (section.isCustom) {
                    if (section.stateId === stateId) {
                        return section;
                    }
                }
            }
        }
    }

    function detectCurrentCustomChildSection(params) {
        if (params && params.childStateId) {
            var stateId = params.childStateId;
            for (var i=0;i<sections.length;i++) {
                var section = sections[i];
                if (section.isCustom) {
                    if (section.pages && section.pages.length) {
                        for (var c=0;c<section.pages.length;c++) {
                            var childSection = section.pages[c];
                            if (childSection.stateId === stateId) {
                                return childSection;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}
