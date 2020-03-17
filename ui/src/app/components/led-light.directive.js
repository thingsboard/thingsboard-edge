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
import Raphael from 'raphael';
import tinycolor from 'tinycolor2';
import $ from 'jquery';

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.ledLight', [])
    .directive('tbLedLight', LedLight).name;

/*@ngInject*/
function LedLight($compile) {

    var linker = function (scope, element) {
        scope.offOpacity = scope.offOpacity || "0.4";
        scope.glowColor = tinycolor(scope.colorOn).lighten().toHexString();

        scope.$watch('tbEnabled',function() {
            scope.draw();
        });

        scope.$watch('size',function() {
            scope.update();
        });

        scope.draw = function () {
            if (scope.tbEnabled) {
                scope.circleElement.attr("fill", scope.colorOn);
                scope.circleElement.attr("stroke", scope.colorOn);
                scope.circleElement.attr("opacity", "1");

                if (scope.circleElement.theGlow) {
                    scope.circleElement.theGlow.remove();
                }

                scope.circleElement.theGlow = scope.circleElement.glow(
                    {
                        color: scope.glowColor,
                        width: scope.radius + scope.glowSize,
                        opacity: 0.8,
                        fill: true
                    });
            } else {
                if (scope.circleElement.theGlow) {
                    scope.circleElement.theGlow.remove();
                }

                /*scope.circleElement.theGlow = scope.circleElement.glow(
                 {
                 color: scope.glowColor,
                 width: scope.radius + scope.glowSize,
                 opacity: 0.4,
                 fill: true
                 });*/

                scope.circleElement.attr("fill", scope.colorOff);
                scope.circleElement.attr("stroke", scope.colorOff);
                scope.circleElement.attr("opacity", scope.offOpacity);
            }
        }

        scope.update = function() {
            scope.size = scope.size || 50;
            scope.canvasSize = scope.size;
            scope.radius = scope.canvasSize / 4;
            scope.glowSize = scope.radius / 5;

            var template = '<div id="canvas_container" style="width: ' + scope.size + 'px; height: ' + scope.size + 'px;"></div>';
            element.html(template);
            $compile(element.contents())(scope);
            scope.paper = new Raphael($('#canvas_container', element)[0], scope.canvasSize, scope.canvasSize);
            var center = scope.canvasSize / 2;
            scope.circleElement = scope.paper.circle(center, center, scope.radius);
            scope.draw();
        }

        scope.update();
    }


    return {
        restrict: "E",
        link: linker,
        scope: {
            size: '=?',
            colorOn: '=',
            colorOff: '=',
            offOpacity: '=?',
            //glowColor: '=',
            tbEnabled: '='
        }
    };

}

/* eslint-enable angular/angularelement */
