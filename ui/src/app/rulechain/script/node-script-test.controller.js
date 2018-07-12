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
import './node-script-test.scss';

import Split from 'split.js';

import beautify from 'js-beautify';

const js_beautify = beautify.js;

/*@ngInject*/
export default function NodeScriptTestController($scope, $mdDialog, $window, $document, $timeout,
                                                $q, $mdUtil, $translate, toast, types, utils,
                                                 ruleChainService, onShowingCallback, msg, msgType, metadata,
                                                 functionTitle, inputParams) {

    var vm = this;

    vm.types = types;
    vm.functionTitle = functionTitle;
    vm.inputParams = inputParams;
    vm.inputParams.msg = js_beautify(angular.toJson(msg), {indent_size: 4});
    vm.inputParams.metadata = metadata;
    vm.inputParams.msgType = msgType;

    vm.output = '';

    vm.test = test;
    vm.save = save;
    vm.cancel = cancel;

    $scope.$watch('theForm.metadataForm.$dirty', (newVal) => {
        if (newVal) {
            toast.hide();
        }
    });

    onShowingCallback.onShowed = () => {
        vm.nodeScriptTestDialogElement = angular.element('.tb-node-script-test-dialog');
        var w = vm.nodeScriptTestDialogElement.width();
        if (w > 0) {
            initSplitLayout();
        } else {
            $scope.$watch(
                function () {
                    return vm.nodeScriptTestDialogElement[0].offsetWidth || parseInt(vm.nodeScriptTestDialogElement.css('width'), 10);
                },
                function (newSize) {
                    if (newSize > 0) {
                        initSplitLayout();
                    }
                }
            );
        }
    };

    function onDividerDrag() {
        $scope.$broadcast('update-ace-editor-size');
    }

    function initSplitLayout() {
        if (!vm.layoutInited) {
            Split([angular.element('#top_panel', vm.nodeScriptTestDialogElement)[0], angular.element('#bottom_panel', vm.nodeScriptTestDialogElement)[0]], {
                sizes: [35, 65],
                gutterSize: 8,
                cursor: 'row-resize',
                direction: 'vertical',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([angular.element('#top_left_panel', vm.nodeScriptTestDialogElement)[0], angular.element('#top_right_panel', vm.nodeScriptTestDialogElement)[0]], {
                sizes: [50, 50],
                gutterSize: 8,
                cursor: 'col-resize',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([angular.element('#bottom_left_panel', vm.nodeScriptTestDialogElement)[0], angular.element('#bottom_right_panel', vm.nodeScriptTestDialogElement)[0]], {
                sizes: [50, 50],
                gutterSize: 8,
                cursor: 'col-resize',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            onDividerDrag();

            $scope.$applyAsync(function () {
                vm.layoutInited = true;
                var w = angular.element($window);
                $timeout(function () {
                    w.triggerHandler('resize')
                });
            });

        }
    }

    function test() {
        testNodeScript().then(
            (output) => {
                vm.output = js_beautify(output, {indent_size: 4});
            }
        );
    }

    function checkInputParamErrors() {
        $scope.theForm.metadataForm.$setPristine();
        $scope.$broadcast('form-submit', 'validatePayload');
        if (!$scope.theForm.payloadForm.$valid) {
            return false;
        } else if (!$scope.theForm.metadataForm.$valid) {
            showMetadataError($translate.instant('rulenode.metadata-required'));
            return false;
        }
        return true;
    }

    function showMetadataError(error) {
        var toastParent = angular.element('#metadata-panel', vm.nodeScriptTestDialogElement);
        toast.showError(error, toastParent, 'bottom left');
    }

    function testNodeScript() {
        var deferred = $q.defer();
        if (checkInputParamErrors()) {
            $mdUtil.nextTick(() => {
                ruleChainService.testScript(vm.inputParams).then(
                    (result) => {
                        if (result.error) {
                            toast.showError(result.error);
                            deferred.reject();
                        } else {
                            deferred.resolve(result.output);
                        }
                    },
                    () => {
                        deferred.reject();
                    }
                );
            });
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        testNodeScript().then(() => {
            $scope.theForm.funcBodyForm.$setPristine();
            $mdDialog.hide(vm.inputParams.script);
        });
    }
}
