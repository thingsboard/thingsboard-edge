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
import './rulenode.scss';

/* eslint-disable import/no-unresolved, import/default */

import ruleNodeFieldsetTemplate from './rulenode-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleNodeDirective($compile, $templateCache, ruleChainService, types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(ruleNodeFieldsetTemplate);
        element.html(template);

        scope.types = types;

        scope.params = {
            targetRuleChainId: null
        };

        scope.$watch('ruleNode', function() {
            if (scope.ruleNode && scope.ruleNode.component.type == types.ruleNodeType.RULE_CHAIN.value) {
                scope.params.targetRuleChainId = scope.ruleNode.targetRuleChainId;
                watchTargetRuleChain();
            } else {
                if (scope.targetRuleChainWatch) {
                    scope.targetRuleChainWatch();
                    scope.targetRuleChainWatch = null;
                }
            }
        });

        function watchTargetRuleChain() {
            scope.targetRuleChainWatch = scope.$watch('params.targetRuleChainId',
                function(targetRuleChainId) {
                    if (scope.ruleNode.targetRuleChainId != targetRuleChainId) {
                        scope.ruleNode.targetRuleChainId = targetRuleChainId;
                        if (targetRuleChainId) {
                            ruleChainService.getRuleChain(targetRuleChainId).then(
                                (ruleChain) => {
                                    scope.ruleNode.name = ruleChain.name;
                                }
                            );
                        } else {
                            scope.ruleNode.name = "";
                        }
                    }
                }
            );
        }
        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            ruleChainId: '=',
            ruleNode: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '=',
            onDeleteRuleNode: '&'
        }
    };
}
