/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import entityAliasDialogTemplate from '../entity/alias/entity-alias-dialog.tpl.html';
import editWidgetTemplate from './edit-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EditWidgetDirective($compile, $templateCache, types, widgetService, entityService, $q, $document, $mdDialog) {

    var linker = function (scope, element) {
        var template = $templateCache.get(editWidgetTemplate);
        element.html(template);

        scope.$watch('widget', function () {
            if (scope.widget) {
                widgetService.getWidgetInfo(scope.widget.bundleAlias,
                    scope.widget.typeAlias,
                    scope.widget.isSystemType).then(
                    function(widgetInfo) {
                        scope.$applyAsync(function(scope) {
                            scope.widgetConfig = {
                                config: scope.widget.config,
                                layout: scope.widgetLayout
                            };
                            var settingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
                            var dataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
                            scope.typeParameters = widgetInfo.typeParameters;
                            scope.actionSources = widgetInfo.actionSources;
                            scope.isDataEnabled = !widgetInfo.typeParameters.useCustomDatasources;
                            if (!settingsSchema || settingsSchema === '') {
                                scope.settingsSchema = {};
                            } else {
                                scope.settingsSchema = angular.fromJson(settingsSchema);
                            }
                            if (!dataKeySettingsSchema || dataKeySettingsSchema === '') {
                                scope.dataKeySettingsSchema = {};
                            } else {
                                scope.dataKeySettingsSchema = angular.fromJson(dataKeySettingsSchema);
                            }

                            scope.functionsOnly = scope.dashboard ? false : true;

                            scope.theForm.$setPristine();
                        });
                    }
                );
            }
        });

        scope.$watch('widgetLayout', function () {
            if (scope.widgetLayout && scope.widgetConfig) {
                scope.widgetConfig.layout = scope.widgetLayout;
            }
        });

        scope.fetchEntityKeys = function (entityAliasId, query, type) {
            var deferred = $q.defer();
            scope.aliasController.getAliasInfo(entityAliasId).then(
                function success(aliasInfo) {
                    var entity = aliasInfo.currentEntity;
                    if (entity) {
                        entityService.getEntityKeys(entity.entityType, entity.id, query, type, {ignoreLoading: true}).then(
                            function success(keys) {
                                deferred.resolve(keys);
                            },
                            function fail() {
                                deferred.resolve([]);
                            }
                        );
                    } else {
                        deferred.resolve([]);
                    }
                },
                function fail() {
                    deferred.resolve([]);
                }
            );
            return deferred.promise;
        };

        scope.fetchDashboardStates = function(query) {
            var deferred = $q.defer();
            var stateIds = Object.keys(scope.dashboard.configuration.states);
            var result = query ? stateIds.filter(
                createFilterForDashboardState(query)) : stateIds;
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([query]);
            }
            return deferred.promise;
        }

        function createFilterForDashboardState (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(stateId) {
                return (angular.lowercase(stateId).indexOf(lowercaseQuery) === 0);
            };
        }

        scope.createEntityAlias = function (event, alias, allowedEntityTypes) {

            var deferred = $q.defer();
            var singleEntityAlias = {id: null, alias: alias, filter: {}};

            $mdDialog.show({
                controller: 'EntityAliasDialogController',
                controllerAs: 'vm',
                templateUrl: entityAliasDialogTemplate,
                locals: {
                    isAdd: true,
                    allowedEntityTypes: allowedEntityTypes,
                    entityAliases: scope.dashboard.configuration.entityAliases,
                    alias: singleEntityAlias
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                skipHide: true,
                targetEvent: event
            }).then(function (singleEntityAlias) {
                scope.dashboard.configuration.entityAliases[singleEntityAlias.id] = singleEntityAlias;
                scope.aliasController.updateEntityAliases(scope.dashboard.configuration.entityAliases);
                deferred.resolve(singleEntityAlias);
            }, function () {
                deferred.reject();
            });

            return deferred.promise;
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            dashboard: '=',
            aliasController: '=',
            widgetEditMode: '=',
            widget: '=',
            widgetLayout: '=',
            theForm: '='
        }
    };
}
