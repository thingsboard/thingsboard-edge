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
const SPECIAL_CHARS_REGEXP = /([\:\-\_]+(.))/g;
const MOZ_HACK_REGEXP = /^moz([A-Z])/;
const PREFIX_REGEXP = /^((?:x|data)[\:\-_])/i;

var tbEventDirectives = {};

angular.forEach(
    'click dblclick mousedown mouseup mouseover mouseout mousemove mouseenter mouseleave contextmenu keydown keyup keypress submit focus blur copy cut paste'.split(' '),
    function(eventName) {
        var directiveName = directiveNormalize('tb-' + eventName);
        tbEventDirectives[directiveName] = ['$parse', '$rootScope', function($parse) {
            return {
                restrict: 'A',
                compile: function($element, attr) {
                    var fn = $parse(attr[directiveName], /* interceptorFn */ null, /* expensiveChecks */ true);
                    return function ngEventHandler(scope, element) {
                        element.on(eventName, function(event) {
                            var callback = function() {
                                fn(scope, {$event:event});
                            };
                            callback();
                        });
                    };
                }
            };
        }];
    }
);

export default angular.module('thingsboard.directives.event', [])
    .directive(tbEventDirectives)
    .name;

function camelCase(name) {
    return name.
    replace(SPECIAL_CHARS_REGEXP, function(_, separator, letter, offset) {
        return offset ? letter.toUpperCase() : letter;
    }).
    replace(MOZ_HACK_REGEXP, 'Moz$1');
}

function directiveNormalize(name) {
    return camelCase(name.replace(PREFIX_REGEXP, ''));
}