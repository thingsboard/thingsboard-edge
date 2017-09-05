/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import thingsboardApiUser from '../api/user.service';

export default angular.module('thingsboard.menu', [thingsboardApiUser])
    .factory('menu', Menu)
    .name;

/*@ngInject*/
function Menu(userService, $state, $rootScope, types, entityGroupService) {

    var authority = '';
    var sections = [];
    var homeSections = [];

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

    var service = {
        getHomeSections: getHomeSections,
        getSections: getSections,
        sectionHeight: sectionHeight,
        sectionActive: sectionActive
    }

    if (userService.isUserLoaded() === true) {
        buildMenu();
    }

    service.authenticatedHandle = $rootScope.$on('authenticated', function () {
        buildMenu();
    });

    return service;

    function getSections() {
        return sections;
    }

    function getHomeSections() {
        return homeSections;
    }

    function buildMenu() {
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
                            name: 'plugin.plugins',
                            type: 'link',
                            state: 'home.plugins',
                            icon: 'extension'
                        },
                        {
                            name: 'rule.rules',
                            type: 'link',
                            state: 'home.rules',
                            icon: 'settings_ethernet'
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
                            height: '80px',
                            icon: 'settings',
                            pages: [
                                {
                                    name: 'admin.outgoing-mail',
                                    type: 'link',
                                    state: 'home.settings.outgoing-mail',
                                    icon: 'mail'
                                },
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint'
                                }
                            ]
                        }];
                    homeSections =
                        [{
                            name: 'rule-plugin.management',
                            places: [
                                {
                                    name: 'plugin.plugins',
                                    icon: 'extension',
                                    state: 'home.plugins'
                                },
                                {
                                    name: 'rule.rules',
                                    icon: 'settings_ethernet',
                                    state: 'home.rules'
                                }
                            ]
                        },
                        {
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
                                        name: 'white-labeling.white-labeling',
                                        icon: 'format_paint',
                                        state: 'home.settings.whiteLabel'
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
                            name: 'plugin.plugins',
                            type: 'link',
                            state: 'home.plugins',
                            icon: 'extension'
                        },
                        {
                            name: 'rule.rules',
                            type: 'link',
                            state: 'home.rules',
                            icon: 'settings_ethernet'
                        },
                        customerGroups,
                        assetGroups,
                        deviceGroups,
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
                            name: 'admin.system-settings',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '40px',
                            icon: 'settings',
                            pages: [
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint'
                                }
                            ]
                        }];

                    homeSections =
                        [{
                            name: 'rule-plugin.management',
                            places: [
                                {
                                    name: 'plugin.plugins',
                                    icon: 'extension',
                                    state: 'home.plugins'
                                },
                                {
                                    name: 'rule.rules',
                                    icon: 'settings_ethernet',
                                    state: 'home.rules'
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
                                name: 'admin.system-settings',
                                places: [
                                    {
                                        name: 'white-labeling.white-labeling',
                                        icon: 'format_paint',
                                        state: 'home.settings.whiteLabel'
                                    }
                                ]
                            }];

                    loadGroups(customerGroups, types.entityType.customer, 'home.customerGroups.customerGroup', 'supervisor_account');
                    loadGroups(assetGroups, types.entityType.asset, 'home.assetGroups.assetGroup', 'domain');
                    loadGroups(deviceGroups, types.entityType.device, 'home.deviceGroups.deviceGroup', 'devices_other');

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
                            name: 'dashboard.dashboards',
                            type: 'link',
                            state: 'home.dashboards',
                            icon: 'dashboard'
                        },
                        {
                            name: 'admin.system-settings',
                            type: 'toggle',
                            state: 'home.settings',
                            height: '40px',
                            icon: 'settings',
                            pages: [
                                {
                                    name: 'white-labeling.white-labeling',
                                    type: 'link',
                                    state: 'home.settings.whiteLabel',
                                    icon: 'format_paint'
                                }
                            ]
                        }];

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
                                name: 'admin.system-settings',
                                places: [
                                    {
                                        name: 'white-labeling.white-labeling',
                                        icon: 'format_paint',
                                        state: 'home.settings.whiteLabel'
                                    }
                                ]
                            }];
                }
            }
        }
    }

    function loadGroups(section, groupType, groupState, icon) {
        entityGroupService.getTenantEntityGroups(groupType).then(
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
            }
        );
        if (service[groupType + 'changeHandle']) {
            service[groupType + 'changeHandle']();
        }
        service[groupType + 'changeHandle'] = $rootScope.$on(groupType + 'changed', function () {
            loadGroups(section, groupType, groupState, icon);
        });
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
