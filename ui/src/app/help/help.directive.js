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
import './help.scss';

import thingsboardHelpLinks from './help-links.constant';

import $ from 'jquery';

export default angular.module('thingsboard.directives.help', [thingsboardHelpLinks])
    .directive('tbHelp', Help)
    .name;

/* eslint-disable angular/angularelement */

/*@ngInject*/
function Help($compile, $window, helpLinks, whiteLabelingService) {

    var linker = function (scope, element, attrs) {

        scope.gotoHelpPage = function ($event) {
            if ($event) {
                $event.stopPropagation();
            }
            var helpUrl = helpLinks.linksMap[scope.helpLinkId];
            if (!helpUrl && scope.helpLinkId &&
                    (scope.helpLinkId.startsWith('http://') || scope.helpLinkId.startsWith('https://'))) {
                helpUrl = scope.helpLinkId;
            }
            if (helpUrl) {
                var baseUrl =  whiteLabelingService.getHelpLinkBaseUrl();
                if (baseUrl) {
                    helpUrl = helpUrl.replace("https://thingsboard.io", baseUrl);
                }
                $window.open(helpUrl, '_blank');
            }
        }

        if (whiteLabelingService.isEnableHelpLinks()) {
            var html = '<md-tooltip md-direction="top">' +
                '{{\'help.goto-help-page\' | translate}}' +
                '</md-tooltip>' +
                '<md-icon class="material-icons">' +
                'help' +
                '</md-icon>';

            var helpButton = angular.element('<md-button class="tb-help-button-style tb-help-button-pos md-icon-button" ' +
                'ng-click="gotoHelpPage($event)">' +
                html +
                '</md-button>');

            if (attrs.helpContainerId) {
                var helpContainer = $('#' + attrs.helpContainerId, element)[0];
                helpContainer = angular.element(helpContainer);
                helpContainer.append(helpButton);
                $compile(helpContainer.contents())(scope);
            } else {
                $compile(helpButton)(scope);
                element.append(helpButton);
            }
        }
    }

    return {
        restrict: "A",
        link: linker,
        scope: {
            helpLinkId: "=tbHelp"
        }
    };
}

/* eslint-enable angular/angularelement */
