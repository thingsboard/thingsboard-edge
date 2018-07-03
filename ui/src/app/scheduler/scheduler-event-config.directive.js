/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import schedulerEventConfigTemplate from './scheduler-event-config.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function SchedulerEventConfigDirective($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(schedulerEventConfigTemplate);
        element.html(template);

        scope.$watch('configuration', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
        };

        scope.$watch('configType', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                updateConfigTypeParams();
            }
        });

        updateConfigTypeParams();

        function updateConfigTypeParams() {
            var useDefinedTemplate = false;
            var showOriginator = true;
            var showMsgType = true;
            var showMetadata = true;
            if (scope.configType) {
                if (scope.configTypes[scope.configType]) {
                    var configTypeDef = scope.configTypes[scope.configType];
                    useDefinedTemplate = configTypeDef.template || configTypeDef.directive;
                    showOriginator = configTypeDef.originator;
                    showMsgType = configTypeDef.msgType;
                    showMetadata = configTypeDef.metadata;
                }
            }
            scope.useDefinedTemplate = useDefinedTemplate;
            scope.showOriginator = showOriginator;
            scope.showMsgType = showMsgType;
            scope.showMetadata = showMetadata;
        }

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            configType: '=',
            configTypes: '=',
            required:'=ngRequired',
            readonly:'=ngReadonly'
        },
        link: linker
    };

}
