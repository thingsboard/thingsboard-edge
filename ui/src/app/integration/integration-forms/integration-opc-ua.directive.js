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
/* eslint-disable import/no-unresolved, import/default */

import integrationOpcUaTemplate from './integration-opc-ua.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './integration-opc-ua.scss';

/*@ngInject*/
export default function IntegrationOpcUaDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationOpcUaTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupOpcUaConfiguration();
            scope.updateValidity();
        };

        function setupOpcUaConfiguration() {
            if (!scope.configuration.clientConfiguration) {
                scope.configuration.clientConfiguration = {
                    applicationUri: '',
                    host: 'localhost',
                    port: 49320,
                    scanPeriodInSeconds: 10,
                    timeoutInMillis: 5000,
                    security: 'Basic128Rsa15',
                    identity: {
                        type: 'anonymous'
                    },
                    keystore: {
                        type: '',
                        fileContent: '',
                        password: 'secret',
                        alias: 'opc-ua-extension',
                        keyPassword: 'secret',
                    },
                    mapping: []
                }
            }
        }

        scope.opcUaSecurityTypeChanged = () => {
            scope.updateValidity();
        };

        scope.opcUaKeystoreFileAdded = function($file) {
            let reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        ngModelCtrl.$setDirty();
                        let addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            scope.configuration.clientConfiguration.keystore.location = $file.name;
                            scope.configuration.clientConfiguration.keystore.fileContent = addedFile.replace(/^data.*base64,/, "");
                        }
                        scope.updateValidity();
                    }
                });
            };
            reader.readAsDataURL($file.file);
        };

        scope.clearUaKeystoreFile = () => {
            ngModelCtrl.$setDirty();
            scope.configuration.clientConfiguration.keystore.location = null;
            scope.configuration.clientConfiguration.keystore.fileContent = null;
            scope.updateValidity();
        };

        function Map() {
            this.deviceNodePattern = "Channel1\\.Device\\d+$";
            this.mappingType = 'FQN';
            this.subscriptionTags = [];
        }

        scope.addMap = (mappingList) => {
            mappingList.push(new Map());
            ngModelCtrl.$setDirty();
            scope.updateValidity();
        };

        scope.removeItem = (item, itemList) => {
            var index = itemList.indexOf(item);
            if (index > -1) {
                itemList.splice(index, 1);
            }
            ngModelCtrl.$setDirty();
            scope.updateValidity();
        };

        scope.updateValidity = () => {
            var uaMappingValid = true;
            var uaKeyStoreValid = true;
            if (!scope.configuration.clientConfiguration.mapping || !scope.configuration.clientConfiguration.mapping.length) {
                uaMappingValid = false;
            }
            if (scope.configuration.clientConfiguration.security != 'None') {
                if (!scope.configuration.clientConfiguration.keystore.fileContent) {
                    uaKeyStoreValid = false;
                }
            }
            ngModelCtrl.$setValidity('UAMapping', uaMappingValid);
            ngModelCtrl.$setValidity('UAKeyStore', uaKeyStoreValid);
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '='
        },
        link: linker
    };
}
