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
import './aliases-entity-select.scss';

import $ from 'jquery';

/* eslint-disable import/no-unresolved, import/default */

import aliasesEntitySelectButtonTemplate from './aliases-entity-select-button.tpl.html';
import aliasesEntitySelectPanelTemplate from './aliases-entity-select-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */
/*@ngInject*/
export default function AliasesEntitySelectDirective($compile, $templateCache, $mdMedia, types, $mdPanel, $document, $translate) {

    var linker = function (scope, element, attrs) {

        /* tbAliasesEntitySelect (ng-model)
         * {
         *    "aliasId": {
         *        alias: alias,
         *        entityType: entityType,
         *        entityId: entityId
         *    }
         * }
         */

        var template = $templateCache.get(aliasesEntitySelectButtonTemplate);

        scope.tooltipDirection = angular.isDefined(attrs.tooltipDirection) ? attrs.tooltipDirection : 'top';

        element.html(template);

        scope.openEditMode = function (event) {
            if (scope.disabled) {
                return;
            }
            var position;
            var panelHeight = $mdMedia('min-height: 350px') ? 250 : 150;
            var panelWidth = 300;
            var offset = element[0].getBoundingClientRect();
            var bottomY = offset.bottom - $(window).scrollTop(); //eslint-disable-line
            var leftX = offset.left - $(window).scrollLeft(); //eslint-disable-line
            var yPosition;
            var xPosition;
            if (bottomY + panelHeight > $( window ).height()) { //eslint-disable-line
                yPosition = $mdPanel.yPosition.ABOVE;
            } else {
                yPosition = $mdPanel.yPosition.BELOW;
            }
            if (leftX + panelWidth > $( window ).width()) { //eslint-disable-line
                xPosition = $mdPanel.xPosition.CENTER;
            } else {
                xPosition = $mdPanel.xPosition.ALIGN_START;
            }
            position = $mdPanel.newPanelPosition()
                .relativeTo(element)
                .addPanelPosition(xPosition, yPosition);
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'AliasesEntitySelectPanelController',
                controllerAs: 'vm',
                templateUrl: aliasesEntitySelectPanelTemplate,
                panelClass: 'tb-aliases-entity-select-panel',
                position: position,
                fullscreen: false,
                locals: {
                    'aliasController': scope.aliasController,
                    'onEntityAliasesUpdate': function () {
                        scope.updateView();
                    }
                },
                openFrom: event,
                clickOutsideToClose: true,
                escapeToClose: true,
                focusOnOpen: false
            };
            $mdPanel.open(config);
        }

        scope.$on('entityAliasesChanged', function() {
            scope.updateView();
        });

        scope.$on('entityAliasResolved', function() {
            scope.updateView();
        });

        scope.updateView = function () {
            updateDisplayValue();
        }

        function updateDisplayValue() {
            var displayValue;
            var singleValue = true;
            var currentAliasId;
            var entityAliases = scope.aliasController.getEntityAliases();
            for (var aliasId in entityAliases) {
                var entityAlias = entityAliases[aliasId];
                if (!entityAlias.filter.resolveMultiple) {
                    var resolvedAlias = scope.aliasController.getInstantAliasInfo(aliasId);
                    if (resolvedAlias && resolvedAlias.currentEntity) {
                        if (!currentAliasId) {
                            currentAliasId = aliasId;
                        } else {
                            singleValue = false;
                            break;
                        }
                    }
                }
            }
            if (singleValue && currentAliasId) {
                var aliasInfo = scope.aliasController.getInstantAliasInfo(currentAliasId);
                displayValue = aliasInfo.currentEntity.name;
            } else {
                displayValue = $translate.instant('entity.entities');
            }
            scope.displayValue = displayValue;
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        scope: {
            aliasController:'='
        },
        link: linker
    };

}

/* eslint-enable angular/angularelement */