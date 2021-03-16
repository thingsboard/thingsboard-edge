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
/* eslint-disable import/no-unresolved, import/default */

import edgeFieldsetTemplate from './edge-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeDirective($compile, $templateCache, $translate, $mdDialog, $document, utils, toast, types, edgeService, userPermissionsService, securityTypes) {
    var linker = function (scope, element) {
        var template = $templateCache.get(edgeFieldsetTemplate);
        element.html(template);

        scope.types = types;
        /*scope.isAssignedToCustomer = false;
        scope.isPublic = false;
        scope.assignedCustomer = null;*/
        scope.isTenantAdmin = userPermissionsService.hasGenericPermission(securityTypes.resource.edge, securityTypes.operation.write); //TODO deaflynx: move this to linker

        scope.$watch('edge', function(newVal) {
            if (newVal) {
                if (scope.edge.id && !scope.edge.id.id) {
                    scope.edge.routingKey = utils.guid('');
                    scope.edge.secret = generateSecret(20);
                    scope.edge.cloudEndpoint = utils.baseUrl();
                    scope.edge.type = 'default';
                }
                /*if (scope.edge.customerId && scope.edge.customerId.id !== types.id.nullUid) {
                    scope.isAssignedToCustomer = true;
                    customerService.getShortCustomerInfo(scope.edge.customerId.id).then(
                        function success(customer) {
                            scope.assignedCustomer = customer;
                            scope.isPublic = customer.isPublic;
                        }
                    );
                } else {
                    scope.isAssignedToCustomer = false;
                    scope.isPublic = false;
                    scope.assignedCustomer = null;
                }*/
            }
        });

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

        scope.onEdgeIdCopied = function() {
            toast.showSuccess($translate.instant('edge.id-copied-message'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.onEdgeSync = function (edgeId) {
            edgeService.syncEdge(edgeId.id).then(
                function success() {
                    toast.showSuccess($translate.instant('edge.sync-message'), 750, angular.element(element).parent().parent(), 'bottom left');
                },
                function fail(error) {
                    toast.showError(error);
                }
            );
        }

        $compile(element.contents())(scope);

        scope.onEdgeInfoCopied = function(type) {
            let infoTypeLabel = "";
            switch (type) {
                case 'key':
                    infoTypeLabel = "edge.edge-key-copied-message";
                    break;
                case 'secret':
                    infoTypeLabel = "edge.edge-secret-copied-message";
                    break;
            }
            toast.showSuccess($translate.instant(infoTypeLabel), 750, angular.element(element).parent().parent(), 'bottom left');
        };

    };
    return {
        restrict: "E",
        link: linker,
        scope: {
            entityGroup: '=',
            edge: '=',
            isEdit: '=',
            edgeScope: '=',
            theForm: '=',
            onManageUsers: '&',
            onManageAssets: '&',
            onManageDevices: '&',
            onManageEntityViews: '&',
            onManageDashboards: '&',
            onManageRuleChains: '&',
            onManageSchedulerEvents: '&',
            onMakePublic: '&',
            onUnassignFromCustomer: '&',
            onDeleteEdge: '&',
            hideAssignmentActions: '=',
            hideDelete: '=',
            hideManageUsers: '=',
            hideManageAssets: '=',
            hideManageDevices: '=',
            hideManageEntityViews: '=',
            hideManageDashboards: '=',
            hideManageSchedulerEvents: '=',
            hideManageRuleChains: '='
        }
    };
}
