/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
function Menu(userService, $state, $rootScope, $q, types, securityTypes, userPermissionsService, entityGroupService) {

    var authority = '';
    var sections = [];
    var homeSections = [];
    var isMenuReady = false;
    var menuReadyTasks = [];

    var entityGroupSections = [];

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
        sectionActive: sectionActive
    };

    if (userService.isUserLoaded() === true) {
        buildMenu();
    }

    service.authenticatedHandle = $rootScope.$on('authenticated', function () {
        buildMenu();
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

    function buildMenu() {
        isMenuReady = false;
        var user = userService.getCurrentUser();
        if (user) {
            sections = [];
            entityGroupSections = [];
            authority = user.authority;
            if (authority === 'SYS_ADMIN') {
                sections = [
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home'
                    },
                    {
                        name: 'tenant.tenants',
                        type: 'link',
                        state: 'home.tenants',
                        icon: 'supervisor_account'
                    },
                    {
                        name: 'widget.widget-library',
                        type: 'link',
                        state: 'home.widgets-bundles',
                        icon: 'now_widgets'
                    },
                    {
                        name: 'admin.system-settings',
                        type: 'toggle',
                        state: 'home.settings',
                        height: '200px',
                        icon: 'settings',
                        pages: [
                            {
                                name: 'admin.outgoing-mail',
                                type: 'link',
                                state: 'home.settings.outgoing-mail',
                                icon: 'mail'
                            },
                            {
                                name: 'admin.mail-templates',
                                type: 'link',
                                state: 'home.settings.mail-template',
                                icon: 'format_shapes'
                            },
                            {
                                name: 'white-labeling.white-labeling',
                                type: 'link',
                                state: 'home.settings.whiteLabel',
                                icon: 'format_paint'
                            },
                            {
                                name: 'white-labeling.login-white-labeling',
                                type: 'link',
                                state: 'home.settings.loginWhiteLabel',
                                icon: 'format_paint'
                            },
                            {
                                name: 'custom-translation.custom-translation',
                                type: 'link',
                                state: 'home.settings.customTranslation',
                                icon: 'language'
                            }
                        ]
                    }];
                homeSections =
                    [{
                        name: 'tenant.management',
                        places: [
                            {
                                name: 'tenant.tenants',
                                icon: 'supervisor_account',
                                state: 'home.tenants'
                            }
                        ]
                    },
                        {
                            name: 'widget.management',
                            places: [
                                {
                                    name: 'widget.widget-library',
                                    icon: 'now_widgets',
                                    state: 'home.widgets-bundles'
                                }
                            ]
                        },
                        {
                            name: 'admin.system-settings',
                            places: [
                                {
                                    name: 'admin.outgoing-mail',
                                    icon: 'mail',
                                    state: 'home.settings.outgoing-mail'
                                },
                                {
                                    name: 'admin.mail-templates',
                                    icon: 'format_shapes',
                                    state: 'home.settings.mail-template'
                                }
                            ]
                        },
                        {
                            name: 'white-labeling.white-labeling',
                            places: [
                                {
                                    name: 'white-labeling.white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.whiteLabel'
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel'
                                }
                            ]
                        },
                        {
                            name: 'custom-translation.custom-translation',
                            places: [
                                {
                                    name: 'custom-translation.custom-translation',
                                    icon: 'language',
                                    state: 'home.settings.customTranslation'
                                }
                            ]
                        }];
            } else if (authority === 'TENANT_ADMIN') {
                sections = [];
                sections.push(
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home'
                    }
                );
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.ruleChain)) {
                    sections.push(
                        {
                            name: 'rulechain.rulechains',
                            type: 'link',
                            state: 'home.ruleChains',
                            icon: 'settings_ethernet'
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.converter)) {
                    sections.push(
                        {
                            name: 'converter.converters',
                            type: 'link',
                            state: 'home.converters',
                            icon: 'transform'
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.integration)) {
                    sections.push(
                        {
                            name: 'integration.integrations',
                            type: 'link',
                            state: 'home.integrations',
                            icon: 'input'
                        }
                    );
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    sections.push(
                        {
                            name: 'role.roles',
                            type: 'link',
                            state: 'home.roles',
                            icon: 'security'
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(
                        {
                            name: 'customers-hierarchy.customers-hierarchy',
                            type: 'link',
                            state: 'home.customers-hierarchy',
                            icon: 'sort'
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user)) {
                    sections.push(userGroups);
                    entityGroupSections.push(userGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(customerGroups);
                    entityGroupSections.push(customerGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset)) {
                    sections.push(assetGroups);
                    entityGroupSections.push(assetGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device)) {
                    sections.push(deviceGroups);
                    entityGroupSections.push(deviceGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView)) {
                    sections.push(entityViewGroups);
                    entityGroupSections.push(entityViewGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.widgetsBundle)) {
                    sections.push(
                        {
                            name: 'widget.widget-library',
                            type: 'link',
                            state: 'home.widgets-bundles',
                            icon: 'now_widgets'
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard)) {
                    sections.push(dashboardGroups);
                    entityGroupSections.push(dashboardGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    sections.push(
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule'
                        }
                    );
                }
                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    sections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '200px',
                            icon: 'format_paint',
                            pages: [
                                {
                                    name: 'admin.outgoing-mail',
                                    type: 'link',
                                    state: 'home.settings.outgoing-mail',
                                    icon: 'mail'
                                },
                                {
                                    name: 'admin.mail-templates',
                                    type: 'link',
                                    state: 'home.settings.mail-template',
                                    icon: 'format_shapes'
                                },
                                {
                                    name: 'custom-translation.custom-translation',
                                    type: 'link',
                                    state: 'home.settings.customTranslation',
                                    icon: 'language'
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint'
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    type: 'link',
                                    state: 'home.settings.loginWhiteLabel',
                                    icon: 'format_paint'
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
                            icon: 'track_changes'
                        }
                    );
                }

                homeSections = [];
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.ruleChain)) {
                    homeSections.push(
                        {
                            name: 'rulechain.management',
                            places: [
                                {
                                    name: 'rulechain.rulechains',
                                    icon: 'settings_ethernet',
                                    state: 'home.ruleChains'
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
                                    state: 'home.converters'
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
                                    state: 'home.integrations'
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
                                    state: 'home.roles'
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
                                    state: 'home.userGroups'
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
                                    state: 'home.customerGroups'
                                },
                                {
                                    name: 'customers-hierarchy.customers-hierarchy',
                                    icon: 'sort',
                                    state: 'home.customers-hierarchy',
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
                                    state: 'home.assetGroups'
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
                                    state: 'home.deviceGroups'
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
                                    state: 'home.entityViewGroups'
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
                                state: 'home.widgets-bundles'
                            }
                        );
                    }
                    if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard)) {
                        dashboardManagement.places.push(
                            {
                                name: 'dashboard.dashboards',
                                icon: 'dashboard',
                                //state: 'home.dashboards',
                                state: 'home.dashboardGroups'
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
                                    state: 'home.scheduler'
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
                                    state: 'home.settings.outgoing-mail'
                                },
                                {
                                    name: 'admin.mail-templates',
                                    icon: 'format_shapes',
                                    state: 'home.settings.mail-template'
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.whiteLabel'
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel'
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
                                    state: 'home.settings.customTranslation'
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
                                    state: 'home.auditLogs'
                                }
                            ]
                        }
                    );
                }

            } else if (authority === 'CUSTOMER_USER') {
                sections = [];
                sections.push(
                    {
                        name: 'home.home',
                        type: 'link',
                        state: 'home.links',
                        icon: 'home'
                    }
                );
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    sections.push(
                        {
                            name: 'role.roles',
                            type: 'link',
                            state: 'home.roles',
                            icon: 'security'
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(
                        {
                            name: 'customers-hierarchy.customers-hierarchy',
                            type: 'link',
                            state: 'home.customers-hierarchy',
                            icon: 'sort'
                        }
                    );
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.user)) {
                    sections.push(userGroups);
                    entityGroupSections.push(userGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.customer)) {
                    sections.push(customerGroups);
                    entityGroupSections.push(customerGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.asset)) {
                    sections.push(assetGroups);
                    entityGroupSections.push(assetGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.device)) {
                    sections.push(deviceGroups);
                    entityGroupSections.push(deviceGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.entityView)) {
                    sections.push(entityViewGroups);
                    entityGroupSections.push(entityViewGroups);
                }
                if (userPermissionsService.hasReadGroupsPermission(types.entityType.dashboard)) {
                    sections.push(dashboardGroups);
                    entityGroupSections.push(dashboardGroups);
                }
                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.schedulerEvent)) {
                    sections.push(
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule'
                        }
                    );
                }
                if (userService.isWhiteLabelingAllowed() && userPermissionsService.hasReadGenericPermission(securityTypes.resource.whiteLabeling)) {
                    sections.push(
                        {
                            name: 'white-labeling.white-labeling',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '120px',
                            icon: 'format_paint',
                            pages: [
                                {
                                    name: 'custom-translation.custom-translation',
                                    type: 'link',
                                    state: 'home.settings.customTranslation',
                                    icon: 'language'
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint'
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    type: 'link',
                                    state: 'home.settings.loginWhiteLabel',
                                    icon: 'format_paint'
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
                            icon: 'track_changes'
                        }
                    );
                }

                homeSections = [];

                if (userPermissionsService.hasReadGenericPermission(securityTypes.resource.role)) {
                    homeSections.push(
                        {
                            name: 'role.management',
                            places: [
                                {
                                    name: 'role.roles',
                                    icon: 'security',
                                    state: 'home.roles'
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
                                    state: 'home.userGroups'
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
                                    state: 'home.customerGroups'
                                },
                                {
                                    name: 'customers-hierarchy.customers-hierarchy',
                                    icon: 'sort',
                                    state: 'home.customers-hierarchy',
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
                                    state: 'home.assetGroups'
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
                                    state: 'home.deviceGroups'
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
                                    state: 'home.entityViewGroups'
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
                                    state: 'home.dashboardGroups'
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
                                    state: 'home.scheduler'
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
                                    state: 'home.settings.whiteLabel'
                                },
                                {
                                    name: 'white-labeling.login-white-labeling',
                                    icon: 'format_paint',
                                    state: 'home.settings.loginWhiteLabel'
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
                                    state: 'home.settings.customTranslation'
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
                                    state: 'home.auditLogs'
                                }
                            ]
                        }
                    );
                }

            }

            if (authority === 'TENANT_ADMIN' || authority === 'CUSTOMER_USER') {
                initGroups();
            }
            onMenuReady();
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
        if ($state.includes(section.state)) {
            return section.height;
        } else {
            return '0px';
        }
    }

    function sectionActive(section) {
        return $state.includes(section.state);
    }

}
