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

import opcUaSubscriptionTagsTemplate from './opc-ua-subscription-tags.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './opc-ua-subscription-tags.scss';

/*@ngInject*/
export default function OpcUaSubscriptionTagsDirective($compile, $templateCache, $translate, $mdExpansionPanel, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(opcUaSubscriptionTagsTemplate);
        element.html(template);

        scope.types = types;
        scope.$mdExpansionPanel = $mdExpansionPanel;

        scope.$watch('subscriptionTags', function (newConfiguration, oldConfiguration) {
            if (!angular.equals(newConfiguration, oldConfiguration)) {
                ngModelCtrl.$setViewValue(scope.subscriptionTags);
            }
        }, true);

        ngModelCtrl.$render = function () {
            scope.subscriptionTags = ngModelCtrl.$viewValue;
            scope.updateValidity();
        };

        scope.addSubscriptionTag = () => {
            if (!scope.subscriptionTags) {
                scope.subscriptionTags = [];
            }
            scope.subscriptionTags.push(
                {
                    key: '',
                    path: '',
                    required: false
                }
            );
            ngModelCtrl.$setDirty();
            scope.updateValidity();
        };

        scope.removeSubscriptionTag = (index) => {
            if (index > -1) {
                scope.subscriptionTags.splice(index, 1);
                ngModelCtrl.$setDirty();
                scope.updateValidity();
            }
        };

        scope.updateValidity = () => {
            var subscriptionTagsValid = true;
            if (!scope.subscriptionTags || !scope.subscriptionTags.length) {
                subscriptionTagsValid = false;
            }
            ngModelCtrl.$setValidity('SubscriptionTags', subscriptionTagsValid);
        };

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            isEdit: '='
        },
        link: linker
    };
}
