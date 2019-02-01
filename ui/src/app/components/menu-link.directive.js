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
import './menu-link.scss';

import thingsboardMenu from '../services/menu.service';

/* eslint-disable import/no-unresolved, import/default */

import menulinkTemplate from './menu-link.tpl.html';
import menutoggleTemplate from './menu-toggle.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.menuLink', [thingsboardMenu])
    .directive('tbMenuLink', MenuLink)
    .filter('nospace', NoSpace)
    .name;

/*@ngInject*/
function MenuLink($compile, $templateCache, menu, utils) {

    var linker = function (scope, element) {
        var template;

        scope.utils = utils;

        if (scope.section.type === 'link') {
            template = $templateCache.get(menulinkTemplate);
        } else {
            template = $templateCache.get(menutoggleTemplate);

            var parentNode = element[0].parentNode.parentNode.parentNode;
            if (parentNode.classList.contains('parent-list-item')) {
                var heading = parentNode.querySelector('h2');
                element[0].firstChild.setAttribute('aria-describedby', heading.id);
            }

            scope.sectionActive = function () {
                return menu.sectionActive(scope.section);
            };

            scope.sectionHeight = function () {
                return menu.sectionHeight(scope.section);
            };
        }

        element.html(template);

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            section: '='
        }
    };
}

function NoSpace() {
    return function (value) {
        return (!value) ? '' : value.replace(/ /g, '');
    }
}
