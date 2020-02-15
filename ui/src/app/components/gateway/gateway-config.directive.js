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
import './gateway-config.scss';

/* eslint-disable import/no-unresolved, import/default */

import gatewayConfigTemplate from './gateway-config.tpl.html';
import gatewayConfigDialogTemplate from './gateway-config-dialog.tpl.html';
import beautify from "js-beautify";

/* eslint-enable import/no-unresolved, import/default */
const js_beautify = beautify.js;

export default angular.module('thingsboard.directives.gatewayConfig', [])
    .directive('tbGatewayConfig', GatewayConfig)
    .name;

/*@ngInject*/
function GatewayConfig() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled: '=ngDisabled',
            gatewayConfig: '=',
            changeAlignment: '=',
            theForm: '='
        },
        controller: GatewayConfigController,
        controllerAs: 'vm',
        templateUrl: gatewayConfigTemplate
    };
}

/*@ngInject*/
function GatewayConfigController($scope, $document, $mdDialog, $mdUtil, $window, types) {
    let vm = this;
    vm.types = types;

    vm.removeConnector = (index) => {
        if (index > -1) {
            vm.gatewayConfig.splice(index, 1);
        }
    };

    vm.addNewConnector = () => {
        vm.gatewayConfig.push({
            enabled: false,
            configType: '',
            config: {},
            name: ''
        });
    };

    vm.openConfigDialog = ($event, index, config, typeName) => {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: GatewayDialogController,
            controllerAs: 'vm',
            templateUrl: gatewayConfigDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                config: config,
                typeName: typeName
            },
            targetEvent: $event,
            fullscreen: true,
            multiple: true,
        }).then(function (config) {
            if (config && index > -1) {
                vm.gatewayConfig[index].config = config;
            }
        });

    };

    vm.changeConnectorType = (connector) => {
        for (let gatewayConfigTypeKey in types.gatewayConfigType) {
            if (types.gatewayConfigType[gatewayConfigTypeKey].value === connector.configType) {
                if (!connector.name) {
                    connector.name = generateConnectorName(types.gatewayConfigType[gatewayConfigTypeKey].name, 0);
                    break;
                }
            }
        }
    };

    vm.changeConnectorName = (connector, currentConnectorIndex) => {
        connector.name = validateConnectorName(connector.name, 0, currentConnectorIndex);
    };

    function generateConnectorName(name, index) {
        let newKeyName = index ? name + index : name;
        let indexRes = vm.gatewayConfig.findIndex((element) => element.name === newKeyName);
        return indexRes === -1 ? newKeyName : generateConnectorName(name, ++index);
    }

    function validateConnectorName(name, index, currentConnectorIndex) {
        for (let i = 0; i < vm.gatewayConfig.length; i++) {
            let nameEq = (index === 0) ? name : name + index;
            if (i !== currentConnectorIndex && vm.gatewayConfig[i].name === nameEq) {
                index++;
                validateConnectorName(name, index, currentConnectorIndex);
            }
        }
        return (index === 0) ? name : name + index;
    }

    vm.validateJSON = (config) => {
        return angular.equals({}, config);
    };
}

/*@ngInject*/
function GatewayDialogController($scope, $mdDialog, $document, $window, config, typeName) {
    let vm = this;
    vm.config = js_beautify(angular.toJson(config), {indent_size: 4});
    vm.typeName = typeName;
    vm.configAreaOptions = {
        useWrapMode: true,
        mode: 'json',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            _ace.$blockScrolling = 1;
        }
    };

    vm.validateConfig = (model, editorName) => {
        if (model && model.length) {
            try {
                angular.fromJson(model);
                $scope.theForm[editorName].$setValidity('config', true);
            } catch (e) {
                $scope.theForm[editorName].$setValidity('config', false);
            }
        }
    };

    vm.save = () => {
        $mdDialog.hide(angular.fromJson(vm.config));
    };

    vm.cancel = () => {
        $mdDialog.hide();
    };

    vm.beautifyJson = () => {
        vm.config = js_beautify(vm.config, {indent_size: 4});
    };
}

