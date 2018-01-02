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

import './integration.scss';

/* eslint-disable import/no-unresolved, import/default */

import integrationFieldsetTemplate from './integration-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function IntegrationDirective($compile, $templateCache, $translate, $mdExpansionPanel, utils, integrationService, toast, types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(integrationFieldsetTemplate);
        element.html(template);

        scope.metadataPanelId = (Math.random()*1000).toFixed(0);
        scope.headersFilterPanelId = (Math.random()*1000).toFixed(0);
        scope.$mdExpansionPanel = $mdExpansionPanel;
        scope.types = types;

        scope.httpEndpoint = null;

        scope.$watch('integration', function(newVal) {
            if (newVal) {
                if (!scope.integration.id) {
                    scope.integration.routingKey = utils.guid('');
                }
                if (!scope.integration.configuration) {
                    scope.integration.configuration = {};
                }
                if (!scope.integration.configuration.metadata) {
                    scope.integration.configuration.metadata = {};
                }
                if (scope.integration.type) {
                    if (types.integrationType[scope.integration.type].http) {
                        setupHttpConfiguration(scope.integration);
                    } else if (types.integrationType[scope.integration.type].mqtt) {
                        setupMqttConfiguration(scope.integration);
                    }
                }
                scope.updateValidity();
            }
        });

        scope.integrationTypeChanged = () => {
            if (types.integrationType[scope.integration.type].http) {
                if (!scope.integration.id && !scope.integration.configuration.baseUrl) {
                    scope.integration.configuration.baseUrl = utils.baseUrl();
                }
                setupHttpConfiguration(scope.integration);
            } else if (types.integrationType[scope.integration.type].mqtt) {
                setupMqttConfiguration(scope.integration);
            }
            scope.updateValidity();
        };

        function setupHttpConfiguration(integration) {
            scope.httpEndpoint = integrationService.getIntegrationHttpEndpointLink(integration);
            delete integration.configuration.clientConfiguration;
            delete integration.configuration.topicFilters;
        }

        function setupMqttConfiguration(integration) {
            if (!integration.configuration.clientConfiguration) {
                integration.configuration.clientConfiguration = {
                    connectTimeoutSec: 10,
                    credentials: {
                    }
                };
                if (integration.type == types.integrationType.AWS_IOT.value) {
                    integration.configuration.clientConfiguration.host = '';
                } else {
                    integration.configuration.clientConfiguration.host = 'localhost';
                    integration.configuration.clientConfiguration.port = 11883;
                    integration.configuration.clientConfiguration.credentials.type = types.mqttCredentialTypes.anonymous.value;
                }
            }
            if (!integration.configuration.topicFilters) {
                integration.configuration.topicFilters = [];
            }
            if (integration.type == types.integrationType.AWS_IOT.value) {
                integration.configuration.clientConfiguration.port = 8883;
                integration.configuration.clientConfiguration.ssl = true;
                integration.configuration.clientConfiguration.credentials.type = types.mqttCredentialTypes['cert.PEM'].value;
            }
        }

        scope.credentialsTypeChanged = () => {
            var type = scope.integration.configuration.clientConfiguration.credentials.type;
            scope.integration.configuration.clientConfiguration.credentials = {};
            scope.integration.configuration.clientConfiguration.credentials.type = type;
            scope.updateValidity();
        };

        scope.certFileAdded = ($file, fileType) => {
            var reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        scope.theForm.$setDirty();
                        var addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            if(fileType == "caCert") {
                                scope.integration.configuration.clientConfiguration.credentials.caCertFileName = $file.name;
                                scope.integration.configuration.clientConfiguration.credentials.caCert = addedFile;
                            }
                            if(fileType == "privateKey") {
                                scope.integration.configuration.clientConfiguration.credentials.privateKeyFileName = $file.name;
                                scope.integration.configuration.clientConfiguration.credentials.privateKey = addedFile;
                            }
                            if(fileType == "Cert") {
                                scope.integration.configuration.clientConfiguration.credentials.certFileName = $file.name;
                                scope.integration.configuration.clientConfiguration.credentials.cert = addedFile;
                            }
                        }
                        scope.updateValidity();
                    }
                });
            };
            reader.readAsText($file.file);
        };

        scope.clearCertFile = (fileType) => {
            scope.theForm.$setDirty();
            if(fileType == "caCert") {
                scope.integration.configuration.clientConfiguration.credentials.caCertFileName = null;
                scope.integration.configuration.clientConfiguration.credentials.caCert = null;
            }
            if(fileType == "privateKey") {
                scope.integration.configuration.clientConfiguration.credentials.privateKeyFileName = null;
                scope.integration.configuration.clientConfiguration.credentials.privateKey = null;
            }
            if(fileType == "Cert") {
                scope.integration.configuration.clientConfiguration.credentials.certFileName = null;
                scope.integration.configuration.clientConfiguration.credentials.cert = null;
            }
            scope.updateValidity();
        };

        scope.updateValidity = () => {
            var certsValid = true;
            var topicFiltersValid = true;
            if (scope.integration.type && types.integrationType[scope.integration.type].mqtt) {
                var credentials = scope.integration.configuration.clientConfiguration.credentials;
                if (credentials.type == types.mqttCredentialTypes['cert.PEM'].value) {
                    if (!credentials.caCert || !credentials.cert || !credentials.privateKey) {
                        certsValid = false;
                    }
                }
                if (!scope.integration.configuration.topicFilters || !scope.integration.configuration.topicFilters.length) {
                    topicFiltersValid = false;
                }
            }
            scope.theForm.$setValidity('Certs', certsValid);
            scope.theForm.$setValidity('TopicFilters', topicFiltersValid);
        };

        scope.integrationBaseUrlChanged = () => {
            if (types.integrationType[scope.integration.type].http) {
                scope.httpEndpoint = integrationService.getIntegrationHttpEndpointLink(scope.integration);
            }
        };

        scope.httpEnableSecurityChanged = () => {
            if (scope.integration.configuration.enableSecurity &&
                !scope.integration.configuration.headersFilter) {
                scope.integration.configuration.headersFilter = {};
            } else if (!scope.integration.configuration.enableSecurity) {
                delete scope.integration.configuration.headersFilter;
            }
        };

        scope.thingparkEnableSecurityChanged = () => {
              if (scope.integration.configuration.enableSecurity &&
                  !scope.integration.configuration.maxTimeDiffInSeconds) {
                  scope.integration.configuration.maxTimeDiffInSeconds = 60;
              }
        };

        scope.onIntegrationIdCopied = function() {
            toast.showSuccess($translate.instant('integration.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.onHttpEndpointCopied = function() {
            toast.showSuccess($translate.instant('integration.http-endpoint-url-copied-message'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.removeTopicFilter = (index) => {
            if (index > -1) {
                scope.integration.configuration.topicFilters.splice(index, 1);
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        };

        scope.addTopicFilter = () => {
            if (!scope.integration.configuration.topicFilters) {
                scope.integration.configuration.topicFilters = [];
            }
            scope.integration.configuration.topicFilters.push(
                {
                    filter: '',
                    qos: 0
                }
            );
            scope.theForm.$setDirty();
            scope.updateValidity();
        };

        $compile(element.contents())(scope);

    };
    return {
        restrict: "E",
        link: linker,
        scope: {
            integration: '=',
            isEdit: '=',
            theForm: '=',
            onDeleteIntegration: '&'
        }
    };
}
