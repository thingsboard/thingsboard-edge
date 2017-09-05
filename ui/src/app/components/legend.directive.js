/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
import './legend.scss';

/* eslint-disable import/no-unresolved, import/default */

import legendTemplate from './legend.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.legend', [])
    .directive('tbLegend', Legend)
    .name;

/*@ngInject*/
function Legend($compile, $templateCache, types) {

    var linker = function (scope, element) {
        var template = $templateCache.get(legendTemplate);
        element.html(template);

        scope.displayHeader = function() {
            return scope.legendConfig.showMin === true ||
                   scope.legendConfig.showMax === true ||
                   scope.legendConfig.showAvg === true ||
                   scope.legendConfig.showTotal === true;
        }

        scope.isHorizontal = scope.legendConfig.position === types.position.bottom.value ||
            scope.legendConfig.position === types.position.top.value;

        scope.toggleHideData = function(index) {
            scope.legendData.keys[index].dataKey.hidden = !scope.legendData.keys[index].dataKey.hidden;
        }

        $compile(element.contents())(scope);

    }

    /*    scope.legendData = {
     keys: [],
     data: []

     key: {
       dataKey: dataKey,
       dataIndex: 0
     }
     data: {
       min: null,
       max: null,
       avg: null,
       total: null
     }
     };*/

    return {
        restrict: "E",
        link: linker,
        scope: {
            legendConfig: '=',
            legendData: '='
        }
    };
}
