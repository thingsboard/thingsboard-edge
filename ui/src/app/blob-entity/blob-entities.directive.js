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
import './blob-entities.scss';

/* eslint-disable import/no-unresolved, import/default */

import blobEntitiesTemplate from './blob-entities.tpl.html';
import blobEntitiesTitleTemplate from './blob-entities-title.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function BlobEntitiesDirective($compile, $templateCache, $rootScope, $filter, $translate, types, securityTypes, userPermissionsService,
                                              userService, blobEntityService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(blobEntitiesTemplate);

        element.html(template);

        scope.showData = (userService.getAuthority() === 'TENANT_ADMIN' || userService.getAuthority() === 'CUSTOMER_USER') &&
            userPermissionsService.hasGenericPermission(securityTypes.resource.blobEntity, securityTypes.operation.read);

        scope.displayCreatedTime = true;
        scope.displayType = true;
        scope.displayCustomer = true;//userService.getAuthority() === 'TENANT_ADMIN' ? true : false;

        scope.defaultType = null;

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

        scope.theBlobEntities = {
            getItemAtIndex: function (index) {
                if (index > scope.blobEntities.filtered.length) {
                    scope.theBlobEntities.fetchMoreItems_(index);
                    return null;
                }
                return scope.blobEntities.filtered[index];
            },

            getLength: function () {
                if (scope.blobEntities.hasNext) {
                    return scope.blobEntities.filtered.length + scope.blobEntities.nextPageLink.limit;
                } else {
                    return scope.blobEntities.filtered.length;
                }
            },

            fetchMoreItems_: function () {
                if (scope.blobEntities.hasNext && !scope.blobEntities.pending) {
                    var promise = getBlobEntitiesPromise(scope.blobEntities.nextPageLink);
                    if (promise) {
                        scope.blobEntities.pending = true;
                        promise.then(
                            function success(blobEntities) {
                                scope.blobEntities.data = scope.blobEntities.data.concat(prepareBlobEntitiesData(blobEntities.data));
                                scope.blobEntities.filtered = $filter('filter')(scope.blobEntities.data, {$: scope.searchText});
                                scope.blobEntities.nextPageLink = blobEntities.nextPageLink;
                                scope.blobEntities.hasNext = blobEntities.hasNext;
                                if (scope.blobEntities.hasNext) {
                                    scope.blobEntities.nextPageLink.limit = pageSize;
                                }
                                scope.blobEntities.pending = false;
                            },
                            function fail() {
                                scope.blobEntities.hasNext = false;
                                scope.blobEntities.pending = false;
                            });
                    } else {
                        scope.blobEntities.hasNext = false;
                    }
                }
            }
        };

        function prepareBlobEntitiesData(data) {
            data.forEach(
                blobEntity => {
                    blobEntity.typeText = blobEntity.type;
                    if (types.blobEntityType[blobEntity.type]) {
                        blobEntity.typeText = $translate.instant(types.blobEntityType[blobEntity.type].name);
                    }
                }
            );
            return data;
        }

        function getBlobEntitiesPromise(pageLink) {
            if (scope.showData && (!scope.widgetMode || (scope.widgetMode && scope.ctx))) {
                return blobEntityService.getBlobEntities(pageLink, scope.defaultType, scope.displayCustomer);
            } else {
                return null;
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

        function resetBlobEntities() {
            scope.blobEntities = {
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
        }

        scope.reload = function() {
            scope.topIndex = 0;
            updateTimeWindowRange();
            resetBlobEntities();
            scope.theBlobEntities.getItemAtIndex(pageSize);
        };

        scope.searchTextUpdated = function() {
            scope.blobEntities.filtered = $filter('filter')(scope.blobEntities.data, {$: scope.searchText});
            scope.theBlobEntities.getItemAtIndex(pageSize);
        };

        scope.noData = function() {
            return scope.blobEntities.data.length == 0 && !scope.blobEntities.hasNext;
        };

        scope.hasData = function() {
            return scope.blobEntities.data.length > 0;
        };

        scope.loading = function() {
            return $rootScope.loading;
        };

        scope.hasScroll = function() {
            var repeatContainer = scope.repeatContainer[0];
            if (repeatContainer) {
                var scrollElement = repeatContainer.children[0];
                if (scrollElement) {
                    return scrollElement.scrollHeight > scrollElement.clientHeight;
                }
            }
            return false;
        };

        resetBlobEntities();

        if (scope.showData) {
            if (scope.widgetMode) {
                scope.$watch('ctx', function() {
                    if (scope.ctx) {
                        scope.settings = scope.ctx.settings;
                        initializeWidgetConfig();
                        scope.reload();
                        initWatchers();
                    }
                });
            } else {
                scope.reload();
                initWatchers();
            }
        }

        function initializeWidgetConfig() {
            scope.ctx.widgetConfig.showTitle = false;
            scope.ctx.widgetTitleTemplate = blobEntitiesTitleTemplate;
            scope.ctx.currentTimewindow = scope.timewindow;
            scope.ctx.currentSearchText = '';
            scope.ctx.widgetTitle = scope.ctx.settings.title;

            scope.displayCreatedTime = angular.isDefined(scope.settings.displayCreatedTime) ? scope.settings.displayCreatedTime : true;
            scope.displayType = angular.isDefined(scope.settings.displayType) ? scope.settings.displayType : true;
            //if (userService.getAuthority() === 'TENANT_ADMIN') {
                scope.displayCustomer = angular.isDefined(scope.settings.displayCustomer) ? scope.settings.displayCustomer : true;
            //}

            if (scope.settings.forceDefaultType && scope.settings.forceDefaultType.length) {
                scope.defaultType = scope.settings.forceDefaultType;
            }

            scope.$watch('ctx.currentSearchText', function() {
                scope.searchText = scope.ctx.currentSearchText;
            });

            scope.$watch('ctx.currentTimewindow', function() {
                scope.timewindow = scope.ctx.currentTimewindow;
            });

            scope.ctx.widgetActions = [
                {
                    name: 'action.refresh',
                    show: true,
                    onAction: function() {
                        scope.reload();
                    },
                    icon: 'refresh'
                }
            ];
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            widgetMode: '=',
            ctx: '='
        }
    };
}
