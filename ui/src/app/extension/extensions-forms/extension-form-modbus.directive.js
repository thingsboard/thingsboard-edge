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
import 'brace/ext/language_tools';
import 'brace/ext/searchbox';
import 'brace/mode/json';
import 'brace/theme/github';

import './extension-form.scss';

/* eslint-disable angular/log */

import extensionFormModbusTemplate from './extension-form-modbus.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ExtensionFormModbusDirective($compile, $templateCache, $translate, types) {


    var linker = function(scope, element) {

        function TcpTransport() {
            this.type = "tcp",
            this.host = "localhost",
            this.port = 502,
            this.timeout = 5000,
            this.reconnect = true,
            this.rtuOverTcp = false
        }

        function UdpTransport() {
            this.type = "udp",
            this.host = "localhost",
            this.port = 502,
            this.timeout = 5000
        }

        function RtuTransport() {
            this.type = "rtu",
            this.portName = "COM1",
            this.encoding = "ascii",
            this.timeout = 5000,
            this.baudRate = 115200,
            this.dataBits = 7,
            this.stopBits = 1,
            this.parity ="even"
        }

        function Server() {
            this.transport = new TcpTransport();
            this.devices = []
        }
		
        function Device() {
            this.unitId = 1;
            this.deviceName = "";
            this.attributesPollPeriod = 1000;
            this.timeseriesPollPeriod = 1000;
            this.attributes = [];
            this.timeseries = [];
        }

        function Tag(globalPollPeriod) {
            this.tag = "";
            this.type = "long";
            this.pollPeriod = globalPollPeriod;
            this.functionCode = 3;
            this.address = 0;
            this.registerCount = 1;
            this.bit = 0;
            this.byteOrder = "BIG";
        }


        var template = $templateCache.get(extensionFormModbusTemplate);
        element.html(template);

        scope.types = types;
        scope.theForm = scope.$parent.theForm;


        if (!scope.configuration.servers.length) {
            scope.configuration.servers.push(new Server());
        }

        scope.addServer = function(serversList) {
            serversList.push(new Server());
            scope.theForm.$setDirty();
        };

        scope.addDevice = function(deviceList) {
            deviceList.push(new Device());
            scope.theForm.$setDirty();
        };

        scope.addNewAttribute = function(device) {
            device.attributes.push(new Tag(device.attributesPollPeriod));
            scope.theForm.$setDirty();
        };

        scope.addNewTimeseries = function(device) {
            device.timeseries.push(new Tag(device.timeseriesPollPeriod));
            scope.theForm.$setDirty();
        };

        scope.removeItem = (item, itemList) => {
            var index = itemList.indexOf(item);
            if (index > -1) {
                itemList.splice(index, 1);
            }
            scope.theForm.$setDirty();
        };

        scope.onTransportChanged = function(server) {
            var type = server.transport.type;

            if (type === "tcp") {
                server.transport = new TcpTransport();
            } else if (type === "udp") {
                server.transport = new UdpTransport();
            } else if (type === "rtu") {
                server.transport = new RtuTransport();
            }

            scope.theForm.$setDirty();
        };
        
        $compile(element.contents())(scope);


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
            isAdd: "=",
            readonly: "="
        }
    }
}