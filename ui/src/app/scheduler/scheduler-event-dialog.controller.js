/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import './scheduler-event-dialog.scss';

/*@ngInject*/
export default function SchedulerEventDialogController($rootScope, $scope, $mdDialog, schedulerEventService, types,
                                                       configTypesList, isAdd, schedulerEvent, defaultEventType) {

    var vm = this;

    vm.types = types;

    vm.defaultTimezone = moment.tz.guess(); //eslint-disable-line

    vm.configTypesList = configTypesList;

    vm.configTypes = {};
    configTypesList.forEach((configType) => {
        vm.configTypes[configType.value] = configType;
    });

    vm.schedulerEvent = schedulerEvent;
    vm.defaultEventType = defaultEventType;
    vm.isAdd = isAdd;
    vm.repeatType = types.schedulerRepeat;

    var startDate;
    if (vm.isAdd) {
        vm.schedulerEvent.type = vm.defaultEventType;
        if (!vm.schedulerEvent.schedule.timezone) {
            vm.schedulerEvent.schedule.timezone = vm.defaultTimezone;
        }
        if (!vm.schedulerEvent.schedule.startTime) {
            var date = new Date();
            startDate = new Date(
                date.getFullYear(),
                date.getMonth(),
                date.getDate());
        } else {
            startDate = dateFromUtcTime(vm.schedulerEvent.schedule.startTime);
        }
    } else {
        startDate = dateFromUtcTime(vm.schedulerEvent.schedule.startTime);
        if (vm.schedulerEvent.schedule.repeat) {
            if (vm.schedulerEvent.schedule.repeat.type == types.schedulerRepeat.weekly.value &&
                vm.schedulerEvent.schedule.repeat.repeatOn) {
                    vm.weeklyRepeat = [];
                    for (var i = 0; i < vm.schedulerEvent.schedule.repeat.repeatOn.length; i++) {
                        vm.weeklyRepeat[vm.schedulerEvent.schedule.repeat.repeatOn[i]] = true;
                    }
            }
            vm.endsOn = dateFromUtcTime(vm.schedulerEvent.schedule.repeat.endsOn);
        }
    }
    setStartDate(startDate);

    vm.lastAppliedTimezone = vm.schedulerEvent.schedule.timezone;

    vm.repeat = vm.schedulerEvent.schedule.repeat ? true : false;

    vm.repeatsChange = repeatsChange;
    vm.repeatTypeChange = repeatTypeChange;
    vm.weekDayChange = weekDayChange;

    vm.save = save;
    vm.cancel = cancel;

    $scope.$watch('vm.schedulerEvent.type', function (newValue, prevValue) {
        if (!angular.equals(newValue, prevValue)) {
            vm.schedulerEvent.configuration = {};
        }
    });

    $scope.$watch('vm.schedulerEvent.schedule.timezone', function (newValue, prevValue) {
        if (!angular.equals(newValue, prevValue) && newValue) {
            timezoneChange();
        }
    });

    function dateFromUtcTime(time, timezone) {
        if (!timezone) {
            timezone = vm.schedulerEvent.schedule.timezone;
        }
        var offset = moment.tz.zone(timezone).utcOffset(time) * 60 * 1000; //eslint-disable-line
        return new Date(time - offset + Date.getTimezoneOffset()*60*1000);
    }
    
    function dateTimeToUtcTime(date, time, timezone) {
        if (!timezone) {
            timezone = vm.schedulerEvent.schedule.timezone;
        }
        var ts = new Date(
            date.getFullYear(),
            date.getMonth(),
            date.getDate(),
            time.getHours(),
            time.getMinutes(),
            time.getSeconds(),
            time.getMilliseconds()
        ).getTime();
        var offset = moment.tz.zone(timezone).utcOffset(ts) * 60 * 1000; //eslint-disable-line
        return ts + offset - Date.getTimezoneOffset()*60*1000;
    }

    function dateToUtcTime(date, timezone) {
        if (!timezone) {
            timezone = vm.schedulerEvent.schedule.timezone;
        }
        var ts = new Date(
            date.getFullYear(),
            date.getMonth(),
            date.getDate()
        ).getTime();
        var offset = moment.tz.zone(timezone).utcOffset(ts) * 60 * 1000; //eslint-disable-line
        return ts + offset - Date.getTimezoneOffset()*60*1000;
    }

    function timezoneChange() {
        if (!angular.equals(vm.schedulerEvent.schedule.timezone, vm.lastAppliedTimezone)) {
            var startTime = dateTimeToUtcTime(vm.startDate, vm.startDateTime, vm.lastAppliedTimezone);
            var startDate = dateFromUtcTime(startTime);
            setStartDate(startDate);
            if (vm.endsOn) {
                var endsOnTime = dateToUtcTime(vm.endsOn, vm.lastAppliedTimezone);
                vm.endsOn = dateFromUtcTime(endsOnTime);
            }
            vm.lastAppliedTimezone = vm.schedulerEvent.schedule.timezone;
        }
    }

    function setStartDate(startDate) {
        vm.startDate = new Date(
            startDate.getFullYear(),
            startDate.getMonth(),
            startDate.getDate());
        var millis = (startDate.getHours() * 60 * 60 * 1000 + startDate.getMinutes() * 60 * 1000) + Date.getTimezoneOffset() * 60 * 1000;
        vm.startDateTime = new Date(millis);
    }

    function repeatsChange() {
        if (vm.repeat) {
            if (!vm.schedulerEvent.schedule.repeat) {
                vm.schedulerEvent.schedule.repeat = {
                    type: types.schedulerRepeat.daily.value
                }
            }
            vm.endsOn = new Date(
                vm.startDate.getFullYear(),
                vm.startDate.getMonth(),
                vm.startDate.getDate()+5);
        }
    }

    function repeatTypeChange() {
        if (vm.repeat && vm.schedulerEvent.schedule.repeat && vm.schedulerEvent.schedule.repeat.type == types.schedulerRepeat.weekly.value) {
            if (!vm.weeklyRepeat) {
                vm.weeklyRepeat = [];
            }
            weekDayChange();
        }
    }

    function weekDayChange() {
        if (vm.repeat && vm.startDate) {
            var setCurrentDay = true;
            for (var i=0;i<7;i++) {
                if (vm.weeklyRepeat[i]) {
                    setCurrentDay = false;
                    break;
                }
            }
            if (setCurrentDay) {
                var day = moment(vm.startDate).day(); //eslint-disable-line
                vm.weeklyRepeat[day] = true;
            }
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        if (!vm.repeat) {
            delete vm.schedulerEvent.schedule.repeat;
        } else {
            vm.schedulerEvent.schedule.repeat.endsOn = dateToUtcTime(vm.endsOn);
            if (vm.schedulerEvent.schedule.repeat.type == types.schedulerRepeat.weekly.value) {
                vm.schedulerEvent.schedule.repeat.repeatOn = [];
                for (var i=0;i<7;i++) {
                    if (vm.weeklyRepeat[i]) {
                        vm.schedulerEvent.schedule.repeat.repeatOn.push(i);
                    }
                }
            } else {
                delete vm.schedulerEvent.schedule.repeat.repeatOn;
            }
        }
        vm.schedulerEvent.schedule.startTime = dateTimeToUtcTime(vm.startDate, vm.startDateTime);
        schedulerEventService.saveSchedulerEvent(vm.schedulerEvent).then(
            () => {
                $mdDialog.hide();
            }
        );
    }
}
