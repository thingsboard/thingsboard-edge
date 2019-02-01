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
import './converter-test.scss';

import Split from 'split.js';

import beautify from 'js-beautify';

const js_beautify = beautify.js;

/*@ngInject*/
export default function ConverterTestController($scope, $mdDialog, $window, $document, $timeout,
                                                    $q, $mdUtil, $translate, toast, types, utils,
                                                    converterService, onShowingCallback, isDecoder, funcBody,
                                                    debugIn) {

    var vm = this;

    vm.types = types;
    vm.isDecoder = isDecoder;
    vm.funcBody = funcBody;

    vm.inputParams = {};

    if (debugIn) {
        if (debugIn.inContentType) {
            vm.inputParams.payloadContentType = debugIn.inContentType;
            if (debugIn.inContent) {
                var inStringContent = debugIn.inContent;
                if (debugIn.inContentType == types.contentType.JSON.value) {
                    inStringContent = js_beautify(inStringContent, {indent_size: 4});
                }
                vm.inputParams.stringContent = inStringContent;
            }
        }
        if (debugIn.inMetadata) {
            vm.inputParams.metadata = angular.fromJson(debugIn.inMetadata);
        }
        if (!vm.isDecoder) {
            if (debugIn.inMsgType) {
                vm.inputParams.msgType = debugIn.inMsgType;
            }
            if (debugIn.inIntegrationMetadata) {
                vm.inputParams.integrationMetadata = angular.fromJson(debugIn.inIntegrationMetadata);
            }
        }
    }

    if (!vm.inputParams.payloadContentType) {
        vm.inputParams.payloadContentType = types.contentType.JSON.value;
    }

    if (vm.isDecoder) {
        if (!vm.inputParams.stringContent) {
            vm.inputParams.stringContent = js_beautify(angular.toJson({devName: "devA", param1: 1, param2: "test"}), {indent_size: 4});
        }
        if (!vm.inputParams.metadata) {
            vm.inputParams.metadata = {
                integrationName: 'Test integration'
            };
        }
        vm.inputParams.payload = null;
    } else {

        if (!vm.inputParams.stringContent) {
            var msg = {
                temperatureUploadFrequency: 60,
                humidityUploadFrequency: 30
            };
            vm.inputParams.stringContent = js_beautify(angular.toJson(msg), {indent_size: 4});
        }
        if (!vm.inputParams.metadata) {
            vm.inputParams.metadata = {
                'deviceName': 'sensorA',
                'deviceType': 'temp-sensor',
                'ss_serialNumber': 'SN111'
            };
        }
        if (!vm.inputParams.integrationMetadata) {
            vm.inputParams.integrationMetadata = {
                integrationName: 'Test integration'
            };
        }
        if (!vm.inputParams.msgType) {
            vm.inputParams.msgType = 'ATTRIBUTES_UPDATED';
        }
        vm.inputParams.msg = null;
    }

    vm.output = '';

    vm.test = test;
    vm.save = save;
    vm.cancel = cancel;

    if (vm.isDecoder) {
        $scope.$watch('vm.inputParams.payloadContentType', (newVal, prevVal) => {
            if (newVal && !angular.equals(newVal, prevVal)) {
                if (prevVal && prevVal == vm.types.contentType.BINARY.value) {
                    vm.inputParams.stringContent = convertContent(vm.inputParams.stringContent, newVal);
                } else if (newVal == vm.types.contentType.BINARY.value) {
                    vm.inputParams.stringContent = utils.stringToBase64(vm.inputParams.stringContent);
                } else if (newVal == vm.types.contentType.JSON.value) {
                    vm.inputParams.stringContent = js_beautify(vm.inputParams.stringContent, {indent_size: 4});
                }
            }
        })
    }

    $scope.$watch('theForm.metadataForm.$dirty', (newVal) => {
        if (newVal) {
            toast.hide();
        }
    });

    $scope.$watch('theForm.integrationMetadataForm.$dirty', (newVal) => {
        if (newVal) {
            toast.hide();
        }
    });

    onShowingCallback.onShowed = () => {
        vm.converterTestDialogElement = angular.element('.tb-converter-test-dialog');
        var w = vm.converterTestDialogElement.width();
        if (w > 0) {
            initSplitLayout();
        } else {
            $scope.$watch(
                function () {
                    return vm.converterTestDialogElement[0].offsetWidth || parseInt(vm.converterTestDialogElement.css('width'), 10);
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
            Split([angular.element('#top_panel', vm.converterTestDialogElement)[0], angular.element('#bottom_panel', vm.converterTestDialogElement)[0]], {
                sizes: [35, 65],
                gutterSize: 8,
                cursor: 'row-resize',
                direction: 'vertical',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([angular.element('#top_left_panel', vm.converterTestDialogElement)[0], angular.element('#top_right_panel', vm.converterTestDialogElement)[0]], {
                sizes: [50, 50],
                gutterSize: 8,
                cursor: 'col-resize',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([angular.element('#bottom_left_panel', vm.converterTestDialogElement)[0], angular.element('#bottom_right_panel', vm.converterTestDialogElement)[0]], {
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

    function convertContent(content, contentType) {
        var stringContent = '';
        if (contentType && content) {
            if (contentType == types.contentType.JSON.value ||
                contentType == types.contentType.TEXT.value) {
                stringContent = utils.base64toString(content);
                if (contentType == types.contentType.JSON.value) {
                    stringContent = js_beautify(stringContent, {indent_size: 4});
                }
            } else {
                stringContent = angular.copy(content);
            }
        }
        return stringContent;
    }

    function updateInputContent() {
        if (vm.isDecoder) {
            if (vm.inputParams.payloadContentType == vm.types.contentType.BINARY.value) {
                vm.inputParams.payload = angular.copy(vm.inputParams.stringContent);
            } else {
                vm.inputParams.payload = utils.stringToBase64(vm.inputParams.stringContent);
            }
        } else {
            vm.inputParams.msg = angular.copy(vm.inputParams.stringContent);
        }
    }

    function test() {
        testConverter().then(
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
            showMetadataError($translate.instant('converter.metadata-required'));
            return false;
        } else if (!vm.isDecoder && !$scope.theForm.integrationMetadataForm.$valid) {
            showIntegrationMetadataError($translate.instant('converter.integration-metadata-required'));
            return false;
        }
        return true;
    }

    function showMetadataError(error) {
        var toastParent = angular.element('#metadata-panel', vm.converterTestDialogElement);
        toast.showError(error, toastParent, 'bottom left');
    }

    function showIntegrationMetadataError(error) {
        var toastParent = angular.element('#integration-metadata-panel', vm.converterTestDialogElement);
        toast.showError(error, toastParent, 'bottom left');
    }

    function testConverter() {
        var deferred = $q.defer();
        if (checkInputParamErrors()) {
            updateInputContent();
            $mdUtil.nextTick(() => {
                var inputParams = {
                    metadata: vm.inputParams.metadata
                };
                if (vm.isDecoder) {
                    inputParams.payload = vm.inputParams.payload;
                    inputParams.decoder = vm.funcBody;
                } else {
                    inputParams.msg = vm.inputParams.msg;
                    inputParams.msgType = vm.inputParams.msgType;
                    inputParams.integrationMetdadata = vm.inputParams.integrationMetdadata;
                    inputParams.encoder = vm.funcBody;
                }
                var testPromise = vm.isDecoder ? converterService.testUpLink(inputParams) :
                    converterService.testDownLink(inputParams);
                testPromise.then(
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
        testConverter().then(() => {
            $scope.theForm.funcBodyForm.$setPristine();
            $mdDialog.hide(vm.funcBody);
        });
    }
}
