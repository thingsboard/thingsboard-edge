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
export default angular.module('thingsboard.api.schedulerEvent', [])
    .factory('schedulerEventService', SchedulerEventService)
    .name;

/*@ngInject*/
function SchedulerEventService($http, $q, customerService, utils) {

    var service = {
        getSchedulerEvents: getSchedulerEvents,
        getSchedulerEventsByIds: getSchedulerEventsByIds,
        getSchedulerEvent: getSchedulerEvent,
        getSchedulerEventInfo: getSchedulerEventInfo,
        saveSchedulerEvent: saveSchedulerEvent,
        deleteSchedulerEvent: deleteSchedulerEvent,
        updateSchedulerEdgeGroups: updateSchedulerEdgeGroups,
        getEdgeSchedulerEvents: getEdgeSchedulerEvents
    };

    var resolvedData = [
            {
                "id": {
                    "entityType": "SCHEDULER_EVENT",
                    "id": "23fd8300-9368-11ea-9f7a-5d9b82eb7ee0"
                },
                "createdTime": 1589188607792,
                "additionalInfo": null,
                "assignedEdgeGroups": null,
                "tenantId": {
                    "entityType": "TENANT",
                    "id": "ab263280-928e-11ea-bf40-57130a664206"
                },
                "customerId": {
                    "entityType": "CUSTOMER",
                    "id": "13814000-1dd2-11b2-8080-808080808080"
                },
                "name": "SCHEDULER 1",
                "type": "generateReport",
                "schedule": {
                    "timezone": "Europe/Kiev",
                    "startTime": 1589144400000
                },
                "ownerId": {
                    "entityType": "TENANT",
                    "id": "ad0c2200-952a-11ea-a4d0-b55ea5c970d7"
                }
            },
            {
                "id": {
                    "entityType": "SCHEDULER_EVENT",
                    "id": "002e2c10-9389-11ea-9f7a-5d9b82eb7ee0"
                },
                "createdTime": 1589202721105,
                "additionalInfo": null,
                "assignedEdgeGroups": null,
                "tenantId": {
                    "entityType": "TENANT",
                    "id": "ab263280-928e-11ea-bf40-57130a664206"
                },
                "customerId": {
                    "entityType": "CUSTOMER",
                    "id": "13814000-1dd2-11b2-8080-808080808080"
                },
                "name": "SCHEDULER 2",
                "type": "updateAttributes",
                "schedule": {
                    "timezone": "Europe/Kiev",
                    "repeat": {
                        "type": "WEEKLY",
                        "endsOn": 1589576400000,
                        "repeatOn": [
                            1,
                            2,
                            4
                        ]
                    },
                    "startTime": 1589144400000
                },
                "ownerId": {
                    "entityType": "TENANT",
                    "id": "ad0c2200-952a-11ea-a4d0-b55ea5c970d7"
                }
            }
        ]

    return service;

    function getEdgeSchedulerEvents() {
        var deferred = $q.defer();
        $http.get('getSchedulerEvents').then(function success() {
            deferred.resolve(utils.prepareAssignedEdgeGroups(angular.copy(resolvedData), 'Scheduler'));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function updateSchedulerEdgeGroups(edgeId, schedulerEventId) {
        var deferred = $q.defer();

        resolvedData[0].assignedEdgeGroups = [];
        resolvedData[0].assignedEdgeGroups.push([{entityGroupId: {edgeId: edgeId, schedulerEventId: schedulerEventId}, name: "All"}]);

        $http.get('updateSchedulerEdgeGroups').then(function success() {
            deferred.resolve(utils.prepareAssignedEdgeGroup(resolvedData));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

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
