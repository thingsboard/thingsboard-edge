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
import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/theme/github';
import beautify from 'js-beautify';

import './alarm-details-dialog.scss';

const js_beautify = beautify.js;

/*@ngInject*/
export default function AlarmDetailsDialogController($mdDialog, $filter, $translate, types,
                                                     alarmService, alarmId, allowAcknowledgment, allowClear, displayDetails, showingCallback) {

    var vm = this;

    vm.alarmId = alarmId;
    vm.allowAcknowledgment = allowAcknowledgment;
    vm.allowClear = allowClear;
    vm.displayDetails = displayDetails;
    vm.types = types;
    vm.alarm = null;

    vm.alarmUpdated = false;

    showingCallback.onShowing = function(scope, element) {
        if (vm.displayDetails) {
            updateEditorSize(element);
        }
    }

    vm.alarmDetailsOptions = {
        useWrapMode: false,
        mode: 'json',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.editor = _ace;
        }
    };

    vm.close = close;
    vm.acknowledge = acknowledge;
    vm.clear = clear;

    loadAlarm();

    function updateEditorSize(element) {
        var newWidth = 600;
        var newHeight = 200;
        angular.element('#tb-alarm-details', element).height(newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
        vm.editor.resize();
    }

    function loadAlarm() {
        alarmService.getAlarmInfo(vm.alarmId).then(
            function success(alarm) {
                vm.alarm = alarm;
                loadAlarmFields();
            },
            function fail() {
                vm.alarm = null;
            }
        );
    }

    function loadAlarmFields() {
        vm.createdTime = $filter('date')(vm.alarm.createdTime, 'yyyy-MM-dd HH:mm:ss');
        vm.startTime = null;
        if (vm.alarm.startTs) {
            vm.startTime = $filter('date')(vm.alarm.startTs, 'yyyy-MM-dd HH:mm:ss');
        }
        vm.endTime = null;
        if (vm.alarm.endTs) {
            vm.endTime = $filter('date')(vm.alarm.endTs, 'yyyy-MM-dd HH:mm:ss');
        }
        vm.ackTime = null;
        if (vm.alarm.ackTs) {
            vm.ackTime = $filter('date')(vm.alarm.ackTs, 'yyyy-MM-dd HH:mm:ss')
        }
        vm.clearTime = null;
        if (vm.alarm.clearTs) {
            vm.clearTime = $filter('date')(vm.alarm.clearTs, 'yyyy-MM-dd HH:mm:ss');
        }

        vm.alarmSeverity = $translate.instant(types.alarmSeverity[vm.alarm.severity].name);

        vm.alarmStatus = $translate.instant('alarm.display-status.' + vm.alarm.status);

        vm.alarmDetails = null;
        if (vm.alarm.details) {
            vm.alarmDetails = angular.toJson(vm.alarm.details);
            vm.alarmDetails = js_beautify(vm.alarmDetails, {indent_size: 4});
        }
    }

    function acknowledge () {
        alarmService.ackAlarm(vm.alarmId).then(
            function success() {
                vm.alarmUpdated = true;
                loadAlarm();
            }
        );
    }

    function clear () {
        alarmService.clearAlarm(vm.alarmId).then(
            function success() {
                vm.alarmUpdated = true;
                loadAlarm();
            }
        );
    }

    function close () {
        $mdDialog.hide(vm.alarmUpdated ? vm.alarm : null);
    }

}
