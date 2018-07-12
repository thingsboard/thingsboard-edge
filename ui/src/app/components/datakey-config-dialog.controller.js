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
import thingsboardDatakeyConfig from './datakey-config.directive';

export default angular.module('thingsboard.dialogs.datakeyConfigDialog', [thingsboardDatakeyConfig])
    .controller('DatakeyConfigDialogController', DatakeyConfigDialogController)
    .name;

/*@ngInject*/
function DatakeyConfigDialogController($scope, $mdDialog, $q, entityService, dataKey, dataKeySettingsSchema, entityAlias, aliasController) {

    var vm = this;

    vm.dataKey = dataKey;
    vm.dataKeySettingsSchema = dataKeySettingsSchema;
    vm.entityAlias = entityAlias;
    vm.aliasController = aliasController;

    vm.hide = function () {
        $mdDialog.hide();
    };

    vm.cancel = function () {
        $mdDialog.cancel();
    };

    vm.fetchEntityKeys = function (entityAliasId, query, type) {
        var deferred = $q.defer();
        vm.aliasController.getAliasInfo(entityAliasId).then(
            function success(aliasInfo) {
                var entity = aliasInfo.currentEntity;
                if (entity) {
                    entityService.getEntityKeys(entity.entityType, entity.id, query, type, {ignoreLoading: true}).then(
                        function success(keys) {
                            deferred.resolve(keys);
                        },
                        function fail() {
                            deferred.resolve([]);
                        }
                    );
                } else {
                    deferred.resolve([]);
                }
            },
            function fail() {
                deferred.resolve([]);
            }
        );
        return deferred.promise;
    };

    vm.save = function () {
        $scope.$broadcast('form-submit');
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            $mdDialog.hide(vm.dataKey);
        }
    };
}