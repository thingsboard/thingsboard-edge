/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import addWidgetsBundleTemplate from './add-widgets-bundle.tpl.html';
import widgetsBundleCard from './widgets-bundle-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WidgetsBundleController(widgetService, userService, importExport, $state, $stateParams, $filter, $translate, types) {

    var widgetsBundleActionsList = [
        {
            onAction: function ($event, item) {
                exportWidgetsBundle($event, item);
            },
            name: function() { $translate.instant('action.export') },
            details: function() { return $translate.instant('widgets-bundle.export') },
            icon: "file_download"
        },
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('widgets-bundle.details') },
            details: function() { return $translate.instant('widgets-bundle.widgets-bundle-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('widgets-bundle.delete') },
            icon: "delete",
            isEnabled: isWidgetsBundleEditable
        }
   ];

    var widgetsBundleAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.create') },
            details: function() { return $translate.instant('widgets-bundle.create-new-widgets-bundle') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importWidgetsBundle($event).then(
                    function() {
                        vm.grid.refreshList();
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('widgets-bundle.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.widgetsBundleGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteWidgetsBundleTitle,
        deleteItemContentFunc: deleteWidgetsBundleText,
        deleteItemsTitleFunc: deleteWidgetsBundlesTitle,
        deleteItemsActionTitleFunc: deleteWidgetsBundlesActionTitle,
        deleteItemsContentFunc: deleteWidgetsBundlesText,

        fetchItemsFunc: fetchWidgetsBundles,
        saveItemFunc: saveWidgetsBundle,
        clickItemFunc: openWidgetsBundle,
        deleteItemFunc: deleteWidgetsBundle,

        getItemTitleFunc: getWidgetsBundleTitle,
        itemCardTemplateUrl: widgetsBundleCard,
        parentCtl: vm,

        actionsList: widgetsBundleActionsList,
        addItemActions: widgetsBundleAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addWidgetsBundleTemplate,

        addItemText: function() { return $translate.instant('widgets-bundle.add-widgets-bundle-text') },
        noItemsText: function() { return $translate.instant('widgets-bundle.no-widgets-bundles-text') },
        itemDetailsText: function() { return $translate.instant('widgets-bundle.widgets-bundle-details') },
        isSelectionEnabled: isWidgetsBundleEditable,
        isDetailsReadOnly: function(widgetsBundle) {
             return !isWidgetsBundleEditable(widgetsBundle);
        }

    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.widgetsBundleGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.widgetsBundleGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.exportWidgetsBundle = exportWidgetsBundle;

    function deleteWidgetsBundleTitle(widgetsBundle) {
        return $translate.instant('widgets-bundle.delete-widgets-bundle-title', {widgetsBundleTitle: widgetsBundle.title});
    }

    function deleteWidgetsBundleText() {
        return $translate.instant('widgets-bundle.delete-widgets-bundle-text');
    }

    function deleteWidgetsBundlesTitle(selectedCount) {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-title', {count: selectedCount}, 'messageformat');
    }

    function deleteWidgetsBundlesActionTitle(selectedCount) {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteWidgetsBundlesText() {
        return $translate.instant('widgets-bundle.delete-widgets-bundles-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchWidgetsBundles(pageLink) {
        return widgetService.getAllWidgetsBundlesByPageLink(pageLink);
    }

    function saveWidgetsBundle(widgetsBundle) {
        return widgetService.saveWidgetsBundle(widgetsBundle);
    }

    function deleteWidgetsBundle(widgetsBundleId) {
        return widgetService.deleteWidgetsBundle(widgetsBundleId);
    }

    function getWidgetsBundleTitle(widgetsBundle) {
        return widgetsBundle ? widgetsBundle.title : '';
    }

    function isWidgetsBundleEditable(widgetsBundle) {
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            return widgetsBundle && widgetsBundle.tenantId.id != types.id.nullUid;
        } else {
            return userService.getAuthority() === 'SYS_ADMIN';
        }
    }

    function exportWidgetsBundle($event, widgetsBundle) {
        $event.stopPropagation();
        importExport.exportWidgetsBundle(widgetsBundle.id.id);
    }

    function openWidgetsBundle($event, widgetsBundle) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.widgets-bundles.widget-types', {widgetsBundleId: widgetsBundle.id.id});
    }

}
