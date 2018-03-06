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
import './plugin.scss';

/* eslint-disable import/no-unresolved, import/default */

import pluginFieldsetTemplate from './plugin-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function PluginDirective($compile, $templateCache, $translate, types, toast, utils, userService, componentDescriptorService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(pluginFieldsetTemplate);
        element.html(template);

        scope.showPluginConfig = false;

        scope.pluginConfiguration = {
            data: null
        };

        if (scope.plugin && !scope.plugin.configuration) {
            scope.plugin.configuration = {};
        }

        scope.$watch("plugin.clazz", function (newValue, prevValue) {
            if (newValue != prevValue) {
                scope.pluginConfiguration.data = null;
                if (scope.plugin) {
                    componentDescriptorService.getComponentDescriptorByClazz(scope.plugin.clazz).then(
                        function success(component) {
                            scope.pluginComponent = component;
                            scope.showPluginConfig = !(userService.getAuthority() === 'TENANT_ADMIN'
                                                        && scope.plugin.tenantId
                                                        && scope.plugin.tenantId.id === types.id.nullUid)
                                                      && utils.isDescriptorSchemaNotEmpty(scope.pluginComponent.configurationDescriptor);
                            scope.pluginConfiguration.data = angular.copy(scope.plugin.configuration);
                        },
                        function fail() {
                        }
                    );
                }
            }
        });

        scope.$watch("pluginConfiguration.data", function (newValue, prevValue) {
            if (newValue && !angular.equals(newValue, prevValue)) {
                scope.plugin.configuration = angular.copy(scope.pluginConfiguration.data);
            }
        }, true);

        scope.onPluginIdCopied = function() {
            toast.showSuccess($translate.instant('plugin.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        componentDescriptorService.getComponentDescriptorsByType(types.componentType.plugin).then(
            function success(components) {
                scope.pluginComponents = components;
            },
            function fail() {
            }
        );

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            plugin: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '=',
            onActivatePlugin: '&',
            onSuspendPlugin: '&',
            onExportPlugin: '&',
            onDeletePlugin: '&'
        }
    };
}
