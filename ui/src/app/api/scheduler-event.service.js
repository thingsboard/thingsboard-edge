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
export default angular.module('thingsboard.api.schedulerEvent', [])
    .factory('schedulerEventService', SchedulerEventService)
    .name;

/*@ngInject*/
function SchedulerEventService($http, $q, customerService) {

    var service = {
        getSchedulerEvents: getSchedulerEvents,
        getSchedulerEventsByIds: getSchedulerEventsByIds,
        getSchedulerEvent: getSchedulerEvent,
        getSchedulerEventInfo: getSchedulerEventInfo,
        saveSchedulerEvent: saveSchedulerEvent,
        deleteSchedulerEvent: deleteSchedulerEvent
    };

    return service;

    function getSchedulerEvents(type, applyCustomersInfo, config) {
        var deferred = $q.defer();
        var url = '/api/schedulerEvents';
        if (type) {
            url += '?type=' + type;
        }
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomersInfo(response.data).then(
                    function success(data) {
                        deferred.resolve(data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSchedulerEventsByIds(schedulerEventIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<schedulerEventIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += schedulerEventIds[i];
        }
        var url = '/api/schedulerEvents?schedulerEventIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var entities = response.data;
            entities.sort(function (entity1, entity2) {
                var id1 =  entity1.id.id;
                var id2 =  entity2.id.id;
                var index1 = schedulerEventIds.indexOf(id1);
                var index2 = schedulerEventIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(entities);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSchedulerEventInfo(schedulerEventId, config) {
        var deferred = $q.defer();
        var url = '/api/schedulerEvent/info/' + schedulerEventId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSchedulerEvent(schedulerEventId, config) {
        var deferred = $q.defer();
        var url = '/api/schedulerEvent/' + schedulerEventId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveSchedulerEvent(schedulerEvent) {
        var deferred = $q.defer();
        var url = '/api/schedulerEvent';
        $http.post(url, schedulerEvent).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteSchedulerEvent(schedulerEventId) {
        var deferred = $q.defer();
        var url = '/api/schedulerEvent/' + schedulerEventId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }
}
