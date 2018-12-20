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
/* eslint-disable import/no-unresolved, import/default */

import updateAttributesTemplate from './update-attributes.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function UpdateAttributesEventConfigDirective($compile, $templateCache, types, $mdExpansionPanel) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(updateAttributesTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;
        scope.attributeScopes = [];
        scope.attributeScopes.push(types.attributesScope.server);
        scope.attributeScopes.push(types.attributesScope.shared);

        scope.currentGroupType = null;

        scope.$watch('configuration', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
            if (!scope.configuration.msgType) {
                scope.configuration.msgType = types.messageType.POST_ATTRIBUTES_REQUEST.value;
                ngModelCtrl.$setViewValue(scope.configuration);
            }
            if (!scope.configuration.metadata.scope) {
                scope.configuration.metadata.scope = types.attributesScope.server.value;
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        };

        scope.$watch('configuration.originatorId', function (newVal, oldVal) {
            if (!angular.equals(newVal, oldVal)) {
                if (!scope.configuration.originatorId || scope.configuration.originatorId.entityType !== types.entityType.device) {
                    if (scope.configuration.metadata.scope !== types.attributesScope.server.value) {
                        scope.configuration.msgBody = {};
                        scope.configuration.metadata.scope = types.attributesScope.server.value;
                    }
                }
            }
        });

        scope.scopeChanged = function() {
            scope.configuration.msgBody = {};
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            readonly:'=ngReadonly'
        },
        link: linker
    };
}
