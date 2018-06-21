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

const SNAKE_CASE_REGEXP = /[A-Z]/g;

/*@ngInject*/
export default function SchedulerEventTemplateConfigDirective($compile, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        scope.types = types;

        attrs.$observe('configType', function() {
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
            if (scope.eventConfigScope) {
                scope.eventConfigScope.$destroy();
            }
            var template = '<div>Not defined!</div>';
            if (scope.configTypes[attrs.configType]) {
                if (scope.configTypes[attrs.configType].directive) {
                    var directive = snake_case(scope.configTypes[attrs.configType].directive, '-');
                    template = `<${directive} ng-model="configuration" ng-required="required" ng-readonly="readonly"></${directive}>`;
                } else if (scope.configTypes[attrs.configType].template) {
                    template = scope.configTypes[attrs.configType].template;
                }
            }
            element.html(template);
            scope.eventConfigScope = scope.$new();
            $compile(element.contents())(scope.eventConfigScope);
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
            configTypes:'=',
            required:'=ngRequired',
            readonly:'=ngReadonly'
        },
        link: linker
    };
}
