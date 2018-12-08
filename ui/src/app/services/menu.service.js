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
function Menu(userService, $state, $rootScope, $q, types, entityGroupService) {

    var authority = '';
    var sections = [];
    var homeSections = [];
    var isMenuReady = false;
    var menuReadyTasks = [];

    var customerGroups = {
        name: 'entity-group.customer-groups',
        type: 'toggle',
        state: 'home.customerGroups',
        height: '0px',
        icon: 'supervisor_account',
        pages: []
    };

    var assetGroups = {
        name: 'entity-group.asset-groups',
        type: 'toggle',
        state: 'home.assetGroups',
        height: '0px',
        icon: 'domain',
        pages: []
    };

    var deviceGroups = {
        name: 'entity-group.device-groups',
        type: 'toggle',
        state: 'home.deviceGroups',
        height: '0px',
        icon: 'devices_other',
        pages: []
    };

    var userGroups = {
        name: 'entity-group.user-groups',
        type: 'toggle',
        state: 'home.userGroups',
        height: '0px',
        icon: 'domain',
        pages: []
    };

    var tenantAdminSettingHomeSection = {
        name: 'white-labeling.white-labeling',
        places: []
    };

    var defaultTenantAdminSettingHomeSectionPlaces = [
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
    ];

    var tenantAdminSettingMenuSection = {
        name: 'white-labeling.white-labeling',
        type: 'toggle',
        state: 'home.settings',
        height: '0px',
        icon: 'format_paint',
        pages: []
    };

    var defaultTenantAdminSettingSectionPages = [
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
        }
    ];

    var customerAdminSettingMenuSection = {
        name: 'white-labeling.white-labeling',
        type: 'toggle',
        state: 'home.settings',
        height: '0px',
        icon: 'format_paint',
        pages: []
    };

    var defaultCustomerAdminSettingMenuSectionPages = [
        {
            name: 'custom-translation.custom-translation',
            type: 'link',
            state: 'home.settings.customTranslation',
            icon: 'language'
        }
    ];

    var whiteLabelingPages = [
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
        }];

    var tenantWhiteLabelingPlaces = [
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
    ];

    var customerAdminSettingHomeSection = {
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
            if (authority !== user.authority) {
                sections = [];
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
                    sections = [
                        {
                            name: 'home.home',
                            type: 'link',
                            state: 'home.links',
                            icon: 'home'
                        },
                        {
                            name: 'rulechain.rulechains',
                            type: 'link',
                            state: 'home.ruleChains',
                            icon: 'settings_ethernet'
                        },
                        {
                            name: 'converter.converters',
                            type: 'link',
                            state: 'home.converters',
                            icon: 'transform'
                        },
                        {
                            name: 'integration.integrations',
                            type: 'link',
                            state: 'home.integrations',
                            icon: 'input'
                        },
                        {
                            name: 'role.roles',
                            type: 'link',
                            state: 'home.roles',
                            icon: 'security'
                        },
                        customerGroups,
                        assetGroups,
                        deviceGroups,
                        userGroups,
                        {
                            name: 'entity-view.entity-views',
                            type: 'link',
                            state: 'home.entityViews',
                            icon: 'view_quilt'
                        },
                        {
                            name: 'widget.widget-library',
                            type: 'link',
                            state: 'home.widgets-bundles',
                            icon: 'now_widgets'
                        },
                        {
                            name: 'dashboard.dashboards',
                            type: 'link',
                            state: 'home.dashboards',
                            icon: 'dashboards'
                        },
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule'
                        },
                        tenantAdminSettingMenuSection,
                        {
                            name: 'audit-log.audit-logs',
                            type: 'link',
                            state: 'home.auditLogs',
                            icon: 'track_changes'
                        }];

                    homeSections =
                        [{
                            name: 'rulechain.management',
                            places: [
                                {
                                    name: 'rulechain.rulechains',
                                    icon: 'settings_ethernet',
                                    state: 'home.ruleChains'
                                }
                            ]
                        },
                        {
                            name: 'converter.management',
                            places: [
                                {
                                    name: 'converter.converters',
                                    icon: 'transform',
                                    state: 'home.converters'
                                }
                            ]
                        },
                        {
                            name: 'integration.management',
                            places: [
                                {
                                    name: 'integration.integrations',
                                    icon: 'input',
                                    state: 'home.integrations'
                                }
                            ]
                        },
                        {
                            name: 'role.management',
                            places: [
                                {
                                    name: 'role.roles',
                                    icon: 'security',
                                    state: 'home.roles'
                                }
                            ]
                        },
                        {
                            name: 'customer.management',
                            places: [
                                {
                                    name: 'customer.customers',
                                    icon: 'supervisor_account',
                                    //state: 'home.customers',
                                    state: 'home.customerGroups'
                                }
                            ]
                        },
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
                            },
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
                            },
                            {
                                name: 'entity-view.management',
                                places: [
                                    {
                                        name: 'entity-view.entity-views',
                                        icon: 'view_quilt',
                                        state: 'home.entityViews'
                                    }
                                ]
                            },
                            {
                                name: 'dashboard.management',
                                places: [
                                    {
                                        name: 'widget.widget-library',
                                        icon: 'now_widgets',
                                        state: 'home.widgets-bundles'
                                    },
                                    {
                                        name: 'dashboard.dashboards',
                                        icon: 'dashboard',
                                        state: 'home.dashboards'
                                    }
                                ]
                            },
                            {
                                name: 'scheduler.management',
                                places: [
                                    {
                                        name: 'scheduler.scheduler',
                                        icon: 'schedule',
                                        state: 'home.scheduler'
                                    }
                                ]
                            },
                            tenantAdminSettingHomeSection,
                            {
                                name: 'audit-log.audit',
                                places: [
                                    {
                                        name: 'audit-log.audit-logs',
                                        icon: 'track_changes',
                                        state: 'home.auditLogs'
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
                } else if (authority === 'CUSTOMER_USER') {
                    sections = [
                        {
                            name: 'home.home',
                            type: 'link',
                            state: 'home.links',
                            icon: 'home'
                        },
                        {
                            name: 'asset.assets',
                            type: 'link',
                            state: 'home.assets',
                            icon: 'domain'
                        },
                        {
                            name: 'device.devices',
                            type: 'link',
                            state: 'home.devices',
                            icon: 'devices_other'
                        },
                        {
                            name: 'entity-view.entity-views',
                            type: 'link',
                            state: 'home.entityViews',
                            icon: 'view_quilt'
                        },
                        {
                            name: 'dashboard.dashboards',
                            type: 'link',
                            state: 'home.dashboards',
                            icon: 'dashboard'
                        },
                        {
                            name: 'scheduler.scheduler',
                            type: 'link',
                            state: 'home.scheduler',
                            icon: 'schedule'
                        },
                        customerAdminSettingMenuSection
                    ];

                    homeSections =
                        [{
                            name: 'asset.view-assets',
                            places: [
                                {
                                    name: 'asset.assets',
                                    icon: 'domain',
                                    state: 'home.assets'
                                }
                            ]
                        },
                        {
                            name: 'device.view-devices',
                            places: [
                                {
                                    name: 'device.devices',
                                    icon: 'devices_other',
                                    state: 'home.devices'
                                }
                            ]
                        },
                        {
                            name: 'entity-view.management',
                            places: [
                                {
                                    name: 'entity-view.entity-views',
                                    icon: 'view_quilt',
                                    state: 'home.entityViews'
                                }
                            ]
                        },
                        {
                            name: 'dashboard.view-dashboards',
                            places: [
                                {
                                    name: 'dashboard.dashboards',
                                    icon: 'dashboard',
                                    state: 'home.dashboards'
                                }
                            ]
                        },
                        {
                            name: 'scheduler.management',
                            places: [
                                {
                                    name: 'scheduler.scheduler',
                                    icon: 'schedule',
                                    state: 'home.scheduler'
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
                }
                checkWhiteLabelingPermissions();
            }
            if (authority === 'TENANT_ADMIN') {
                reloadGroups().then(() => {
                    onMenuReady();
                });
            } else {
                onMenuReady();
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

    function reloadGroups() {
        var tasks = [];
        tasks.push(loadGroups(customerGroups, types.entityType.customer, 'home.customerGroups.customerGroup', 'supervisor_account'));
        tasks.push(loadGroups(assetGroups, types.entityType.asset, 'home.assetGroups.assetGroup', 'domain'));
        tasks.push(loadGroups(deviceGroups, types.entityType.device, 'home.deviceGroups.deviceGroup', 'devices_other'));
        tasks.push(loadGroups(userGroups, types.entityType.user, 'home.userGroups.userGroup', 'devices_other'));
        return $q.all(tasks);
    }

    function loadGroups(section, groupType, groupState, icon) {
        var deferred = $q.defer();
        entityGroupService.getEntityGroups(groupType).then(
            function success(entityGroups) {
                var pages = [];
                entityGroups.forEach(function(entityGroup) {
                    var page = {
                        name: entityGroup.name,
                        type: 'link',
                        state: groupState + '({entityGroupId:\''+entityGroup.id.id+'\'})',
                        ignoreTranslate: true,
                        icon: icon
                    };
                    pages.push(page);
                });
                section.height = (40 * pages.length) + 'px';
                section.pages = pages;
                deferred.resolve();
            }
        );
        if (service[groupType + 'changeHandle']) {
            service[groupType + 'changeHandle']();
        }
        service[groupType + 'changeHandle'] = $rootScope.$on(groupType + 'changed', function () {
            loadGroups(section, groupType, groupState, icon);
        });
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

    function checkWhiteLabelingPermissions() {
        var places = [];
        var pages = [];
        if (authority === 'TENANT_ADMIN') {
            if (userService.isWhiteLabelingAllowed()) {
                places = places.concat(defaultTenantAdminSettingHomeSectionPlaces);
                places = places.concat(tenantWhiteLabelingPlaces);
                tenantAdminSettingHomeSection.places = places;
                tenantAdminSettingMenuSection.height = '200px';
                pages = pages.concat(defaultTenantAdminSettingSectionPages);
                pages = pages.concat(whiteLabelingPages);
                tenantAdminSettingMenuSection.pages = pages;
            } else {
                places = places.concat(defaultTenantAdminSettingHomeSectionPlaces);
                tenantAdminSettingHomeSection.places = places;
                tenantAdminSettingMenuSection.height = '120px';
                pages = pages.concat(defaultTenantAdminSettingSectionPages);
                tenantAdminSettingMenuSection.pages = pages;
            }
        }

        if (authority === 'CUSTOMER_USER') {
            if (userService.isWhiteLabelingAllowed()) {
                homeSections.push(customerAdminSettingHomeSection);
                customerAdminSettingMenuSection.height = '120px';
                pages = pages.concat(defaultCustomerAdminSettingMenuSectionPages);
                pages = pages.concat(whiteLabelingPages);
                customerAdminSettingMenuSection.pages = pages;
            } else {
                customerAdminSettingMenuSection.height = '40px';
                pages = pages.concat(defaultCustomerAdminSettingMenuSectionPages);
                customerAdminSettingMenuSection.pages = pages;
            }
        }
    }

}
