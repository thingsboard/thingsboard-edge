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
import './audit-log.scss';

/* eslint-disable import/no-unresolved, import/default */

import auditLogTableTemplate from './audit-log-table.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AuditLogTableDirective($compile, $templateCache, $rootScope, $filter, $translate, types, auditLogService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(auditLogTableTemplate);

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

        scope.theAuditLogs = {
            getItemAtIndex: function (index) {
                if (index > scope.auditLogs.filtered.length) {
                    scope.theAuditLogs.fetchMoreItems_(index);
                    return null;
                }
                return scope.auditLogs.filtered[index];
            },

            getLength: function () {
                if (scope.auditLogs.hasNext) {
                    return scope.auditLogs.filtered.length + scope.auditLogs.nextPageLink.limit;
                } else {
                    return scope.auditLogs.filtered.length;
                }
            },

            fetchMoreItems_: function () {
                if (scope.auditLogs.hasNext && !scope.auditLogs.pending) {
                    var promise = getAuditLogsPromise(scope.auditLogs.nextPageLink);
                    if (promise) {
                        scope.auditLogs.pending = true;
                        promise.then(
                            function success(auditLogs) {
                                scope.auditLogs.data = scope.auditLogs.data.concat(prepareAuditLogsData(auditLogs.data));
                                scope.auditLogs.filtered = $filter('filter')(scope.auditLogs.data, {$: scope.searchText});
                                scope.auditLogs.nextPageLink = auditLogs.nextPageLink;
                                scope.auditLogs.hasNext = auditLogs.hasNext;
                                if (scope.auditLogs.hasNext) {
                                    scope.auditLogs.nextPageLink.limit = pageSize;
                                }
                                scope.auditLogs.pending = false;
                            },
                            function fail() {
                                scope.auditLogs.hasNext = false;
                                scope.auditLogs.pending = false;
                            });
                    } else {
                        scope.auditLogs.hasNext = false;
                    }
                }
            }
        };

        function prepareAuditLogsData(data) {
            data.forEach(
                auditLog => {
                    auditLog.entityTypeText = $translate.instant(types.entityTypeTranslations[auditLog.entityId.entityType].type);
                    auditLog.actionTypeText = $translate.instant(types.auditLogActionType[auditLog.actionType].name);
                    auditLog.actionStatusText = $translate.instant(types.auditLogActionStatus[auditLog.actionStatus].name);
                    auditLog.actionDataText = auditLog.actionData ? angular.toJson(auditLog.actionData, true) : '';
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

        function getAuditLogsPromise(pageLink) {
            switch(scope.auditLogMode) {
                case types.auditLogMode.tenant:
                    return auditLogService.getAuditLogs(pageLink);
                case types.auditLogMode.entity:
                    if (scope.entityType && scope.entityId) {
                        return auditLogService.getAuditLogsByEntityId(scope.entityType, scope.entityId,
                            pageLink);
                    } else {
                        return null;
                    }
                case types.auditLogMode.user:
                    if (scope.userId) {
                        return auditLogService.getAuditLogsByUserId(scope.userId, pageLink);
                    } else {
                        return null;
                    }
                case types.auditLogMode.customer:
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
            scope.auditLogs = {
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
            scope.theAuditLogs.getItemAtIndex(pageSize);
        }

        scope.searchTextUpdated = function() {
            scope.auditLogs.filtered = $filter('filter')(scope.auditLogs.data, {$: scope.searchText});
            scope.theAuditLogs.getItemAtIndex(pageSize);
        }

        scope.noData = function() {
            return scope.auditLogs.data.length == 0 && !scope.auditLogs.hasNext;
        }

        scope.hasData = function() {
            return scope.auditLogs.data.length > 0;
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
            auditLogMode: '@',
            pageMode: '@?'
        }
    };
}
