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
import './entity-alias-dialog.scss';

/*@ngInject*/
export default function EntityAliasDialogController($scope, $mdDialog, $q, $filter, utils, entityService, types, isAdd, allowedEntityTypes, entityAliases, alias) {

    var vm = this;

    vm.types = types;
    vm.isAdd = isAdd;
    vm.allowedEntityTypes = allowedEntityTypes;
    if (angular.isArray(entityAliases)) {
        vm.entityAliases = entityAliases;
    } else {
        vm.entityAliases = [];
        for (var aliasId in entityAliases) {
            vm.entityAliases.push(entityAliases[aliasId]);
        }
    }
    if (vm.isAdd && !alias) {
        vm.alias = {
            alias: '',
            filter: {
                resolveMultiple: false
            }
        };
    } else {
        vm.alias = alias;
    }

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch('vm.alias.alias', function (newAlias) {
        if (newAlias) {
            var valid = true;
            var result = $filter('filter')(vm.entityAliases, {alias: newAlias}, true);
            if (result && result.length) {
                if (vm.isAdd || vm.alias.id != result[0].id) {
                    valid = false;
                }
            }
            $scope.theForm.aliasName.$setValidity('duplicateAliasName', valid);
        }
    });

    $scope.$watch('theForm.$pristine', function() {
        if ($scope.theForm && !$scope.theForm.$pristine) {
            $scope.theForm.$setValidity('entityFilter', true);
        }
    });

    function validate() {
        var deferred = $q.defer();
        var validationResult = {
            entity: null,
            stateEntity: false
        }
        entityService.resolveAliasFilter(vm.alias.filter, null, 1, true).then(
            function success(result) {
                validationResult.stateEntity = result.stateEntity;
                var entities = result.entities;
                if (entities.length) {
                    validationResult.entity = entities[0];
                }
                deferred.resolve(validationResult);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        validate().then(
            function success() {
                if (vm.isAdd) {
                    vm.alias.id = utils.guid();
                }
                $mdDialog.hide(vm.alias);
            },
            function fail() {
                $scope.theForm.$setValidity('entityFilter', false);
            }
        )
    }

}
