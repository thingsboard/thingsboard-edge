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
/*@ngInject*/
export default function EdgeController($scope, $filter, $translate, userPermissionsService, securityTypes, attributeService, edgeService, types, userService) {

    var vm = this;

    vm.subscriptionId = null;
    vm.active = '';
    vm.lastConnectTime = '';
    vm.lastDisconnectTime = '';
    vm.edgeSettings = {};
    vm.activeStatus = '';

    vm.isGroupDetailsReadOnly = userPermissionsService.hasEntityGroupPermission(securityTypes.operation.read);

    var params = {
        entityType: types.entityType.tenant,
        entityId: userService.getCurrentUser().tenantId,
        attributeScope: types.attributesScope.server.value,
        keys: Object.values(types.edgeAttributeKeys).join(","),
        config: {},
        query: {
            order: '',
            limit: 5,
            page: 1,
            search: null
        }
    };

    if (vm.isGroupDetailsReadOnly) {
        loadEdgeInfo();
    } else {
        loadEdgeName();
    }

    function loadEdgeName() {
        edgeService.getEdgeSetting().then(
            function success(edgeSettings) {
                vm.edgeSettings.name = edgeSettings.data.name;
                vm.edgeSettings.cloudType = edgeSettings.data.cloudType;
                vm.edgeSettings.edgeId = edgeSettings.data.edgeId;
                vm.edgeSettings.routingKey = edgeSettings.data.routingKey;
                vm.edgeSettings.type = edgeSettings.data.type;
            },
            function fail() {
            }
        );
    }

    function loadEdgeInfo() {
        attributeService.getEntityAttributesValues(params.entityType, params.entityId, params.attributeScope, params.keys, params.config)
            .then(function success(attributes) {
                onUpdate(attributes);
            });

        checkSubscription();

        attributeService.getEntityAttributes(params.entityType, params.entityId, params.attributeScope, params.query,
            function (attributes) {
            if (attributes && attributes.data) {
                onUpdate(attributes.data);
            }
        });
    }

    function onUpdate(attributes) {
        let edge = attributes.reduce(function (map, attribute) {
            map[attribute.key] = attribute.value;
            return map;
        }, {});
        $scope.$applyAsync(() => {
            vm.active = edge.active.toString();
            vm.lastConnectTime = $filter('date')(edge.lastConnectTime, 'yyyy-MM-dd HH:mm:ss');
            vm.lastDisconnectTime = $filter('date')(edge.lastDisconnectTime, 'yyyy-MM-dd HH:mm:ss');
            vm.edgeSettings = angular.fromJson(edge.edgeSettings);
            vm.activeStatus = vm.active === 'true' ? $translate.instant('edge.connected') : $translate.instant('edge.disconnected');
        })
    }

    function checkSubscription() {
        var newSubscriptionId = null;
        if (params.entityId && params.entityType && params.attributeScope) {
            newSubscriptionId = attributeService.subscribeForEntityAttributes(params.entityType, params.entityId, params.attributeScope);
        }
        if (vm.subscriptionId && vm.subscriptionId != newSubscriptionId) {
            attributeService.unsubscribeForEntityAttributes(vm.subscriptionId);
        }
        vm.subscriptionId = newSubscriptionId;
    }

    $scope.$on('$destroy', function() {
        attributeService.unsubscribeForEntityAttributes(vm.edgeSettings);
    });

}
