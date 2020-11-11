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

import cloudEventDetailsDialogTemplate from './cloud-event-details-dialog.tpl.html';
import cloudEventRowTemplate from './cloud-event-row.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CloudEventRowDirective($compile, $templateCache, types, $mdDialog, $document, utils, $translate,
                                               ruleChainService, entityService, toast) {

    var linker = function (scope, element, attrs) {

        var template = $templateCache.get(cloudEventRowTemplate);
        element.html(template);

        scope.cloudEvent = attrs.cloudEvent;
        scope.types = types;
        scope.utils = utils;

        scope.updateStatus = function(eventCreatedTime) {
            var status;
            if (eventCreatedTime < scope.queueStartTs) {
                status = $translate.instant('edge.success');
                scope.statusColor = '#000';
            } else {
                status = $translate.instant('edge.failed');
                scope.statusColor = 'rgba(0, 0, 0, .38)';
            }
            return status;
        }

        scope.checkCloudEventType = function (cloudEventType) {
            return !(cloudEventType === types.cloudEventType.widgetType ||
                cloudEventType === types.cloudEventType.adminSettings ||
                cloudEventType === types.cloudEventType.widgetsBundle );
        }

        scope.showCloudEventDetails = function($event) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            var content = '';

            switch(scope.cloudEvent.cloudEventType) {
                case types.cloudEventType.relation:
                    content = angular.toJson(scope.cloudEvent.body);
                    break;
                case types.cloudEventType.ruleChainMetaData:
                    content = ruleChainService.getRuleChainMetaData(scope.cloudEvent.entityId, {ignoreErrors: true}).then(
                        function success(info) {
                            showDialog();
                            return angular.toJson(info);
                        }, function fail() {
                            showError();
                        });
                    break;
                default:
                    content = entityService.getEntity(scope.cloudEvent.cloudEventType.toUpperCase(), scope.cloudEvent.entityId, {ignoreErrors: true}).then(
                        function success(info) {
                            showDialog();
                            return angular.toJson(info);
                        }, function fail() {
                            showError();
                        });
                    break;
            }

            function showDialog() {
                $mdDialog.show({
                    controller: 'CloudEventDetailsDialogController',
                    controllerAs: 'vm',
                    templateUrl: cloudEventDetailsDialogTemplate,
                    locals: {
                        content: content,
                        contentType: 'JSON',
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

        $compile(element.contents())(scope);
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
