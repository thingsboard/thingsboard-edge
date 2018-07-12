/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import './extension-form.scss';

/* eslint-disable angular/log */

import extensionFormMqttTemplate from './extension-form-mqtt.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ExtensionFormHttpDirective($compile, $templateCache, $translate, types) {

    var linker = function(scope, element) {

        var template = $templateCache.get(extensionFormMqttTemplate);
        element.html(template);

        scope.types = types;
        scope.theForm = scope.$parent.theForm;

        scope.deviceNameExpressions = {
            deviceNameJsonExpression: "extension.converter-json",
            deviceNameTopicExpression: "extension.topic"
        };
        scope.deviceTypeExpressions = {
            deviceTypeJsonExpression: "extension.converter-json",
            deviceTypeTopicExpression: "extension.topic"
        };
        scope.attributeKeyExpressions = {
            attributeKeyJsonExpression: "extension.converter-json",
            attributeKeyTopicExpression: "extension.topic"
        };
        scope.requestIdExpressions = {
            requestIdJsonExpression: "extension.converter-json",
            requestIdTopicExpression: "extension.topic"
        }

        scope.extensionCustomConverterOptions = {
            useWrapMode: false,
            mode: 'json',
            showGutter: true,
            showPrintMargin: true,
            theme: 'github',
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function(_ace) {
                _ace.$blockScrolling = 1;
            }
        };

        scope.updateValidity = function () {
            if(scope.brokers.length) {
                for(let i=0;i<scope.brokers.length;i++) {
                    if(scope.brokers[i].credentials.type == scope.types.mqttCredentialTypes['cert.PEM'].value) {
                        if(!(scope.brokers[i].credentials.caCert && scope.brokers[i].credentials.privateKey && scope.brokers[i].credentials.cert)) {
                            scope.theForm.$setValidity('cert.PEM', false);
                            break;
                        } else {
                            scope.theForm.$setValidity('cert.PEM', true);
                        }
                    }
                }
            }
        };

        scope.$watch('brokers', function() {
            scope.updateValidity();
        }, true);

        scope.addBroker = function() {
            var newBroker = {
                host: "localhost",
                port: 1882,
                ssl: false,
                retryInterval: 3000,
                credentials: {type:"anonymous"},
                mapping: [],
                connectRequests: [],
                disconnectRequests: [],
                attributeRequests: [],
                attributeUpdates: [],
                serverSideRpc: []
            };
            scope.brokers.push(newBroker);
        };

        scope.removeBroker = function(broker) {
            var index = scope.brokers.indexOf(broker);
            if (index > -1) {
                scope.brokers.splice(index, 1);
            }
        };

        if(scope.isAdd) {
            scope.brokers = [];
            scope.config.brokers = scope.brokers;
            scope.addBroker();
        } else {
            scope.brokers = scope.config.brokers;
        }

        scope.addMap = function(mapping) {
            var newMap = {topicFilter:"sensors", converter:{attributes:[],timeseries:[]}};

            mapping.push(newMap);
        };

        scope.removeMap = function(map, mapping) {
            var index = mapping.indexOf(map);
            if (index > -1) {
                mapping.splice(index, 1);
            }
        };

        scope.addAttribute = function(attributes) {
            var newAttribute = {type:"", key:"", value:""};
            attributes.push(newAttribute);
        };

        scope.removeAttribute = function(attribute, attributes) {
            var index = attributes.indexOf(attribute);
            if (index > -1) {
                attributes.splice(index, 1);
            }
        };

        scope.addConnectRequest = function(requests, type) {
            var newRequest = {};
            if(type == "connect") {
                newRequest.topicFilter = "sensors/connect";
            } else {
                newRequest.topicFilter = "sensors/disconnect";
            }
            requests.push(newRequest);
        };

        scope.addAttributeRequest = function(requests) {
            var newRequest = {
                topicFilter: "sensors/attributes",
                clientScope: false,
                responseTopicExpression: "sensors/${deviceName}/attributes/${responseId}",
                valueExpression: "${attributeValue}"
            };
            requests.push(newRequest);
        };

        scope.addAttributeUpdate = function(updates) {
            var newUpdate = {
                deviceNameFilter: ".*",
                attributeFilter: ".*",
                topicExpression: "sensor/${deviceName}/${attributeKey}",
                valueExpression: "{\"${attributeKey}\":\"${attributeValue}\"}"
            }
            updates.push(newUpdate);
        };

        scope.addServerSideRpc = function(rpcRequests) {
            var newRpc = {
                deviceNameFilter: ".*",
                methodFilter: "echo",
                requestTopicExpression: "sensor/${deviceName}/request/${methodName}/${requestId}",
                responseTopicExpression: "sensor/${deviceName}/response/${methodName}/${requestId}",
                responseTimeout: 10000,
                valueExpression: "${params}"
            };
            rpcRequests.push(newRpc);
        };

        scope.changeCredentials = function(broker) {
            var type = broker.credentials.type;
            broker.credentials = {};
            broker.credentials.type = type;
        };

        scope.changeConverterType = function(map) {
            if(map.converterType == "custom"){
                map.converter = "";
            }
            if(map.converterType == "json") {
                map.converter = {attributes:[],timeseries:[]};
            }
        };

        scope.changeNameExpression = function(element, type) {
            if(element.nameExp == "deviceNameJsonExpression") {
                if(element.deviceNameTopicExpression) {
                    delete element.deviceNameTopicExpression;
                }
                if(type) {
                    element.deviceNameJsonExpression = "${$.serialNumber}";
                }
            }
            if(element.nameExp == "deviceNameTopicExpression") {
                if(element.deviceNameJsonExpression) {
                    delete element.deviceNameJsonExpression;
                }
                if(type && type == "connect") {
                    element.deviceNameTopicExpression = "(?<=sensor\\/)(.*?)(?=\\/connect)";
                }
                if(type && type == "disconnect") {
                    element.deviceNameTopicExpression = "(?<=sensor\\/)(.*?)(?=\\/disconnect)";
                }
                if(type && type == "attribute") {
                    element.deviceNameTopicExpression = "(?<=sensors\\/)(.*?)(?=\\/attributes)";
                }
            }
        };

        scope.changeTypeExpression = function(converter) {
            if(converter.typeExp == "deviceTypeJsonExpression") {
                if(converter.deviceTypeTopicExpression) {
                    delete converter.deviceTypeTopicExpression;
                }
            }
            if(converter.typeExp == "deviceTypeTopicExpression") {
                if(converter.deviceTypeJsonExpression) {
                    delete converter.deviceTypeJsonExpression;
                }
            }
        };

        scope.changeAttrKeyExpression = function(request) {
            if(request.attrKey == "attributeKeyJsonExpression") {
                if(request.attributeKeyTopicExpression) {
                    delete request.attributeKeyTopicExpression;
                }
                request.attributeKeyJsonExpression = "${$.key}";
            }
            if(request.attrKey == "attributeKeyTopicExpression") {
                if(request.attributeKeyJsonExpression) {
                    delete request.attributeKeyJsonExpression;
                }
                request.attributeKeyTopicExpression = "(?<=attributes\\/)(.*?)(?=\\/request)";
            }
        };

        scope.changeRequestIdExpression = function(request) {
            if(request.requestId == "requestIdJsonExpression") {
                if(request.requestIdTopicExpression) {
                    delete request.requestIdTopicExpression;
                }
                request.requestIdJsonExpression = "${$.requestId}";
            }
            if(request.requestId == "requestIdTopicExpression") {
                if(request.requestIdJsonExpression) {
                    delete request.requestIdJsonExpression;
                }
                request.requestIdTopicExpression = "(?<=request\\/)(.*?)($)";
            }
        };

        scope.validateCustomConverter = function(model, editorName) {
            if(model && model.length) {
                try {
                    angular.fromJson(model);
                    scope.theForm[editorName].$setValidity('converterJSON', true);
                } catch(e) {
                    scope.theForm[editorName].$setValidity('converterJSON', false);
                }
            }
        };

        scope.fileAdded = function($file, broker, fileType) {
            var reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        scope.theForm.$setDirty();
                        var addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            if(fileType == "caCert") {
                                broker.credentials.caCertFileName = $file.name;
                                broker.credentials.caCert = addedFile.replace(/^data.*base64,/, "");
                            }
                            if(fileType == "privateKey") {
                                broker.credentials.privateKeyFileName = $file.name;
                                broker.credentials.privateKey = addedFile.replace(/^data.*base64,/, "");
                            }
                            if(fileType == "Cert") {
                                broker.credentials.certFileName = $file.name;
                                broker.credentials.cert = addedFile.replace(/^data.*base64,/, "");
                            }
                        }
                    }
                });
            };
            reader.readAsDataURL($file.file);
        };

        scope.clearFile = function(broker, fileType) {
            scope.theForm.$setDirty();
            if(fileType == "caCert") {
                broker.credentials.caCertFileName = null;
                broker.credentials.caCert = null;
            }
            if(fileType == "privateKey") {
                broker.credentials.privateKeyFileName = null;
                broker.credentials.privateKey = null;
            }
            if(fileType == "Cert") {
                broker.credentials.certFileName = null;
                broker.credentials.cert = null;
            }
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

        $compile(element.contents())(scope);
    };

    return {
        restrict: "A",
        link: linker,
        scope: {
            config: "=",
            isAdd: "="
        }
    }
}