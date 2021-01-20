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
                                          types, utils, toast, entityService, ruleChainService) {

    var linker = function (scope, element, attrs) {

        var template = edgeDownlinlsRowTemplate;

        element.html($templateCache.get(template));
        $compile(element.contents())(scope);

        scope.types = types;
        scope.downlink = attrs.downlink;

        scope.showEdgeEntityContent = function($event, title, contentType) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            if (!contentType) {
                contentType = null;
            }
            var content = '';
            switch(scope.downlink.type) {
                case types.edgeEventType.relation:
                    content = angular.toJson(scope.downlink.body);
                    showDialog();
                    break;
                case types.edgeEventType.ruleChainMetaData:
                    content = ruleChainService.getRuleChainMetaData(scope.downlink.entityId, {ignoreErrors: true}).then(
                        function success(info) {
                            showDialog();
                            return angular.toJson(info);
                        }, function fail() {
                            showError();
                        });
                    break;
                default:
                    content = entityService.getEntity(scope.downlink.type, scope.downlink.entityId, {ignoreErrors: true}).then(
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
                    controller: 'EdgeDownlinksContentDialogController',
                    controllerAs: 'vm',
                    templateUrl: edgeDownlinksContentTemplate,
                    locals: {content: content, title: title, contentType: contentType, showingCallback: onShowingCallback},
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

        scope.checkEdgeDownlinksType = function (type) {
            return !(type === types.edgeEventType.widgetType ||
                type === types.edgeEventType.adminSettings ||
                type === types.edgeEventType.widgetsBundle );
        }

        scope.checkTooltip = function($event) {
            var el = $event.target;
            var $el = angular.element(el);
            if(el.offsetWidth < el.scrollWidth && !$el.attr('title')){
                $el.attr('title', $el.text());
            }
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
