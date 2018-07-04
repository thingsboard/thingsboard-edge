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

import entityGroupSelectTemplate from './entity-group-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupSelectDirective($compile, $templateCache, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityGroupSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;


        if (scope.allowedGroupTypes && scope.allowedGroupTypes.length) {
            scope.entityGroupTypes = scope.allowedGroupTypes;
        } else {
            scope.entityGroupTypes = [types.entityType.device, types.entityType.asset, types.entityType.customer];
        }
        if (scope.entityGroupTypes.length === 1) {
            scope.displayGroupTypeSelect = false;
            scope.defaultGroupType = scope.entityGroupTypes[0];
        } else {
            scope.displayGroupTypeSelect = true;
        }

        scope.model = {
            groupType: scope.defaultGroupType
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = ngModelCtrl.$viewValue;
                if (scope.model && scope.model.groupId) {
                    if (!value) {
                        value = {};
                    }
                    value.entityType = types.entityType.entityGroup;
                    value.id = scope.model.groupId;
                    ngModelCtrl.$setViewValue(value);
                } else {
                    ngModelCtrl.$setViewValue(null);
                }
            }
        };

        ngModelCtrl.$render = function () {
            destroyWatchers();
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.model.groupId = value.id;
            } else {
                scope.model.groupId = null;
            }
            initWatchers();
        };

        scope.entityGroupLoaded = function(entityGroup) {
            if (entityGroup) {
                scope.model.groupType = entityGroup.type;
            }
        };

        function initWatchers() {
            scope.groupIdDeregistration = scope.$watch('model.groupId', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
            scope.disabledDeregistration = scope.$watch('disabled', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
        }

        function destroyWatchers() {
            if (scope.groupIdDeregistration) {
                scope.groupIdDeregistration();
                scope.groupIdDeregistration = null;
            }
            if (scope.disabledDeregistration) {
                scope.disabledDeregistration();
                scope.disabledDeregistration = null;
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            allowedGroupTypes: '=?',
            defaultGroupType: '=?',
            excludeGroupIds: '=?',
            excludeGroupAll: '=?',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@'
        }
    };
}
