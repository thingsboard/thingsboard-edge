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
export default angular.module('thingsboard.directives.confirmOnExit', [])
    .directive('tbConfirmOnExit', ConfirmOnExit)
    .name;

/*@ngInject*/
function ConfirmOnExit($state, $mdDialog, $window, $filter, $parse, userService) {
    return {
        link: function ($scope, $element, $attributes) {
            $scope.confirmForm = $scope.$eval($attributes.confirmForm);
            $window.onbeforeunload = function () {
                if (userService.isAuthenticated() && (($scope.confirmForm && $scope.confirmForm.$dirty) || $scope.$eval($attributes.isDirty))) {
                    return $filter('translate')('confirm-on-exit.message');
                }
            }
            $scope.$on('$stateChangeStart', function (event, next, current, params) {
                if (userService.isAuthenticated() && (($scope.confirmForm && $scope.confirmForm.$dirty) || $scope.$eval($attributes.isDirty))) {
                    event.preventDefault();
                    var confirm = $mdDialog.confirm()
                        .title($filter('translate')('confirm-on-exit.title'))
                        .htmlContent($filter('translate')('confirm-on-exit.html-message'))
                        .ariaLabel($filter('translate')('confirm-on-exit.title'))
                        .cancel($filter('translate')('action.cancel'))
                        .ok($filter('translate')('action.ok'));
                    $mdDialog.show(confirm).then(function () {
                        if ($scope.confirmForm) {
                            $scope.confirmForm.$setPristine();
                        } else {
                            var remoteSetter = $parse($attributes.isDirty).assign;
                            remoteSetter($scope, false);
                            //$scope.isDirty = false;
                        }
                        if ($attributes.tbConfirmOnExit) {
                            $scope.$eval($attributes.tbConfirmOnExit);
                        }
                        $state.go(next.name, params);
                    }, function () {
                    });
                }
            });
        },
        scope: false
    };
}