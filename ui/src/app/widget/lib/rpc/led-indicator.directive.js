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
import './led-indicator.scss';

import tinycolor from 'tinycolor2';

/* eslint-disable import/no-unresolved, import/default */

import ledIndicatorTemplate from './led-indicator.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.rpc.ledIndicator', [])
    .directive('tbLedIndicator', LedIndicator)
    .name;

/*@ngInject*/
function LedIndicator() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: LedIndicatorController,
        controllerAs: 'vm',
        templateUrl: ledIndicatorTemplate
    };
}

/*@ngInject*/
function LedIndicatorController($element, $scope, $timeout, utils, types) {
    let vm = this;

    vm.showTitle = false;
    vm.value = false;
    vm.error = '';

    const checkStatusPollingInterval = 10000;

    vm.subscriptionOptions = {
        callbacks: {
            onDataUpdated: onDataUpdated,
            onDataUpdateError: onDataUpdateError,
            dataLoading: () => {}
        }
    };

    var led = angular.element('.led', $element),
        ledContainer = angular.element('#led-container', $element),
        textMeasure = angular.element('#text-measure', $element),
        ledTitleContainer = angular.element('.title-container', $element),
        ledTitle = angular.element('.led-title', $element),
        ledErrorContainer = angular.element('.error-container', $element),
        ledError = angular.element('.led-error', $element);

    $scope.$watch('vm.ctx', () => {
        if (vm.ctx) {
            init();
        }
    });

    $scope.$on('$destroy', () => {
        vm.destroyed = true;
        if (vm.checkStatusTimeoutHandle) {
            $timeout.cancel(vm.checkStatusTimeoutHandle);
        }
        if (vm.subscription) {
            vm.ctx.subscriptionApi.removeSubscription(vm.subscription.id);
        }
    });

    resize();

    function init() {

        vm.title = angular.isDefined(vm.ctx.settings.title) ? vm.ctx.settings.title : '';
        vm.showTitle = vm.title && vm.title.length ? true : false;

        var origColor = angular.isDefined(vm.ctx.settings.ledColor) ? vm.ctx.settings.ledColor : 'green';

        vm.valueAttribute = angular.isDefined(vm.ctx.settings.valueAttribute) ? vm.ctx.settings.valueAttribute : 'value';

        vm.ledColor = tinycolor(origColor).brighten(30).toHexString();
        vm.ledMiddleColor = tinycolor(origColor).toHexString();
        vm.disabledColor = tinycolor(origColor).darken(40).toHexString();
        vm.disabledMiddleColor = tinycolor(origColor).darken(60).toHexString();

        vm.ctx.resize = resize;
        $scope.$applyAsync(() => {
            resize();
        });
        var initialValue = angular.isDefined(vm.ctx.settings.initialValue) ? vm.ctx.settings.initialValue : false;
        setValue(initialValue, true);

        var subscription = vm.ctx.defaultSubscription;
        var rpcEnabled = subscription.rpcEnabled;

        vm.isSimulated = $scope.widgetEditMode;

        vm.requestTimeout = 500;
        if (vm.ctx.settings.requestTimeout) {
            vm.requestTimeout = vm.ctx.settings.requestTimeout;
        }
        vm.retrieveValueMethod = 'attribute';
        if (vm.ctx.settings.retrieveValueMethod && vm.ctx.settings.retrieveValueMethod.length) {
            vm.retrieveValueMethod = vm.ctx.settings.retrieveValueMethod;
        }

        vm.parseValueFunction = (data) => data ? true : false;
        if (vm.ctx.settings.parseValueFunction && vm.ctx.settings.parseValueFunction.length) {
            try {
                vm.parseValueFunction = new Function('data', vm.ctx.settings.parseValueFunction);
            } catch (e) {
                vm.parseValueFunction = (data) => data ? true : false;
            }
        }

        vm.performCheckStatus = vm.ctx.settings.performCheckStatus != false;
        if (vm.performCheckStatus) {
            vm.checkStatusMethod = 'checkStatus';
            if (vm.ctx.settings.checkStatusMethod && vm.ctx.settings.checkStatusMethod.length) {
                vm.checkStatusMethod = vm.ctx.settings.checkStatusMethod;
            }
        }
        if (!rpcEnabled) {
            onError('Target device is not set!');
        } else {
            if (!vm.isSimulated) {
                if (vm.performCheckStatus) {
                    rpcCheckStatus();
                } else {
                    subscribeForValue();
                }
            }
        }
    }

    function resize() {
        var width = ledContainer.width();
        var height = ledContainer.height();
        var size = Math.min(width, height);

        led.css({width: size, height: size});

        if (vm.showTitle) {
            setFontSize(ledTitle, vm.title, ledTitleContainer.height() * 2 / 3, ledTitleContainer.width());
        }
        setFontSize(ledError, vm.error, ledErrorContainer.height(), ledErrorContainer.width());
    }

    function setValue(value, forceUpdate) {
        if (vm.value != value || forceUpdate) {
            vm.value = value;
            updateColor();
        }
    }

    function updateColor() {
        var color = vm.value ? vm.ledColor : vm.disabledColor;
        var middleColor = vm.value ? vm.ledMiddleColor : vm.disabledMiddleColor;
        var boxShadow = `#000 0 -1px 6px 1px, inset ${middleColor} 0 -1px 8px, ${color} 0 3px 11px`;
        led.css({'backgroundColor': color});
        led.css({'boxShadow': boxShadow});
        if (vm.value) {
            led.removeClass( 'disabled' );
        } else {
            led.addClass( 'disabled' );
        }
    }

    function onError(error) {
        $scope.$applyAsync(() => {
            vm.error = error;
            setFontSize(ledError, vm.error, ledErrorContainer.height(), ledErrorContainer.width());
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

    function rpcCheckStatus() {
        if (vm.destroyed) {
            return;
        }
        vm.error = '';
        vm.ctx.controlApi.sendTwoWayCommand(vm.checkStatusMethod, null, vm.requestTimeout).then(
            (responseBody) => {
                var status = responseBody ? true : false;
                if (status) {
                    if (vm.checkStatusTimeoutHandle) {
                        $timeout.cancel(vm.checkStatusTimeoutHandle);
                        vm.checkStatusTimeoutHandle = null;
                    }
                    subscribeForValue();
                } else {
                    var errorText = 'Unknown device status!';
                    onError(errorText);
                    if (vm.checkStatusTimeoutHandle) {
                        $timeout.cancel(vm.checkStatusTimeoutHandle);
                    }
                    vm.checkStatusTimeoutHandle = $timeout(rpcCheckStatus, checkStatusPollingInterval);
                }
            },
            () => {
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
                if (vm.checkStatusTimeoutHandle) {
                    $timeout.cancel(vm.checkStatusTimeoutHandle);
                }
                vm.checkStatusTimeoutHandle = $timeout(rpcCheckStatus, checkStatusPollingInterval);
            }
        );
    }

    function subscribeForValue() {
        var subscriptionsInfo = [{
            type: types.datasourceType.entity,
            entityType: types.entityType.device,
            entityId: vm.ctx.defaultSubscription.targetDeviceId
        }];

        if (vm.retrieveValueMethod == 'attribute') {
            subscriptionsInfo[0].attributes = [
                {name: vm.valueAttribute}
            ];
        } else {
            subscriptionsInfo[0].timeseries = [
                {name: vm.valueAttribute}
            ];
        }

        vm.ctx.subscriptionApi.createSubscriptionFromInfo (
            types.widgetType.latest.value, subscriptionsInfo, vm.subscriptionOptions, false, true).then(
            function(subscription) {
                vm.subscription = subscription;
            }
        );
    }

    function onDataUpdated(subscription, apply) {
        var value = false;
        var data = subscription.data;
        if (data.length) {
            var keyData = data[0];
            if (keyData && keyData.data && keyData.data[0]) {
                var attrValue = keyData.data[0][1];
                if (attrValue) {
                    var parsed = null;
                    try {
                        parsed = vm.parseValueFunction(angular.fromJson(attrValue));
                    } catch (e){/**/}
                    value = parsed ? true : false;
                }
            }
        }
        setValue(value);
        if (apply) {
            $scope.$digest();
        }
    }

    function onDataUpdateError(subscription, e) {
        var exceptionData = utils.parseException(e);
        var errorText = exceptionData.name;
        if (exceptionData.message) {
            errorText += ': ' + exceptionData.message;
        }
        onError(errorText);
    }

}
