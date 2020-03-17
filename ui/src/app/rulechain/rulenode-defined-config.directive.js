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
const SNAKE_CASE_REGEXP = /[A-Z]/g;

/*@ngInject*/
export default function RuleNodeDefinedConfigDirective($compile) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        attrs.$observe('ruleNodeDirective', function() {
            loadTemplate();
        });

        scope.$watch('configuration', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                ngModelCtrl.$setViewValue(scope.configuration);
            }
        });

        ngModelCtrl.$render = function () {
            scope.configuration = ngModelCtrl.$viewValue;
        };

        function loadTemplate() {
            if (scope.ruleNodeConfigScope) {
                scope.ruleNodeConfigScope.$destroy();
            }
            var directive = snake_case(attrs.ruleNodeDirective, '-');
            var template = `<${directive} rule-node-id="ruleNodeId" ng-model="configuration" ng-required="required" ng-readonly="readonly"></${directive}>`;
            element.html(template);
            scope.ruleNodeConfigScope = scope.$new();
            $compile(element.contents())(scope.ruleNodeConfigScope);
        }

        function snake_case(name, separator) {
            separator = separator || '_';
            return name.replace(SNAKE_CASE_REGEXP, function(letter, pos) {
                return (pos ? separator : '') + letter.toLowerCase();
            });
        }
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            ruleNodeId:'=',
            required:'=ngRequired',
            readonly:'=ngReadonly'
        },
        link: linker
    };

}
