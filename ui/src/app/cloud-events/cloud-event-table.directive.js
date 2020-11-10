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
import './cloud-event.scss';

/* eslint-disable import/no-unresolved, import/default */

import cloudEventTableTemplate from './cloud-event-table.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CloudEventTableDirective($compile, $templateCache, $rootScope, $filter, $translate, types, auditLogService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(cloudEventTableTemplate);

        element.html(template);

        scope.types = types;

        var pageSize = 20;
        var startTime = 0;
        var endTime = 0;

        scope.timewindow = {
            history: {
                timewindowMs: 24 * 60 * 60 * 1000 // 1 day
            }
        }

        scope.topIndex = 0;
        scope.searchText = '';

        scope.theCloudEvents = {
            getItemAtIndex: function (index) {
                if (index > scope.cloudEvents.filtered.length) {
                    scope.theCloudEvents.fetchMoreItems_(index);
                    return null;
                }
                return scope.cloudEvents.filtered[index];
            },

            getLength: function () {
                if (scope.cloudEvents.hasNext) {
                    return scope.cloudEvents.filtered.length + scope.cloudEvents.nextPageLink.limit;
                } else {
                    return scope.cloudEvents.filtered.length;
                }
            },

            fetchMoreItems_: function () {
                if (scope.cloudEvents.hasNext && !scope.cloudEvents.pending) {
                    var promise = getCloudEventsPromise(scope.cloudEvents.nextPageLink);
                    if (promise) {
                        scope.cloudEvents.pending = true;
                        promise.then(
                            function success(cloudEvents) {
                                scope.cloudEvents.data = scope.cloudEvents.data.concat(prepareCloudEventsData(cloudEvents.data));
                                scope.cloudEvents.filtered = $filter('filter')(scope.cloudEvents.data, {$: scope.searchText});
                                scope.cloudEvents.nextPageLink = cloudEvents.nextPageLink;
                                scope.cloudEvents.hasNext = cloudEvents.hasNext;
                                if (scope.cloudEvents.hasNext) {
                                    scope.cloudEvents.nextPageLink.limit = pageSize;
                                }
                                scope.cloudEvents.pending = false;
                            },
                            function fail() {
                                scope.cloudEvents.hasNext = false;
                                scope.cloudEvents.pending = false;
                            });
                    } else {
                        scope.cloudEvents.hasNext = false;
                    }
                }
            }
        };

        function prepareCloudEventsData(data) {
            data.forEach(
                cloudEvent => {
                    cloudEvent.entityTypeText = $translate.instant(types.entityTypeTranslations[cloudEvent.entityId.entityType].type);
                    cloudEvent.actionTypeText = $translate.instant(types.cloudEventActionType[cloudEvent.actionType].name);
                    cloudEvent.actionStatusText = $translate.instant(types.cloudEventActionStatus[cloudEvent.actionStatus].name);
                    cloudEvent.actionDataText = cloudEvent.actionData ? angular.toJson(cloudEvent.actionData, true) : '';
                }
            );
            return data;
        }

        scope.$watch("entityId", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                resetFilter();
                scope.reload();
            }
        });

        scope.$watch("userId", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                resetFilter();
                scope.reload();
            }
        });

        scope.$watch("customerId", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                resetFilter();
                scope.reload();
            }
        });

        function getCloudEventsPromise(pageLink) {
            switch(scope.cloudEventMode) {
                case types.cloudEventMode.tenant:
                    return auditLogService.getAuditLogs(pageLink);
                case types.cloudEventMode.entity:
                    if (scope.entityType && scope.entityId) {
                        return auditLogService.getAuditLogsByEntityId(scope.entityType, scope.entityId,
                            pageLink);
                    } else {
                        return null;
                    }
                case types.cloudEventMode.user:
                    if (scope.userId) {
                        return auditLogService.getAuditLogsByUserId(scope.userId, pageLink);
                    } else {
                        return null;
                    }
                case types.cloudEventMode.customer:
                    if (scope.customerId) {
                        return auditLogService.getAuditLogsByCustomerId(scope.customerId, pageLink);
                    } else {
                        return null;
                    }
            }
        }

        function destroyWatchers() {
            if (scope.timewindowWatchHandle) {
                scope.timewindowWatchHandle();
                scope.timewindowWatchHandle = null;
            }
            if (scope.searchTextWatchHandle) {
                scope.searchTextWatchHandle();
                scope.searchTextWatchHandle = null;
            }
        }

        function initWatchers() {
            scope.timewindowWatchHandle = scope.$watch("timewindow", function(newVal, prevVal) {
                if (newVal && !angular.equals(newVal, prevVal)) {
                    scope.reload();
                }
            }, true);

            scope.searchTextWatchHandle = scope.$watch("searchText", function(newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.searchTextUpdated();
                }
            }, true);
        }

        function resetFilter() {
            destroyWatchers();
            scope.timewindow = {
                history: {
                    timewindowMs: 24 * 60 * 60 * 1000 // 1 day
                }
            };
            scope.searchText = '';
            initWatchers();
        }

        function updateTimeWindowRange () {
            if (scope.timewindow.history.timewindowMs) {
                var currentTime = (new Date).getTime();
                startTime = currentTime - scope.timewindow.history.timewindowMs;
                endTime = currentTime;
            } else {
                startTime = scope.timewindow.history.fixedTimewindow.startTimeMs;
                endTime = scope.timewindow.history.fixedTimewindow.endTimeMs;
            }
        }

        scope.reload = function() {
            scope.topIndex = 0;
            updateTimeWindowRange();
            scope.cloudEvents = {
                data: [],
                filtered: [],
                nextPageLink: {
                    limit: pageSize,
                    startTime: startTime,
                    endTime: endTime
                },
                hasNext: true,
                pending: false
            };
            scope.theCloudEvents.getItemAtIndex(pageSize);
        }

        scope.searchTextUpdated = function() {
            scope.cloudEvents.filtered = $filter('filter')(scope.cloudEvents.data, {$: scope.searchText});
            scope.theCloudEvents.getItemAtIndex(pageSize);
        }

        scope.noData = function() {
            return scope.cloudEvents.data.length == 0 && !scope.cloudEvents.hasNext;
        }

        scope.hasData = function() {
            return scope.cloudEvents.data.length > 0;
        }

        scope.loading = function() {
            return $rootScope.loading;
        }

        scope.hasScroll = function() {
            var repeatContainer = scope.repeatContainer[0];
            if (repeatContainer) {
                var scrollElement = repeatContainer.children[0];
                if (scrollElement) {
                    return scrollElement.scrollHeight > scrollElement.clientHeight;
                }
            }
            return false;
        }

        scope.reload();

        initWatchers();

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            entityType: '=?',
            entityId: '=?',
            userId: '=?',
            customerId: '=?',
            cloudEventMode: '@',
            pageMode: '@?'
        }
    };
}
