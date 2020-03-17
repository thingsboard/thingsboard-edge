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
export default angular.module('thingsboard.api.alarm', [])
    .factory('alarmService', AlarmService)
    .name;

/*@ngInject*/
function AlarmService($http, $q, $interval, $filter, $timeout, utils, types) {

    var alarmSourceListeners = {};

    var simulatedAlarm = {
        createdTime: (new Date).getTime(),
        startTs: (new Date).getTime(),
        endTs: 0,
        ackTs: 0,
        clearTs: 0,
        originatorName: 'Simulated',
        originator: {
            entityType: "DEVICE",
            id: "1"
        },
        type: 'TEMPERATURE',
        severity: "MAJOR",
        status: types.alarmStatus.activeUnack,
        details: {
            message: "Temperature is high!"
        }
    };

    var service = {
        getAlarm: getAlarm,
        getAlarmInfo: getAlarmInfo,
        saveAlarm: saveAlarm,
        ackAlarm: ackAlarm,
        clearAlarm: clearAlarm,
        getAlarms: getAlarms,
        getHighestAlarmSeverity: getHighestAlarmSeverity,
        pollAlarms: pollAlarms,
        cancelPollAlarms: cancelPollAlarms,
        subscribeForAlarms: subscribeForAlarms,
        unsubscribeFromAlarms: unsubscribeFromAlarms
    }

    return service;

    function getAlarm(alarmId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/' + alarmId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getAlarmInfo(alarmId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/info/' + alarmId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveAlarm(alarm, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/alarm';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, alarm, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function ackAlarm(alarmId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/' + alarmId + '/ack';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function clearAlarm(alarmId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/' + alarmId + '/clear';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getAlarms(entityType, entityId, pageLink, alarmSearchStatus, alarmStatus, fetchOriginator, ascOrder, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/' + entityType + '/' + entityId + '?limit=' + pageLink.limit;

        if (angular.isDefined(pageLink.startTime) && pageLink.startTime != null) {
            url += '&startTime=' + pageLink.startTime;
        }
        if (angular.isDefined(pageLink.endTime) && pageLink.endTime != null) {
            url += '&endTime=' + pageLink.endTime;
        }
        if (angular.isDefined(pageLink.idOffset) && pageLink.idOffset != null) {
            url += '&offset=' + pageLink.idOffset;
        }
        if (alarmSearchStatus) {
            url += '&searchStatus=' + alarmSearchStatus;
        }
        if (alarmStatus) {
            url += '&status=' + alarmStatus;
        }
        if (fetchOriginator) {
            url += '&fetchOriginator=' + ((fetchOriginator===true) ? 'true' : 'false');
        }
        if (angular.isDefined(ascOrder) && ascOrder != null) {
            url += '&ascOrder=' + (ascOrder ? 'true' : 'false');
        }

        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getHighestAlarmSeverity(entityType, entityId, alarmSearchStatus, alarmStatus, config) {
        var deferred = $q.defer();
        var url = '/api/alarm/highestSeverity/' + entityType + '/' + entityId;

        if (alarmSearchStatus) {
            url += '?searchStatus=' + alarmSearchStatus;
        } else if (alarmStatus) {
            url += '?status=' + alarmStatus;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function fetchAlarms(alarmsQuery, pageLink, deferred, alarmsList) {
        getAlarms(alarmsQuery.entityType, alarmsQuery.entityId,
            pageLink, alarmsQuery.alarmSearchStatus, alarmsQuery.alarmStatus,
            alarmsQuery.fetchOriginator, false, {ignoreLoading: true}).then(
            function success(alarms) {
                if (!alarmsList) {
                    alarmsList = [];
                }
                alarmsList = alarmsList.concat(alarms.data);
                if (alarms.hasNext && !alarmsQuery.limit) {
                    fetchAlarms(alarmsQuery, alarms.nextPageLink, deferred, alarmsList);
                } else {
                    alarmsList = $filter('orderBy')(alarmsList, ['-createdTime']);
                    deferred.resolve(alarmsList);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
    }

    function getAlarmsByQuery(alarmsQuery) {
        var deferred = $q.defer();
        var time = Date.now();
        var pageLink;
        if (alarmsQuery.limit) {
            pageLink = {
                limit: alarmsQuery.limit
            };
        } else if (alarmsQuery.interval) {
            pageLink = {
                limit: 100,
                startTime: time - alarmsQuery.interval
            };
        } else if (alarmsQuery.startTime) {
            pageLink = {
                limit: 100,
                startTime: Math.round(alarmsQuery.startTime)
            }
            if (alarmsQuery.endTime) {
                pageLink.endTime = Math.round(alarmsQuery.endTime);
            }
        }

        fetchAlarms(alarmsQuery, pageLink, deferred);
        return deferred.promise;
    }

    function onPollAlarms(alarmsQuery) {
        getAlarmsByQuery(alarmsQuery).then(
            function success(alarms) {
                alarmsQuery.onAlarms(alarms);
            },
            function fail() {}
        );
    }

    function pollAlarms(entityType, entityId, alarmStatus, interval, limit, pollingInterval, onAlarms) {
        var alarmsQuery = {
            entityType: entityType,
            entityId: entityId,
            alarmSearchStatus: null,
            alarmStatus: alarmStatus,
            fetchOriginator: false,
            interval: interval,
            limit: limit,
            onAlarms: onAlarms
        };
        onPollAlarms(alarmsQuery);
        return $interval(onPollAlarms, pollingInterval, 0, false, alarmsQuery);
    }

    function cancelPollAlarms(pollPromise) {
        if (angular.isDefined(pollPromise)) {
            $interval.cancel(pollPromise);
        }
    }

    function subscribeForAlarms(alarmSourceListener) {
        alarmSourceListener.id = utils.guid();
        alarmSourceListeners[alarmSourceListener.id] = alarmSourceListener;
        var alarmSource = alarmSourceListener.alarmSource;
        if (alarmSource.type == types.datasourceType.function) {
            $timeout(function() {
                alarmSourceListener.alarmsUpdated([simulatedAlarm], false);
            });
        } else if (alarmSource.entityType && alarmSource.entityId) {
            var pollingInterval = alarmSourceListener.alarmsPollingInterval;
            alarmSourceListener.alarmsQuery = {
                entityType: alarmSource.entityType,
                entityId: alarmSource.entityId,
                alarmSearchStatus: alarmSourceListener.alarmSearchStatus,
                alarmStatus: null
            }
            var originatorKeys = $filter('filter')(alarmSource.dataKeys, {name: 'originator'});
            if (originatorKeys && originatorKeys.length) {
                alarmSourceListener.alarmsQuery.fetchOriginator = true;
            }
            var subscriptionTimewindow = alarmSourceListener.subscriptionTimewindow;
            if (subscriptionTimewindow.realtimeWindowMs) {
                alarmSourceListener.alarmsQuery.startTime = subscriptionTimewindow.startTs;
            } else {
                alarmSourceListener.alarmsQuery.startTime = subscriptionTimewindow.fixedWindow.startTimeMs;
                alarmSourceListener.alarmsQuery.endTime = subscriptionTimewindow.fixedWindow.endTimeMs;
            }
            alarmSourceListener.alarmsQuery.onAlarms = function(alarms) {
                if (subscriptionTimewindow.realtimeWindowMs) {
                    var now = Date.now();
                    if (alarmSourceListener.lastUpdateTs) {
                        var interval = now - alarmSourceListener.lastUpdateTs;
                        alarmSourceListener.alarmsQuery.startTime += interval;
                    }
                    alarmSourceListener.lastUpdateTs = now;
                }
                alarmSourceListener.alarmsUpdated(alarms, false);
            }
            onPollAlarms(alarmSourceListener.alarmsQuery);
            alarmSourceListener.pollPromise = $interval(onPollAlarms, pollingInterval,
                0, false, alarmSourceListener.alarmsQuery);
        }

    }

    function unsubscribeFromAlarms(alarmSourceListener) {
        if (alarmSourceListener && alarmSourceListener.id) {
            if (alarmSourceListener.pollPromise) {
                $interval.cancel(alarmSourceListener.pollPromise);
                alarmSourceListener.pollPromise = null;
            }
            delete alarmSourceListeners[alarmSourceListener.id];
        }
    }
}
