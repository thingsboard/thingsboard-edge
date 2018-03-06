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
import './rule.scss';

/* eslint-disable import/no-unresolved, import/default */

import ruleFieldsetTemplate from './rule-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleDirective($compile, $templateCache, $mdDialog, $document, $q, $translate, pluginService,
                                      componentDialogService, componentDescriptorService, types, toast) {
    var linker = function (scope, element) {
        var template = $templateCache.get(ruleFieldsetTemplate);
        element.html(template);

        scope.plugin = null;
        scope.types = types;
        scope.filters = [];

        scope.addFilter = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.filter', types.componentType.filter).then(
                function success(filter) {
                    scope.filters.push({ value: filter });
                },
                function fail() {}
            );
        }

        scope.removeFilter = function ($event, filter) {
            var index = scope.filters.indexOf(filter);
            if (index > -1) {
                scope.filters.splice(index, 1);
            }
        };

        scope.addProcessor = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.processor', types.componentType.processor).then(
                function success(processor) {
                    scope.rule.processor = processor;
                },
                function fail() {}
            );
        }

        scope.removeProcessor = function() {
            if (scope.rule.processor) {
                scope.rule.processor = null;
            }
        }

        scope.addAction = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.plugin-action', types.componentType.action, scope.plugin.clazz).then(
                function success(action) {
                    scope.rule.action = action;
                },
                function fail() {}
            );
        }

        scope.removeAction = function() {
            if (scope.rule.action) {
                scope.rule.action = null;
            }
        }

        scope.updateValidity = function () {
            if (scope.rule) {
                var valid = scope.rule.filters && scope.rule.filters.length > 0;
                scope.theForm.$setValidity('filters', valid);
                var processorDefined = angular.isDefined(scope.rule.processor) && scope.rule.processor != null;
                var pluginDefined = angular.isDefined(scope.rule.pluginToken) && scope.rule.pluginToken != null;
                var pluginActionDefined = angular.isDefined(scope.rule.action) && scope.rule.action != null;
                valid = processorDefined && !pluginDefined || (pluginDefined && pluginActionDefined);
                scope.theForm.$setValidity('processorOrPlugin', valid);
            }
        };

        scope.onRuleIdCopied = function() {
            toast.showSuccess($translate.instant('rule.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.$watch('rule', function(newVal, prevVal) {
                if (newVal) {
                    if (!scope.rule.filters) {
                        scope.rule.filters = [];
                    }
                    if (!angular.equals(newVal, prevVal)) {
                        if (scope.rule.pluginToken) {
                            pluginService.getPluginByToken(scope.rule.pluginToken).then(
                                function success(plugin) {
                                    scope.plugin = plugin;
                                },
                                function fail() {}
                            );
                        } else {
                            scope.plugin = null;
                        }
                        if (scope.filters) {
                            scope.filters.splice(0, scope.filters.length);
                        } else {
                            scope.filters = [];
                        }
                        if (scope.rule.filters) {
                            for (var i in scope.rule.filters) {
                                scope.filters.push({value: scope.rule.filters[i]});
                            }
                        }
                    }
                    scope.updateValidity();
                }
            }
        );

        scope.$watch('filters', function (newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                if (scope.rule.filters) {
                    scope.rule.filters.splice(0, scope.rule.filters.length);
                } else {
                    scope.rule.filters = [];
                }
                if (scope.filters) {
                    for (var i in scope.filters) {
                        scope.rule.filters.push(scope.filters[i].value);
                    }
                }
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        scope.$watch('plugin', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                if (newVal) {
                    scope.rule.pluginToken = scope.plugin.apiToken;
                } else {
                    scope.rule.pluginToken = null;
                }
                scope.rule.action = null;
                scope.updateValidity();
            }
        }, true);

        scope.$watch('rule.processor', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        scope.$watch('rule.action', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            rule: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '=',
            onActivateRule: '&',
            onSuspendRule: '&',
            onExportRule: '&',
            onDeleteRule: '&'
        }
    };
}
