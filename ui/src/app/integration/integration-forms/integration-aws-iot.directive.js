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

import integrationAwsIotTemplate from './integration-aws-iot.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function IntegrationAwsIotDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(integrationAwsIotTemplate);
        element.html(template);

        scope.types = types;

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            setupAwsIotConfiguration();
            scope.updateValidity();
        };

        function setupAwsIotConfiguration() {
            if (!scope.configuration.clientConfiguration) {
                scope.configuration.clientConfiguration = {
                    connectTimeoutSec: 10,
                    credentials: {
                    },
                    cleanSession: true
                };
                scope.configuration.clientConfiguration.host = '';
                scope.configuration.clientConfiguration.port = 8883;
                scope.configuration.clientConfiguration.ssl = true;
                scope.configuration.clientConfiguration.credentials.type = types.mqttCredentialTypes['cert.PEM'].value;
            }
            if (!scope.configuration.topicFilters) {
                scope.configuration.topicFilters = [];
            }
            if (!scope.configuration.downlinkTopicPattern) {
                scope.configuration.downlinkTopicPattern = "${topic}";
            }
        }

        scope.updateValidity = () => {
            var certsValid = true;
            var credentials = scope.configuration.clientConfiguration.credentials;
            if (credentials.type == types.mqttCredentialTypes['cert.PEM'].value) {
                if (!credentials.caCert || !credentials.cert || !credentials.privateKey) {
                    certsValid = false;
                }
            }
            ngModelCtrl.$setValidity('Certs', certsValid);
        };

        scope.certFileAdded = ($file, fileType) => {
            var reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        ngModelCtrl.$setDirty();
                        var addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            if(fileType == "caCert") {
                                scope.configuration.clientConfiguration.credentials.caCertFileName = $file.name;
                                scope.configuration.clientConfiguration.credentials.caCert = addedFile;
                            }
                            if(fileType == "privateKey") {
                                scope.configuration.clientConfiguration.credentials.privateKeyFileName = $file.name;
                                scope.configuration.clientConfiguration.credentials.privateKey = addedFile;
                            }
                            if(fileType == "Cert") {
                                scope.configuration.clientConfiguration.credentials.certFileName = $file.name;
                                scope.configuration.clientConfiguration.credentials.cert = addedFile;
                            }
                        }
                        scope.updateValidity();
                    }
                });
            };
            reader.readAsText($file.file);
        };

        scope.clearCertFile = (fileType) => {
            ngModelCtrl.$setDirty();
            if(fileType == "caCert") {
                scope.configuration.clientConfiguration.credentials.caCertFileName = null;
                scope.configuration.clientConfiguration.credentials.caCert = null;
            }
            if(fileType == "privateKey") {
                scope.configuration.clientConfiguration.credentials.privateKeyFileName = null;
                scope.configuration.clientConfiguration.credentials.privateKey = null;
            }
            if(fileType == "Cert") {
                scope.configuration.clientConfiguration.credentials.certFileName = null;
                scope.configuration.clientConfiguration.credentials.cert = null;
            }
            scope.updateValidity();
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
