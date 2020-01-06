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
import './entity-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySelectTemplate from './entity-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySelect($compile, $templateCache, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;

        var entityTypes = entityService.prepareAllowedEntityTypesList(scope.allowedEntityTypes, scope.useAliasEntityTypes, scope.operation);

        var entityTypeKeys = Object.keys(entityTypes);

        if (entityTypeKeys.length === 1) {
            scope.displayEntityTypeSelect = false;
            scope.defaultEntityType = entityTypes[entityTypeKeys[0]];
        } else {
            scope.displayEntityTypeSelect = true;
        }

        scope.model = {
            entityType: scope.defaultEntityType
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = ngModelCtrl.$viewValue;
                if (scope.model && scope.model.entityType && scope.model.entityId) {
                    if (!value) {
                        value = {};
                    }
                    value.entityType = scope.model.entityType;
                    value.id = scope.model.entityId;
                    ngModelCtrl.$setViewValue(value);
                } else {
                    ngModelCtrl.$setViewValue(null);
                }
            }
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            if (ngModelCtrl.$viewValue && ngModelCtrl.$viewValue.entityType) {
                var value = ngModelCtrl.$viewValue;
                scope.model.entityType = value.entityType;
                scope.model.entityId = value.id;
            } else {
                scope.model.entityType = scope.defaultEntityType;
                scope.model.entityId = null;
            }
            initWatchers();
        }

        function initWatchers() {
            scope.entityTypeDeregistration = scope.$watch('model.entityType', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });

            scope.entityIdDeregistration = scope.$watch('model.entityId', function (newVal, prevVal) {
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
            if (scope.entityTypeDeregistration) {
                scope.entityTypeDeregistration();
                scope.entityTypeDeregistration = null;
            }
            if (scope.entityIdDeregistration) {
                scope.entityIdDeregistration();
                scope.entityIdDeregistration = null;
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
            allowedEntityTypes: "=?",
            useAliasEntityTypes: "=?",
            operation: "=?"
        }
    };
}
