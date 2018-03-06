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
import './widget.scss';

import thingsboardLegend from '../legend.directive';
import thingsboardTypes from '../../common/types.constant';
import thingsboardApiDatasource from '../../api/datasource.service';

import WidgetController from './widget.controller';

export default angular.module('thingsboard.directives.widget', [thingsboardLegend, thingsboardTypes, thingsboardApiDatasource])
    .controller('WidgetController', WidgetController)
    .directive('tbWidget', Widget)
    .name;

/*@ngInject*/
function Widget($controller, widgetService) {
    return {
        scope: true,
        link: function (scope, elem, attrs) {

            var locals = scope.$eval(attrs.locals);
            var widget = locals.widget;

            var widgetController;
            var gridsterItem;

            //TODO: widgets visibility
            /*scope.$on('visibleRectChanged', function (event, newVisibleRect) {
                locals.visibleRect = newVisibleRect;
                if (widgetController) {
                    widgetController.visibleRectChanged(newVisibleRect);
                }
            });*/

            scope.$on('gridster-item-initialized', function (event, item) {
                gridsterItem = item;
                if (widgetController) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            });

            //TODO:
            //elem.html('<div id="progress-cover" flex layout="column" layout-align="center center" style="height: 100%;">' +
            //          '     <md-progress-circular md-mode="indeterminate" class="md-accent md-hue-2" md-diameter="120"></md-progress-circular>' +
            //          '</div>');

            //var progressElement = angular.element(elem[0].querySelector('#progress-cover'));
            //var progressScope = scope.$new();
            //$compile(elem.contents())(progressScope);

            widgetService.getWidgetInfo(widget.bundleAlias, widget.typeAlias, widget.isSystemType).then(
                function(widgetInfo) {
                    loadFromWidgetInfo(widgetInfo);
                }
            );

            function loadFromWidgetInfo(widgetInfo) {

                scope.loadingData = true;

                elem.addClass("tb-widget");

                var widgetNamespace = "widget-type-" + (widget.isSystemType ? 'sys-' : '')
                    + widget.bundleAlias + '-'
                    + widget.typeAlias;

                elem.addClass(widgetNamespace);

                var widgetType = widgetService.getWidgetTypeFunction(widget.bundleAlias, widget.typeAlias, widget.isSystemType);

                angular.extend(locals, {$scope: scope, $element: elem, widgetInfo: widgetInfo, widgetType: widgetType});

                widgetController = $controller('WidgetController', locals);

                if (gridsterItem) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            }
        }
    };
}
