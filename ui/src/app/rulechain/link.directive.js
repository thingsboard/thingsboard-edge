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
/* eslint-disable import/no-unresolved, import/default */

import linkFieldsetTemplate from './link-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function LinkDirective($compile, $templateCache, $filter) {
    var linker = function (scope, element) {
        var template = $templateCache.get(linkFieldsetTemplate);
        element.html(template);

        scope.selectedLabel = null;

        scope.$watch('link', function() {
            scope.selectedLabel = null;
             if (scope.link && scope.labels) {
                 if (scope.link.label) {
                     var result = $filter('filter')(scope.labels, {name: scope.link.label});
                     if (result && result.length) {
                         scope.selectedLabel = result[0];
                     } else {
                         result = $filter('filter')(scope.labels, {custom: true});
                         if (result && result.length && result[0].custom) {
                             scope.selectedLabel = result[0];
                         }
                     }
                 }
             }
        });

        scope.selectedLabelChanged = function() {
            if (scope.link && scope.selectedLabel) {
                if (!scope.selectedLabel.custom) {
                    scope.link.label = scope.selectedLabel.name;
                } else {
                    scope.link.label = "";
                }
            }
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            link: '=',
            labels: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '='
        }
    };
}
