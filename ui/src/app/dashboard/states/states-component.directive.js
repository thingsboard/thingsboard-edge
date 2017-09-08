/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
export default function StatesComponent($compile, $templateCache, $controller, statesControllerService) {

    var linker = function (scope, element) {

        function destroyStateController() {
            if (scope.statesController && angular.isFunction(scope.statesController.$onDestroy)) {
                scope.statesController.$onDestroy();
            }
        }

        function init() {

            var stateController = scope.dashboardCtrl.dashboardCtx.stateController;

            stateController.openState = function(id, params, openRightLayout) {
                if (scope.statesController) {
                    scope.statesController.openState(id, params, openRightLayout);
                }
            }

            stateController.updateState = function(id, params, openRightLayout) {
                if (scope.statesController) {
                    scope.statesController.updateState(id, params, openRightLayout);
                }
            }

            stateController.resetState = function() {
                if (scope.statesController) {
                    scope.statesController.resetState();
                }
            }

            stateController.preserveState = function() {
                if (scope.statesController) {
                    var state = scope.statesController.getStateObject();
                    statesControllerService.preserveStateControllerState(scope.statesControllerId, state);
                }
            }

            stateController.navigatePrevState = function(index) {
                if (scope.statesController) {
                    scope.statesController.navigatePrevState(index);
                }
            }

            stateController.getStateId = function() {
                if (scope.statesController) {
                    return scope.statesController.getStateId();
                } else {
                    return '';
                }
            }

            stateController.getStateParams = function() {
                if (scope.statesController) {
                    return scope.statesController.getStateParams();
                } else {
                    return {};
                }
            }

            stateController.getStateParamsByStateId = function(id) {
                if (scope.statesController) {
                    return scope.statesController.getStateParamsByStateId(id);
                } else {
                    return null;
                }
            }

            stateController.getEntityId = function(entityParamName) {
                if (scope.statesController) {
                    return scope.statesController.getEntityId(entityParamName);
                } else {
                    return null;
                }
            }

        }

        scope.$on('$destroy', function callOnDestroyHook() {
            destroyStateController();
        });

        scope.$watch('scope.dashboardCtrl', function() {
            if (scope.dashboardCtrl.dashboardCtx) {
                init();
            }
        })

        scope.$watch('statesControllerId', function(newValue) {
            if (newValue) {
                if (scope.statesController) {
                    destroyStateController();
                }
                var statesControllerInfo = statesControllerService.getStateController(scope.statesControllerId);
                if (!statesControllerInfo) {
                    //fallback to default
                    statesControllerInfo = statesControllerService.getStateController('default');
                }
                var template = $templateCache.get(statesControllerInfo.templateUrl);
                element.html(template);

                var preservedState = statesControllerService.withdrawStateControllerState(scope.statesControllerId);

                var locals = {
                    preservedState: preservedState
                };
                angular.extend(locals, {$scope: scope, $element: element});
                var controller = $controller(statesControllerInfo.controller, locals, true, 'vm');
                controller.instance = controller();
                scope.statesController = controller.instance;
                scope.statesController.dashboardCtrl = scope.dashboardCtrl;
                scope.statesController.states = scope.states;
                $compile(element.contents())(scope);
            }
        });

        scope.$watch('states', function() {
            if (scope.statesController) {
                scope.statesController.states = scope.states;
            }
        });

    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            statesControllerId: '=',
            dashboardCtrl: '=',
            states: '='
        }
    };
}
