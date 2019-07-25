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
        scope.$mdExpansionPanel = $mdExpansionPanel;
        scope.types = types;

        scope.$watch('integration', function(newVal) {
            if (newVal) {
                if (!scope.integration.id) {
                    scope.integration.routingKey = utils.guid('');
                    scope.integration.secret = generateSecret(20);
                }
                if (!scope.integration.configuration) {
                    scope.integration.configuration = {};
                }
                if (!scope.integration.configuration.metadata) {
                    scope.integration.configuration.metadata = {};
                }
            }
        });

        scope.integrationTypeChanged = () => {
            scope.integration.configuration = {
                metadata: {}
            };
            if (types.integrationType[scope.integration.type].remote) {
                scope.integration.remote = true;
            }
        };

        function generateSecret(length) {
            if (angular.isUndefined(length) || length == null) {
                length = 1;
            }
            var l = length > 10 ? 10 : length;
            var str = Math.random().toString(36).substr(2, l);
            if(str.length >= length){
                return str;
            }
            return str.concat(generateSecret(length - str.length));
        }

        scope.onIntegrationIdCopied = function() {
            toast.showSuccess($translate.instant('integration.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);

        scope.onIntegrationInfoCopied = function (type){
            let translateInstant = "";
            switch (type) {
                case 'key':
                    translateInstant = "integration.integration-key-copied-message";
                    break;
                case 'secret':
                    translateInstant = "integration.integration-secret-copied-message";
                    break;
            }
            toast.showSuccess($translate.instant(translateInstant), 750, angular.element(element).parent(), 'top left');
        }

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
