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
/*@ngInject*/
export default function CustomMenuController($scope, $q, $translate, utils, toast, types, securityTypes, userPermissionsService, customMenuService) {

    var vm = this;

    vm.readonly = !userPermissionsService.hasGenericPermission(securityTypes.resource.whiteLabeling, securityTypes.operation.write);

    vm.types = types;
    vm.customMenu = null;
    vm.save = save;

    vm.menuPlaceholder =
        '******* Example of custom menu ******** \n*\n' +
        '* menuItems - array of custom menu items\n' +
        '* disabledMenuItems - array of ThingsBoard menu items to be disabled, available menu items names:\n*\n' +
        '* "home", "tenants", "widget_library", "mail_server", "mail_templates", "white_labeling", "login_white_labeling",\n' +
        '* "custom_translation", "custom_menu", "rule_chains", "converters", "integrations", "roles", "customers_hierarchy",\n' +
        '* "user_groups", "customer_groups", "asset_groups", "device_groups", "entity_view_groups",\n' +
        '* "dashboard_groups", "scheduler", "audit_log"\n\n' +
        angular.toJson(
            {
                disabledMenuItems: ['home'],
                menuItems: [
                    {
                        "name":"My Custom Menu",
                        "iconUrl":null,
                        "materialIcon":"menu",
                        "iframeUrl":"https://thingsboard.io",
                        "setAccessToken":false,
                        "childMenuItems":[

                        ]
                    },
                    {
                        "name":"My Custom Menu 2",
                        "iconUrl":null,
                        "materialIcon":"menu",
                        "iframeUrl":"https://thingsboard.io",
                        "setAccessToken":false,
                        "childMenuItems":[
                            {
                                "name":"My Child Menu 1",
                                "iconUrl":null,
                                "materialIcon":"menu",
                                "iframeUrl":"https://thingsboard.io",
                                "setAccessToken":false,
                                "childMenuItems":[

                                ]
                            },
                            {
                                "name":"My Child Menu 2",
                                "iconUrl":null,
                                "materialIcon":"menu",
                                "iframeUrl":"https://thingsboard.io",
                                "setAccessToken":false,
                                "childMenuItems":[

                                ]
                            }
                        ]
                    }
                ]
            }, 2
        );

    vm.showError = function (error) {
        var toastParent = angular.element('#tb-custom-menu-panel');
        toast.showError(error, toastParent, 'top left');
    };

    getCurrentCustomMenu();

    function getCurrentCustomMenu() {
        var deferred = $q.defer();
        var loadPromise = customMenuService.getCurrentCustomMenu();
        loadPromise.then(
            (customMenu) => {
                vm.customMenu = customMenu;
                vm.customMenuJson = vm.customMenu ? angular.toJson(vm.customMenu, true) : null;
                deferred.resolve();
            },
            () => {
                deferred.reject();
            });
        return deferred.promise;
    }

    function parse() {
        if (vm.customMenuJson) {
            try {
                vm.customMenu = angular.fromJson(vm.customMenuJson);
            } catch (e) {
                var details = utils.parseException(e);
                var errorInfo = 'Error parsing JSON for custom menu:';
                if (details.name) {
                    errorInfo += ' ' + details.name + ':';
                }
                if (details.message) {
                    errorInfo += ' ' + details.message;
                }
                vm.showError(errorInfo);
                return false;
            }
        } else {
            vm.customMenu = null;
        }
        return true;
    }

    function save() {
        if (parse()) {
            var savePromise = customMenuService.saveCustomMenu(vm.customMenu);
            savePromise.then(
                function success() {
                    getCurrentCustomMenu().then(() => {
                        vm.customMenuForm.$setPristine();
                    });
                });
        }
    }

}