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
/* eslint-disable import/no-unresolved, import/default */

import entityFilterViewTemplate from './entity-filter-view.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-filter-view.scss';

/*@ngInject*/
export default function EntityFilterViewDirective($compile, $templateCache, $q, $document, $mdDialog, $translate, types/*, entityService*/) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityFilterViewTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;
        scope.filterDisplayValue = '';

        scope.$watch('filter', function () {
            scope.updateDisplayValue();
        });

        scope.updateDisplayValue = function() {
            if (scope.filter && scope.filter.type) {
                var entityType;
                var prefix;
                switch (scope.filter.type) {
                    case types.aliasFilterType.singleEntity.value:
                        entityType = scope.filter.singleEntity.entityType;
                        scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].list, {count: 1}, 'messageformat');
                        break;
                    case types.aliasFilterType.entityGroup.value:
                        if (scope.filter.groupStateEntity) {
                            scope.filterDisplayValue = $translate.instant('alias.entities-of-group-state-entity');
                        } else {
                            entityType = scope.filter.groupType;
                            scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].group);
                        }
                        break;
                    case types.aliasFilterType.entityList.value:
                        entityType = scope.filter.entityType;
                        var count = scope.filter.entityList.length;
                        scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].list, {count: count}, 'messageformat');
                        break;
                    case types.aliasFilterType.entityName.value:
                        entityType = scope.filter.entityType;
                        prefix = scope.filter.entityNameFilter;
                        scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].nameStartsWith, {prefix: prefix});
                        break;
                    case types.aliasFilterType.entityGroupList.value:
                        entityType = scope.filter.groupType;
                        count = scope.filter.entityGroupList.length;
                        scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].groupList, {count: count}, 'messageformat');
                        break;
                    case types.aliasFilterType.entityGroupName.value:
                        entityType = scope.filter.groupType;
                        prefix = scope.filter.entityGroupNameFilter;
                        scope.filterDisplayValue = $translate.instant(types.entityTypeTranslations[entityType].groupNameStartsWith, {prefix: prefix});
                        break;
                    case types.aliasFilterType.stateEntity.value:
                        scope.filterDisplayValue = $translate.instant('alias.filter-type-state-entity-description');
                        break;
                    case types.aliasFilterType.assetType.value:
                        var assetType = scope.filter.assetType;
                        prefix = scope.filter.assetNameFilter;
                        if (prefix && prefix.length) {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-asset-type-and-name-description', {assetType: assetType, prefix: prefix});
                        } else {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-asset-type-description', {assetType: assetType});
                        }
                        break;
                    case types.aliasFilterType.deviceType.value:
                        var deviceType = scope.filter.deviceType;
                        prefix = scope.filter.deviceNameFilter;
                        if (prefix && prefix.length) {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-device-type-and-name-description', {deviceType: deviceType, prefix: prefix});
                        } else {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-device-type-description', {deviceType: deviceType});
                        }
                        break;
                    case types.aliasFilterType.entityViewType.value:
                        var entityViewType = scope.filter.entityViewType;
                        prefix = scope.filter.entityViewNameFilter;
                        if (prefix && prefix.length) {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-entity-view-type-and-name-description', {entityViewType: entityViewType, prefix: prefix});
                        } else {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-entity-view-type-description', {entityViewType: entityViewType});
                        }
                        break;
                    case types.aliasFilterType.relationsQuery.value:
                        var rootEntityText;
                        var directionText;
                        var allEntitiesText = $translate.instant('alias.all-entities');
                        var anyRelationText = $translate.instant('alias.any-relation');
                        if (scope.filter.rootStateEntity) {
                            rootEntityText = $translate.instant('alias.state-entity');
                        } else {
                            rootEntityText = $translate.instant(types.entityTypeTranslations[scope.filter.rootEntity.entityType].type);
                        }
                        directionText = $translate.instant('relation.direction-type.' + scope.filter.direction);
                        var relationFilters = scope.filter.filters;
                        if (relationFilters && relationFilters.length) {
                            var relationFiltersDisplayValues = [];
                            relationFilters.forEach(function(relationFilter) {
                                var entitiesText;
                                if (relationFilter.entityTypes && relationFilter.entityTypes.length) {
                                    var entitiesNamesList = [];
                                    relationFilter.entityTypes.forEach(function(entityType) {
                                        entitiesNamesList.push(
                                            $translate.instant(types.entityTypeTranslations[entityType].typePlural)
                                        );
                                    });
                                    entitiesText = entitiesNamesList.join(', ');
                                } else {
                                    entitiesText = allEntitiesText;
                                }
                                var relationTypeText;
                                if (relationFilter.relationType && relationFilter.relationType.length) {
                                    relationTypeText = "'" + relationFilter.relationType + "'";
                                } else {
                                    relationTypeText = anyRelationText;
                                }
                                var relationFilterDisplayValue = $translate.instant('alias.filter-type-relations-query-description',
                                    {
                                        entities: entitiesText,
                                        relationType: relationTypeText,
                                        direction: directionText,
                                        rootEntity: rootEntityText
                                    }
                                );
                                relationFiltersDisplayValues.push(relationFilterDisplayValue);
                            });
                            scope.filterDisplayValue = relationFiltersDisplayValues.join(', ');
                        } else {
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-relations-query-description',
                                {
                                    entities: allEntitiesText,
                                    relationType: anyRelationText,
                                    direction: directionText,
                                    rootEntity: rootEntityText
                                }
                            );
                        }
                        break;
                    case types.aliasFilterType.assetSearchQuery.value:
                    case types.aliasFilterType.deviceSearchQuery.value:
                    case types.aliasFilterType.entityViewSearchQuery.value:
                        allEntitiesText = $translate.instant('alias.all-entities');
                        anyRelationText = $translate.instant('alias.any-relation');
                        if (scope.filter.rootStateEntity) {
                            rootEntityText = $translate.instant('alias.state-entity');
                        } else {
                            rootEntityText = $translate.instant(types.entityTypeTranslations[scope.filter.rootEntity.entityType].type);
                        }
                        directionText = $translate.instant('relation.direction-type.' + scope.filter.direction);
                        var relationTypeText;
                        if (scope.filter.relationType && scope.filter.relationType.length) {
                            relationTypeText = "'" + scope.filter.relationType + "'";
                        } else {
                            relationTypeText = anyRelationText;
                        }

                        var translationValues = {
                            relationType: relationTypeText,
                            direction: directionText,
                            rootEntity: rootEntityText
                        }

                        if (scope.filter.type == types.aliasFilterType.assetSearchQuery.value) {
                            var assetTypesQuoted = [];
                            scope.filter.assetTypes.forEach(function(assetType) {
                                assetTypesQuoted.push("'"+assetType+"'");
                            });
                            var assetTypesText = assetTypesQuoted.join(', ');
                            translationValues.assetTypes = assetTypesText;
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-asset-search-query-description',
                                translationValues
                            );
                        } else if (scope.filter.type == types.aliasFilterType.deviceSearchQuery.value) {
                            var deviceTypesQuoted = [];
                            scope.filter.deviceTypes.forEach(function(deviceType) {
                                deviceTypesQuoted.push("'"+deviceType+"'");
                            });
                            var deviceTypesText = deviceTypesQuoted.join(', ');
                            translationValues.deviceTypes = deviceTypesText;
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-device-search-query-description',
                                translationValues
                            );
                        } else if (scope.filter.type == types.aliasFilterType.entityViewSearchQuery.value) {
                            var entityViewTypesQuoted = [];
                            scope.filter.entityViewTypes.forEach(function(entityViewType) {
                                entityViewTypesQuoted.push("'"+entityViewType+"'");
                            });
                            var entityViewTypesText = entityViewTypesQuoted.join(', ');
                            translationValues.entityViewTypes = entityViewTypesText;
                            scope.filterDisplayValue = $translate.instant('alias.filter-type-entity-view-search-query-description',
                                translationValues
                            );
                        }
                        break;
                    default:
                        scope.filterDisplayValue = scope.filter.type;
                        break;
                }
            } else {
                scope.filterDisplayValue = '';
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.filter = ngModelCtrl.$viewValue;
            } else {
                scope.filter = null;
            }
        }

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: true
    };

}
