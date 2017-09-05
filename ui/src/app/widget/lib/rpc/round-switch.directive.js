/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
import './round-switch.scss';

/* eslint-disable import/no-unresolved, import/default */

import roundSwitchTemplate from './round-switch.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.rpc.roundSwitch', [])
    .directive('tbRoundSwitch', RoundSwitch)
    .name;

/*@ngInject*/
function RoundSwitch() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: RoundSwitchController,
        controllerAs: 'vm',
        templateUrl: roundSwitchTemplate
    };
}

/*@ngInject*/
function RoundSwitchController($element, $scope, utils) {
    let vm = this;

    vm.showTitle = false;
    vm.value = false;
    vm.error = '';

    vm.checkboxId = 'onoff-' + utils.guid();

    var switchElement = angular.element('.switch', $element),
        switchContainer = angular.element('#switch-container', $element),
        onoff = angular.element('input', $element),
        textMeasure = angular.element('#text-measure', $element),
        switchTitleContainer = angular.element('.title-container', $element),
        switchTitle = angular.element('.switch-title', $element),
        switchErrorContainer = angular.element('.error-container', $element),
        switchError = angular.element('.switch-error', $element);

    onoff.bind('change', () => {
        vm.value = onoff.prop('checked') === false;
        onValue();
    });

    $scope.$watch('vm.ctx', () => {
        if (vm.ctx) {
            init();
        }
    });

    resize();

    function init() {

        vm.title = angular.isDefined(vm.ctx.settings.title) ? vm.ctx.settings.title : '';
        vm.showTitle = vm.title && vm.title.length ? true : false;
        vm.ctx.resize = resize;
        $scope.$applyAsync(() => {
            resize();
        });
        var initialValue = angular.isDefined(vm.ctx.settings.initialValue) ? vm.ctx.settings.initialValue : false;
        setValue(initialValue);

        var subscription = vm.ctx.defaultSubscription;
        var rpcEnabled = subscription.rpcEnabled;

        vm.isSimulated = $scope.widgetEditMode;

        vm.requestTimeout = 500;
        if (vm.ctx.settings.requestTimeout) {
            vm.requestTimeout = vm.ctx.settings.requestTimeout;
        }
        vm.getValueMethod = 'getValue';
        if (vm.ctx.settings.getValueMethod && vm.ctx.settings.getValueMethod.length) {
            vm.getValueMethod = vm.ctx.settings.getValueMethod;
        }
        vm.setValueMethod = 'setValue';
        if (vm.ctx.settings.setValueMethod && vm.ctx.settings.setValueMethod.length) {
            vm.setValueMethod = vm.ctx.settings.setValueMethod;
        }
        if (!rpcEnabled) {
            onError('Target device is not set!');
        } else {
            if (!vm.isSimulated) {
                rpcRequestValue();
            }
        }
    }

    function resize() {
        var width = switchContainer.width();
        var height = switchContainer.height();
        var size = Math.min(width, height);
        var scale = size/260;
        switchElement.css({
            '-webkit-transform': `scale(${scale})`,
            '-moz-transform': `scale(${scale})`,
            '-ms-transform': `scale(${scale})`,
            '-o-transform': `scale(${scale})`,
            transform: `scale(${scale})`
        });
        if (vm.showTitle) {
            setFontSize(switchTitle, vm.title, switchTitleContainer.height() * 2 / 3, switchTitleContainer.width());
        }
        setFontSize(switchError, vm.error, switchErrorContainer.height(), switchErrorContainer.width());
    }

    function setValue(value) {
        vm.value = value ? true : false;
        onoff.prop('checked', !vm.value);
    }

    function onValue() {
        rpcUpdateValue(vm.value);
    }

    function onError(error) {
        $scope.$applyAsync(() => {
            vm.error = error;
            setFontSize(switchError, vm.error, switchErrorContainer.height(), switchErrorContainer.width());
        });
    }

    function setFontSize(element, text, fontSize, maxWidth) {
        var textWidth = measureTextWidth(text, fontSize);
        while (textWidth > maxWidth) {
            fontSize--;
            textWidth = measureTextWidth(text, fontSize);
        }
        element.css({'fontSize': fontSize+'px', 'lineHeight': fontSize+'px'});
    }

    function measureTextWidth(text, fontSize) {
        textMeasure.css({'fontSize': fontSize+'px', 'lineHeight': fontSize+'px'});
        textMeasure.text(text);
        return textMeasure.width();
    }

    function rpcRequestValue() {
        vm.error = '';
        vm.ctx.controlApi.sendTwoWayCommand(vm.getValueMethod, null, vm.requestTimeout).then(
            (responseBody) => {
                setValue(responseBody);
            },
            () => {
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
            }
        );
    }

    function rpcUpdateValue(value) {
        if (vm.executingUpdateValue) {
            vm.scheduledValue = value;
            return;
        } else {
            vm.scheduledValue = null;
            vm.rpcValue = value;
            vm.executingUpdateValue = true;
        }
        vm.error = '';
        vm.ctx.controlApi.sendOneWayCommand(vm.setValueMethod, value, vm.requestTimeout).then(
            () => {
                vm.executingUpdateValue = false;
                if (vm.scheduledValue != null && vm.scheduledValue != vm.rpcValue) {
                    rpcUpdateValue(vm.scheduledValue);
                }
            },
            () => {
                vm.executingUpdateValue = false;
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
            }
        );
    }
}
