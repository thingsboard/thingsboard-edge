/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import $ from 'jquery';

export default angular.module('thingsboard.directives.circularProgress', [])
    .directive('tbCircularProgress', CircularProgress)
    .name;

/* eslint-disable angular/angularelement */

/*@ngInject*/
function CircularProgress($compile) {

    var linker = function (scope, element) {

        var circularProgressElement = angular.element('<md-progress-circular style="margin: auto;" md-mode="indeterminate" md-diameter="20"></md-progress-circular>');

        $compile(circularProgressElement)(scope);

        var children = null;
        var cssWidth = element.prop('style')['width'];
        var width = null;
        if (!cssWidth) {
            $(element).css('width', width + 'px');
        }

        scope.$watch('circularProgress', function (newCircularProgress, prevCircularProgress) {
            if (newCircularProgress != prevCircularProgress) {
                if (newCircularProgress) {
                    if (!cssWidth) {
                        $(element).css('width', '');
                        width = element.prop('offsetWidth');
                        $(element).css('width', width + 'px');
                    }
                    children = $(element).children();
                    $(element).empty();
                    $(element).append($(circularProgressElement));
                } else {
                    $(element).empty();
                    $(element).append(children);
                    if (cssWidth) {
                        $(element).css('width', cssWidth);
                    } else {
                        $(element).css('width', '');
                    }
                }
            }
        });

    }

    return {
        restrict: "A",
        link: linker,
        scope: {
            circularProgress: "=tbCircularProgress"
        }
    };
}

/* eslint-enable angular/angularelement */
