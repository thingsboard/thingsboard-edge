/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

/*@ngInject*/
export default function CustomerHierarchyViewDirective($compile, $templateCache, $controller) {
    var linker = function (scope, element) {


        scope.$watch('currentTemplateUrl', function (newVal) {
            if (newVal) {
                loadView();
            }
        });

        scope.$on('hierarchyViewChanged', () => {
            if (scope.currentTemplateUrl) {
                loadView();
            }
        });

        function loadView() {
            if (scope.viewScope) {
                scope.viewScope.$destroy();
            }
            var template = $templateCache.get(scope.currentTemplateUrl);
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

            $controller(scope.currentController + ' as vm', locals);

            $compile(element.contents())(scope.viewScope);
        }
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            currentTemplateUrl: '=',
            currentController: '=',
            stateParams: '=',
            locals: '='
        }
    };
}
