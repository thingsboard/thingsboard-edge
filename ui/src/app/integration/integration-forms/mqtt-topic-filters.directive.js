/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import mqttTopicFiltersTemplate from './mqtt-topic-filters.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './mqtt-topic-filters.scss';

/*@ngInject*/
export default function MqttTopicFiltersDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(mqttTopicFiltersTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;

        scope.$watch('topicFilters', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.topicFilters);
            }
        }, true);

        ngModelCtrl.$render = function () {
            scope.topicFilters = ngModelCtrl.$viewValue;
            scope.updateValidity();
        };

        scope.addTopicFilter = () => {
            if (!scope.topicFilters) {
                scope.topicFilters = [];
            }
            scope.topicFilters.push(
                {
                    filter: '',
                    qos: 0
                }
            );
            ngModelCtrl.$setDirty();
            scope.updateValidity();
        };

        scope.removeTopicFilter = (index) => {
            if (index > -1) {
                scope.topicFilters.splice(index, 1);
                ngModelCtrl.$setDirty();
                scope.updateValidity();
            }
        };

        scope.updateValidity = () => {
            var topicFiltersValid = true;
            if (!scope.topicFilters || !scope.topicFilters.length) {
                topicFiltersValid = false;
            }
            ngModelCtrl.$setValidity('TopicFilters', topicFiltersValid);
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '=',
            disableMqttTopics: '='
        },
        link: linker
    };
}
