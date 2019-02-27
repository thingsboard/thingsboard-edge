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
import EntityAliasesController from './alias/entity-aliases.controller';
import EntityAliasDialogController from './alias/entity-alias-dialog.controller';
import EntityTypeSelectDirective from './entity-type-select.directive';
import EntityTypeListDirective from './entity-type-list.directive';
import EntitySubtypeListDirective from './entity-subtype-list.directive';
import EntitySubtypeSelectDirective from './entity-subtype-select.directive';
import EntitySubtypeAutocompleteDirective from './entity-subtype-autocomplete.directive';
import EntityAutocompleteDirective from './entity-autocomplete.directive';
import EntityListDirective from './entity-list.directive';
import EntitySelectDirective from './entity-select.directive';
import EntityFilterDirective from './entity-filter.directive';
import EntityFilterViewDirective from './entity-filter-view.directive';
import AliasesEntitySelectPanelController from './alias/aliases-entity-select-panel.controller';
import AliasesEntitySelectDirective from './alias/aliases-entity-select.directive';
import AddAttributeDialogController from './attribute/add-attribute-dialog.controller';
import AddWidgetToDashboardDialogController from './attribute/add-widget-to-dashboard-dialog.controller';
import AttributeTableDirective from './attribute/attribute-table.directive';
import RelationFiltersDirective from './relation/relation-filters.directive';
import RelationTableDirective from './relation/relation-table.directive';
import RelationTypeAutocompleteDirective from './relation/relation-type-autocomplete.directive';

export default angular.module('thingsboard.entity', [])
    .controller('EntityAliasesController', EntityAliasesController)
    .controller('EntityAliasDialogController', EntityAliasDialogController)
    .controller('AliasesEntitySelectPanelController', AliasesEntitySelectPanelController)
    .controller('AddAttributeDialogController', AddAttributeDialogController)
    .controller('AddWidgetToDashboardDialogController', AddWidgetToDashboardDialogController)
    .directive('tbEntityTypeSelect', EntityTypeSelectDirective)
    .directive('tbEntityTypeList', EntityTypeListDirective)
    .directive('tbEntitySubtypeList', EntitySubtypeListDirective)
    .directive('tbEntitySubtypeSelect', EntitySubtypeSelectDirective)
    .directive('tbEntitySubtypeAutocomplete', EntitySubtypeAutocompleteDirective)
    .directive('tbEntityAutocomplete', EntityAutocompleteDirective)
    .directive('tbEntityList', EntityListDirective)
    .directive('tbEntitySelect', EntitySelectDirective)
    .directive('tbEntityFilter', EntityFilterDirective)
    .directive('tbEntityFilterView', EntityFilterViewDirective)
    .directive('tbAliasesEntitySelect', AliasesEntitySelectDirective)
    .directive('tbAttributeTable', AttributeTableDirective)
    .directive('tbRelationFilters', RelationFiltersDirective)
    .directive('tbRelationTable', RelationTableDirective)
    .directive('tbRelationTypeAutocomplete', RelationTypeAutocompleteDirective)
    .name;
