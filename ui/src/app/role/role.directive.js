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

import './role.scss';

/* eslint-disable import/no-unresolved, import/default */

import roleFieldsetTemplate from './role-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RoleDirective($q, $compile, $templateCache, $filter, toast, $translate, $mdConstant, $mdExpansionPanel, types, securityTypes) {
    var linker = function (scope, element) {
        var template = $templateCache.get(roleFieldsetTemplate);
        element.html(template);

        scope.permissionsPanelId = (Math.random()*1000).toFixed(0);
        scope.$mdExpansionPanel = $mdExpansionPanel;
        scope.types = types;
        scope.securityTypes = securityTypes;

        var semicolon = 186;
        scope.separatorKeys = [$mdConstant.KEY_CODE.ENTER, $mdConstant.KEY_CODE.COMMA, semicolon];

        scope.$watch('role', function(newVal) {
            if (newVal) {
                if (!scope.role.permissions) {
                    scope.role.permissions = {};
                }
            }
        });

        scope.$watch('role.type', function(newVal, prevVal) {
            if (scope.isEdit && !angular.equals(newVal, prevVal)) {
                if (scope.role.type === scope.securityTypes.roleType.generic) {
                    scope.role.permissions = {};
                } else if (scope.role.type === scope.securityTypes.roleType.group) {
                    scope.role.permissions = [];
                }
            }
        });

        scope.onRoleIdCopied = function() {
            toast.showSuccess($translate.instant('role.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            role: '=',
            isEdit: '=',
            readonly: '=?tbReadonly',
            theForm: '=',
            onDeleteRole: '&'
        }
    };
}
