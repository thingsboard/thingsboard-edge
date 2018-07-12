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

import eventHeaderLcEventTemplate from './event-header-lc-event.tpl.html';
import eventHeaderStatsTemplate from './event-header-stats.tpl.html';
import eventHeaderErrorTemplate from './event-header-error.tpl.html';
import eventHeaderDebugConverterTemplate from './event-header-debug-converter.tpl.html';
import eventHeaderDebugIntegrationTemplate from './event-header-debug-integration.tpl.html';
import eventHeaderDebugRuleNodeTemplate from './event-header-debug-rulenode.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EventHeaderDirective($compile, $templateCache, types) {

    var linker = function (scope, element, attrs) {

        var getTemplate = function(eventType) {
            var template = '';
            switch(eventType) {
                case types.eventType.lcEvent.value:
                    template = eventHeaderLcEventTemplate;
                    break;
                case types.eventType.stats.value:
                    template = eventHeaderStatsTemplate;
                    break;
                case types.eventType.error.value:
                    template = eventHeaderErrorTemplate;
                    break;
                case types.debugEventType.debugConverter.value:
                    template = eventHeaderDebugConverterTemplate;
                    break;
                case types.debugEventType.debugIntegration.value:
                    template = eventHeaderDebugIntegrationTemplate;
                    break;
                case types.debugEventType.debugRuleNode.value:
                    template = eventHeaderDebugRuleNodeTemplate;
                    break;
                case types.debugEventType.debugRuleChain.value:
                    template = eventHeaderDebugRuleNodeTemplate;
                    break;
            }
            return $templateCache.get(template);
        };

        scope.loadTemplate = function() {
            element.html(getTemplate(attrs.eventType));
            $compile(element.contents())(scope);
        };

        attrs.$observe('eventType', function() {
            scope.loadTemplate();
        });

    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
