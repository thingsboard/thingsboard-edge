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

import edgeDownlinksContentTemplate from './edge-downlinks-content-dialog.tpl.html';
import edgeDownlinlsRowTemplate from './edge-downlinks-row.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeDownlinksRowDirective($compile, $templateCache, $mdDialog, $document, $translate,
                                          types, utils, toast, entityService) {

    var linker = function (scope, element, attrs) {

        var template = edgeDownlinlsRowTemplate;

        element.html($templateCache.get(template));
        $compile(element.contents())(scope);

        scope.types = types;
        scope.downlink = attrs.downlink;

        scope.showEdgeEntityContent = function($event) {
            var onShowingCallback = {
                onShowing: function () {
                }
            }
            var content = entityService.getEdgeEventContentByEntityType(scope.downlink).then(
                function success(content) {
                    showDialog();
                    return angular.toJson(content);
                }, function fail() {
                    showError();
                });
            function showDialog() {
                $mdDialog.show({
                    controller: 'EdgeDownlinksContentDialogController',
                    controllerAs: 'vm',
                    templateUrl: edgeDownlinksContentTemplate,
                    locals: {
                        content: content,
                        title: $translate.instant('edge.entity-info'),
                        contentType: types.contentType.JSON.value,
                        showingCallback: onShowingCallback
                    },
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event,
                    multiple: true,
                    onShowing: function(scope, element) {
                        onShowingCallback.onShowing(scope, element);
                    }
                });
            }
            function showError() {
                toast.showError($translate.instant('edge.load-entity-error'));
            }
        }

        scope.isEdgeEventHasData = function(type) {
            return !(
                type === types.edgeEventType.role ||
                type === types.edgeEventType.adminSettings ||
                type === types.edgeEventType.customTranslation ||
                type === types.edgeEventType.whiteLabeling ||
                type === types.edgeEventType.loginWhiteLabeling ||
                type === types.edgeEventType.deviceProfile ||
                type === types.edgeEventType.groupPermission
            );
        }

        $compile(element.contents())(scope);

        scope.updateStatus = function(downlinkCreatedTime) {
            var status;
            if (downlinkCreatedTime < scope.queueStartTs) {
                status = $translate.instant(types.edgeEventStatus.DEPLOYED.name);
                scope.statusColor = types.edgeEventStatus.DEPLOYED.color;
            } else {
                status = $translate.instant(types.edgeEventStatus.PENDING.name);
                scope.statusColor = types.edgeEventStatus.PENDING.color;
            }
            return status;
        }
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
