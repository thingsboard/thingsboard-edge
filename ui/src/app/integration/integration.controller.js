/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import addIntegrationTemplate from './add-integration.tpl.html';
import integrationCard from './integration-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function IntegrationCardController(types) {

    var vm = this;
    vm.types = types;

}

/*@ngInject*/
export function IntegrationController(integrationService, $state, $stateParams, $translate, types, securityTypes, helpLinks, userPermissionsService) {

    var integrationActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('integration.delete') },
            icon: "delete",
            isEnabled: function() {
                return userPermissionsService.hasGenericPermission(securityTypes.resource.integration, securityTypes.operation.delete);
            }
        }
    ];

    var vm = this;

    vm.types = types;

    vm.helpLinkIdForIntegration = helpLinkIdForIntegration;

    vm.integrationGridConfig = {

        resource: securityTypes.resource.integration,

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteIntegrationTitle,
        deleteItemContentFunc: deleteIntegrationText,
        deleteItemsTitleFunc: deleteIntegrationsTitle,
        deleteItemsActionTitleFunc: deleteIntegrationsActionTitle,
        deleteItemsContentFunc: deleteIntegrationsText,

        fetchItemsFunc: fetchIntegrations,
        saveItemFunc: saveIntegration,
        deleteItemFunc: deleteIntegration,

        getItemTitleFunc: getIntegrationTitle,

        itemCardController: 'IntegrationCardController',
        itemCardTemplateUrl: integrationCard,
        parentCtl: vm,

        actionsList: integrationActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addIntegrationTemplate,

        addItemText: function() { return $translate.instant('integration.add-integration-text') },
        noItemsText: function() { return $translate.instant('integration.no-integrations-text') },
        itemDetailsText: function() {
            return $translate.instant('integration.integration-details');
        },
        isSelectionEnabled: function () {
            return userPermissionsService.hasGenericPermission(securityTypes.resource.integration, securityTypes.operation.delete);
        },
        isDetailsReadOnly: function () {
            return !userPermissionsService.hasGenericPermission(securityTypes.resource.integration, securityTypes.operation.write);
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.integrationGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.integrationGridConfig.topIndex = $stateParams.topIndex;
    }

    function helpLinkIdForIntegration() {
        return helpLinks.getIntegrationLink(vm.grid.operatingItem());
    }

    function deleteIntegrationTitle(integration) {
        return $translate.instant('integration.delete-integration-title', {integrationName: integration.name});
    }

    function deleteIntegrationText() {
        return $translate.instant('integration.delete-integration-text');
    }

    function deleteIntegrationsTitle(selectedCount) {
        return $translate.instant('integration.delete-integrations-title', {count: selectedCount}, 'messageformat');
    }

    function deleteIntegrationsActionTitle(selectedCount) {
        return $translate.instant('integration.delete-integrations-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteIntegrationsText() {
        return $translate.instant('integration.delete-integrations-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchIntegrations(pageLink) {
        return integrationService.getIntegrations(pageLink);
    }

    function saveIntegration(integration) {
        return integrationService.saveIntegration(integration);
    }

    function deleteIntegration(integrationId) {
        return integrationService.deleteIntegration(integrationId);
    }

    function getIntegrationTitle(integration) {
        return integration ? integration.name : '';
    }

}
