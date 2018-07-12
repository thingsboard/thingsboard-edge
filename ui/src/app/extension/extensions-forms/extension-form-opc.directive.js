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
import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/theme/github';

import './extension-form.scss';

/* eslint-disable angular/log */

import extensionFormOpcTemplate from './extension-form-opc.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ExtensionFormOpcDirective($compile, $templateCache, $translate, types) {


    var linker = function(scope, element) {


        function Server() {
            this.applicationName = "Thingsboard OPC-UA client";
            this.applicationUri = "";
            this.host = "localhost";
            this.port = 49320;
            this.scanPeriodInSeconds = 10;
            this.timeoutInMillis = 5000;
            this.security = "Basic128Rsa15";
            this.identity = {
                "type": "anonymous"
            };
            this.keystore = {
                "type": "PKCS12",
                "location": "example.pfx",
                "password": "secret",
                "alias": "gateway",
                "keyPassword": "secret"
            };
            this.mapping = []
        }

        function Map() {
            this.deviceNodePattern = "Channel1\\.Device\\d+$";
            this.deviceNamePattern = "Device ${_System._DeviceId}";
            this.attributes = [];
            this.timeseries = [];
        }

        function Attribute() {
            this.key = "Tag1";
            this.type = "string";
            this.value = "${Tag1}";
        }

        function Timeseries() {
            this.key = "Tag2";
            this.type = "long";
            this.value = "${Tag2}";
        }


        var template = $templateCache.get(extensionFormOpcTemplate);
        element.html(template);

        scope.types = types;
        scope.theForm = scope.$parent.theForm;


        if (!scope.configuration.servers.length) {
            scope.configuration.servers.push(new Server());
        }

        scope.addServer = function(serversList) {
            serversList.push(new Server());
            // scope.addMap(serversList[serversList.length-1].mapping);

            scope.theForm.$setDirty();
        };

        scope.addMap = function(mappingList) {
            mappingList.push(new Map());
            scope.theForm.$setDirty();
        };

        scope.addNewAttribute = function(attributesList) {
            attributesList.push(new Attribute());
            scope.theForm.$setDirty();
        };

        scope.addNewTimeseries = function(timeseriesList) {
            timeseriesList.push(new Timeseries());
            scope.theForm.$setDirty();
        };


        scope.removeItem = (item, itemList) => {
            var index = itemList.indexOf(item);
            if (index > -1) {
                itemList.splice(index, 1);
            }
            scope.theForm.$setDirty();
        };


        $compile(element.contents())(scope);


        scope.fileAdded = function($file, model, options) {
            let reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        scope.theForm.$setDirty();
                        let addedFile = event.target.result;

                        if (addedFile && addedFile.length > 0) {
                            model[options.location] = $file.name;
                            model[options.fileContent] = addedFile.replace(/^data.*base64,/, "");

                        }
                    }
                });
            };
            reader.readAsDataURL($file.file);

        };

        scope.clearFile = function(model, options) {
            scope.theForm.$setDirty();

            model[options.location] = null;
            model[options.fileContent] = null;

        };

        scope.collapseValidation = function(index, id) {
            var invalidState = angular.element('#'+id+':has(.ng-invalid)');
            if(invalidState.length) {
                invalidState.addClass('inner-invalid');
            }
        };

        scope.expandValidation = function (index, id) {
            var invalidState = angular.element('#'+id);
            invalidState.removeClass('inner-invalid');
        };

    };

    return {
        restrict: "A",
        link: linker,
        scope: {
            configuration: "=",
            isAdd: "="
        }
    }
}