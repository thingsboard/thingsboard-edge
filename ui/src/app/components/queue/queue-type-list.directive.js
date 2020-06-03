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
import './queue-type-list.scss';

/* eslint-disable import/no-unresolved, import/default */

import queueTypeListTemplate from './queue-type-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function QueueTypeList($compile, $templateCache, $q, $filter, queueService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(queueTypeListTemplate);
        element.html(template);

        scope.queues = null;
        scope.queue = null;
        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.queueSearchText = '';

        scope.fetchQueues = function(searchText) {
            var deferred = $q.defer();
            loadQueues().then(
                function success(queueArr) {
                    let result = $filter('filter')(queueArr, {'$': searchText});
                    if (result && result.length) {
                        if (searchText && searchText.length && result.indexOf(searchText) === -1) {
                            result.push(searchText);
                        }
                        result.sort();
                        deferred.resolve(result);
                    } else {
                        deferred.resolve([searchText]);
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );

            return deferred.promise;
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.queue);
            }
        };

        function loadQueues() {
            var deferred = $q.defer();
            if (!scope.queues) {
                queueService.getTenantQueuesByServiceType(scope.queueType).then(
                function success(queueArr) {
                    scope.queues = queueArr.data;
                    deferred.resolve(scope.queues);
                },
                function fail() {
                    deferred.reject();
                }
                );
            } else {
                deferred.resolve(scope.queues);
            }
            return deferred.promise;
        }

        ngModelCtrl.$render = function () {
            scope.queue = ngModelCtrl.$viewValue;
        };

        scope.$watch('queue', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            queueType: '=?'
        }
    };
}