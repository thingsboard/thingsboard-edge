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
import './extensions-table-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import extensionsTableWidgetTemplate from './extensions-table-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.extensionsTableWidget', [])
    .directive('tbExtensionsTableWidget', ExtensionsTableWidget)
    .name;

/*@ngInject*/
function ExtensionsTableWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: ExtensionsTableWidgetController,
        controllerAs: 'vm',
        templateUrl: extensionsTableWidgetTemplate
    };
}

/*@ngInject*/
function ExtensionsTableWidgetController($scope, $translate, utils) {
    var vm = this;

    vm.datasources = null;
    vm.tabsHidden = false;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
        }
    });

    function initializeConfig() {

        if (vm.settings.extensionsTitle && vm.settings.extensionsTitle.length) {
            vm.extensionsTitle = utils.customTranslation(vm.settings.extensionsTitle, vm.settings.extensionsTitle);
        } else {
            vm.extensionsTitle = $translate.instant('extension.extensions');
        }
        vm.ctx.widgetTitle = vm.extensionsTitle;

        vm.ctx.widgetActions = [vm.importExtensionsAction, vm.exportExtensionsAction, vm.addAction, vm.searchAction, vm.refreshAction];
    }

    function updateDatasources() {

        var datasource = vm.datasources[0];
        vm.selectedSource = vm.datasources[0];
        vm.ctx.widgetTitle = utils.createLabelFromDatasource(datasource, vm.extensionsTitle);
    }

    vm.changeSelectedSource = function(source) {
        vm.selectedSource = source;
    };

    vm.searchAction = {
        name: "action.search",
        show: true,
        onAction: function() {
            $scope.$broadcast("showSearch", vm.selectedSource);
        },
        icon: "search"
    };

    vm.refreshAction = {
        name: "action.refresh",
        show: true,
        onAction: function() {
            $scope.$broadcast("refreshExtensions", vm.selectedSource);
        },
        icon: "refresh"
    };

    vm.addAction = {
        name: "action.add",
        show: true,
        onAction: function() {
            $scope.$broadcast("addExtension", vm.selectedSource);
        },
        icon: "add"
    };

    vm.exportExtensionsAction = {
        name: "extension.export-extensions-configuration",
        show: true,
        onAction: function() {
            $scope.$broadcast("exportExtensions", vm.selectedSource);
        },
        icon: "file_download"
    };

    vm.importExtensionsAction = {
        name: "extension.import-extensions-configuration",
        show: true,
        onAction: function() {
            $scope.$broadcast("importExtensions", vm.selectedSource);
        },
        icon: "file_upload"
    };

    $scope.$on("filterMode", function($event, mode) {
        vm.tabsHidden = mode;
    });

    $scope.$on("selectedExtensions", function($event, mode) {
        vm.tabsHidden = mode;
    });
}