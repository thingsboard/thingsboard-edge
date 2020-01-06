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
/* eslint-disable import/no-unresolved, import/default */

import entityGroupsTemplate from '../group/entity-groups.tpl.html';
import entityGroupTemplate from '../group/entity-group.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CustomerHierarchyViewDirective($compile, $templateCache, $controller, $q, entityGroupService) {
    var linker = function (scope, element, attrs) {

        var inited = false;

        attrs.$observe('mode', function() {
            loadView();
        });

        scope.$on("$destroy", function () {
            if (scope.viewScope) {
                scope.viewScope.$destroy();
            }
        });

        function loadView() {
            var controller, template;
            if (attrs.mode === 'groups') {
                controller = 'EntityGroupsController';
                template = $templateCache.get(entityGroupsTemplate);
            } else if (attrs.mode === 'group') {
                controller = 'EntityGroupController';
                template = $templateCache.get(entityGroupTemplate);
            }
            scope.stateParams.hierarchyCallbacks.reload = () => {
                var promise;
                if (attrs.mode === 'groups') {
                    promise = $q.when();
                } else if (attrs.mode === 'group') {
                    var deferred = $q.defer();
                    entityGroupService.constructGroupConfig(scope.stateParams, scope.stateParams.entityGroup).then(
                        (entityGroup) => {
                            scope.stateParams.entityGroup = entityGroup;
                            scope.locals.entityGroup = entityGroup;
                            deferred.resolve();
                        },
                        () => {
                            deferred.reject();
                        }
                    );
                    promise = deferred.promise;
                }
                if (promise) {
                    promise.then(() => {
                        if (!inited) {
                            initView(template, controller);
                            inited = true;
                        } else {
                            if (scope.stateParams.hierarchyCallbacks.reloadData) {
                                scope.stateParams.hierarchyCallbacks.reloadData();
                            }
                        }
                    });
                }
            };
        }

        function initView(template, controller) {
            if (controller && template) {
                if (scope.viewScope) {
                    scope.viewScope.$destroy();
                }
                element.html(template);
                scope.viewScope = scope.$new();

                scope.viewScope.searchConfig = {
                    searchEnabled: false,
                    searchByEntitySubtype: false,
                    searchEntityType: null,
                    showSearch: false,
                    searchText: "",
                    searchEntitySubtype: ""
                };

                var locals = {$scope: scope.viewScope, $element: element, $stateParams: scope.stateParams};
                if (scope.locals) {
                    angular.extend(locals, scope.locals);
                }

                $controller(controller + ' as vm', locals);

                $compile(element.contents())(scope.viewScope);
            }
        }
    };

    return {
        restrict: "E",
        link: linker,
        scope: {
            stateParams: '=',
            locals: '='
        }
    };
}
