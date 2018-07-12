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
/* eslint-disable import/no-unresolved, import/default */

import activationLinkDialogTemplate from './activation-link.dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/*@ngInject*/
export default function AddUserController($scope, $mdDialog, $state, $stateParams, $document, $q, types, userService, saveItemFunction, helpLinks) {

    var vm = this;

    var tenantId = $stateParams.tenantId;
    var customerId = $stateParams.customerId;
    var usersType = $state.$current.data.usersType;

    vm.helpLinks = helpLinks;
    vm.item = {};

    vm.activationMethods = [
        {
            value: 'displayActivationLink',
            name: 'user.display-activation-link'
        },
        {
            value: 'sendActivationMail',
            name: 'user.send-activation-mail'
        }
    ];

    vm.userActivationMethod = 'displayActivationLink';

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add($event) {
        var sendActivationMail = false;
        if (vm.userActivationMethod == 'sendActivationMail') {
            sendActivationMail = true;
        }
        if (usersType === 'tenant') {
            vm.item.authority = "TENANT_ADMIN";
            vm.item.tenantId = {
                entityType: types.entityType.tenant,
                id: tenantId
            };
        } else if (usersType === 'customer') {
            vm.item.authority = "CUSTOMER_USER";
            vm.item.customerId = {
                entityType: types.entityType.customer,
                id: customerId
            };
        }
        userService.saveUser(vm.item, sendActivationMail).then(function success(item) {
            vm.item = item;
            $scope.theForm.$setPristine();
            if (vm.userActivationMethod == 'displayActivationLink') {
                userService.getActivationLink(vm.item.id.id).then(
                    function success(activationLink) {
                        displayActivationLink($event, activationLink).then(
                            function() {
                                $mdDialog.hide();
                            }
                        );
                    }
                );
            } else {
                $mdDialog.hide();
            }
        });
    }

    function displayActivationLink($event, activationLink) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'ActivationLinkDialogController',
            controllerAs: 'vm',
            templateUrl: activationLinkDialogTemplate,
            locals: {
                activationLink: activationLink
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            skipHide: true,
            targetEvent: $event
        }).then(function () {
            deferred.resolve();
        });
        return deferred.promise;
    }

}