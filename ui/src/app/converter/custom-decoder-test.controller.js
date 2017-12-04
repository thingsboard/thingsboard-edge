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

import './custom-decoder-test.scss';

import beautify from 'js-beautify';
import {utf8ToBytes} from './../common/utf8-support';

const js_beautify = beautify.js;

/*@ngInject*/
export default function CustomDecoderTestController($scope, $mdDialog, $mdExpansionPanel, $mdExpansionPanelGroup, $q, $mdUtil, types, utils, decoder) {

    var vm = this;

    vm.metadataPanelId = (Math.random()*10000).toFixed(0);
    vm.payloadPanelId = (Math.random()*10000).toFixed(0);
    vm.outputPanelId = (Math.random()*10000).toFixed(0);
    vm.$mdExpansionPanel = $mdExpansionPanel;

    vm.types = types;
    vm.decoder = decoder;

    vm.inputParams = {
        payloadContentType: types.contentType.JSON.value,
        stringContent: js_beautify(angular.toJson({devName: "devA", param1: 1, param2: "test"}), {indent_size: 4}),
        metadata: {
            integrationName: 'Test integration'
        },
        payload: null
    };

    vm.output = '';

    vm.test = test;
    vm.save = save;
    vm.cancel = cancel;

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
    });

    $mdExpansionPanel().waitFor(vm.payloadPanelId).then(() => {
        expandPanel(vm.payloadPanelId);
    });

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
        var inputString;
        if (vm.inputParams.payloadContentType == vm.types.contentType.BINARY.value) {
            inputString = utils.base64toString(vm.inputParams.stringContent);
        } else {
            inputString = vm.inputParams.stringContent;
        }
        vm.inputParams.payload = utf8ToBytes(inputString);
    }

    function test() {
        collapsePanelGroup('inputParametersPanelGroup');
        collapsePanel(vm.outputPanelId, false).then(
            () => {
                vm.output = '';
                if (checkInputParamErrors()) {
                    updateInputContent();
                    $mdUtil.nextTick(() => {
                        if (checkDecoderErrors()) {
                            var decoderFunction = new Function('payload, metadata', vm.decoder);
                            var res = decoderFunction.apply(this, [vm.inputParams.payload, vm.inputParams.metadata]);
                            vm.output = angular.toJson(res, true);
                            expandPanel(vm.outputPanelId).then(
                                () => {
                                    focusToComponent('#'+vm.outputPanelId);
                                });
                        }
                    });
                }
            });
    }

    function checkInputParamErrors() {
        $scope.$broadcast('form-submit', 'validatePayload');
        if (!$scope.theForm.payloadForm.$valid) {
            expandPanel(vm.payloadPanelId).then(
                () => {
                    focusToComponent('#'+vm.payloadPanelId);
                });
            return false;
        } else if (!$scope.theForm.metadataForm.$valid) {
            expandPanel(vm.metadataPanelId).then(
                () => {
                    focusToComponent('#'+vm.metadataPanelId);
                });
            return false;
        }
        return true;
    }

    function checkDecoderErrors() {
        $scope.$broadcast('form-submit', 'validateDecoder');
        if (!$scope.theForm.decoderForm.$valid) {
            focusToComponent('#decoderInput');
            return false;
        }
        return true;
    }

    function expandPanel(panelId) {
        var panel = vm.$mdExpansionPanel(panelId);
        if (panel.isOpen()) {
            return $q.when();
        } else {
            return panel.expand();
        }
    }

    function collapsePanelGroup(panelGroupId) {
        var panelGroup = vm.$mdExpansionPanel(panelGroupId);
        if (panelGroup.getOpen().length) {
            panelGroup.collapseAll(true);
        }
    }

    function collapsePanel(panelId, animate) {
        if (angular.isUndefined(animate)) {
            animate = true;
        }
        var panel = vm.$mdExpansionPanel(panelId);
        if (!panel.isOpen()) {
            return $q.when();
        } else {
            return panel.collapse({animation: animate});
        }
    }

    function focusToComponent(id) {
        var scrollParent = angular.element('.tb-custom-decoder-test-dialog md-dialog-content');
        scrollParent.animate({
            scrollTop: scrollParent.scrollTop() + (angular.element(id).offset().top - scrollParent.offset().top)
        }, 500);
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        updateInputContent();
        $mdUtil.nextTick(() => {
            if (checkDecoderErrors()) {
                $scope.theForm.decoderForm.$setPristine();
                $mdDialog.hide(vm.decoder);
            }
        });
    }
}
