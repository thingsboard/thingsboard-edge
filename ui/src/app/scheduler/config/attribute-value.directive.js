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

import attributeValueTemplate from './attribute-value.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/*@ngInject*/
export default function AttributeValueDirective($compile, $templateCache, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(attributeValueTemplate);
        element.html(template);

        scope.valueTypes = types.valueType;

        scope.$watch('model.value', function (newValue, oldValue) {
            if (!angular.equals(newValue, oldValue)) {
                ngModelCtrl.$setViewValue(prepareValue(scope.model.value));
            }
        });

        function prepareValue(origValue) {
            var result = origValue;
            if (scope.valueType === types.valueType.string) {
                if (!origValue) {
                    result = '';
                }
            }
            return result;
        }

        scope.valueTypeChange = function() {
            if (scope.valueType === types.valueType.boolean) {
                scope.model.value = false;
            } else if (scope.valueType === types.valueType.string) {
                scope.model.value = '';
            } else {
                scope.model.value = null;
            }
        };

        ngModelCtrl.$render = function () {
            var attributeValue = ngModelCtrl.$viewValue;
            scope.model = {
                value: attributeValue
            };
            if (scope.model.value === true || scope.model.value === false) {
                scope.valueType = types.valueType.boolean;
            } else if (angular.isNumber(scope.model.value)) {
                if (scope.model.value.toString().indexOf('.') == -1) {
                    scope.valueType = types.valueType.integer;
                } else {
                    scope.valueType = types.valueType.double;
                }
            } else {
                scope.valueType = types.valueType.string;
            }
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            disabled: "=ngDisabled"
        },
        link: linker
    };
}
