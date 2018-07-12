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
import EntityGroupRoutes from './entity-group.routes';
import {EntityGroupsController, EntityGroupCardController} from './entity-groups.controller';
import EntityGroupController from './entity-group.controller';
import EntityGroupDirective from './entity-group.directive';
import EntityGroupSettings from './entity-group-settings.directive';
import EntityGroupAutocompleteDirective from './entity-group-autocomplete.directive';
import EntityGroupListDirective from './entity-group-list.directive';
import EntityGroupColumn from './entity-group-column.directive';
import EntityGroupColumns from './entity-group-columns.directive';
import EntityDetailsSidenav from './entity-details-sidenav.directive';
import Entity from './entity.directive';

export default angular.module('thingsboard.entityGroup', [])
    .config(EntityGroupRoutes)
    .controller('EntityGroupsController', EntityGroupsController)
    .controller('EntityGroupCardController', EntityGroupCardController)
    .controller('EntityGroupController', EntityGroupController)
    .directive('tbEntityGroup', EntityGroupDirective)
    .directive('tbEntityGroupSettings', EntityGroupSettings)
    .directive('tbEntityGroupAutocomplete', EntityGroupAutocompleteDirective)
    .directive('tbEntityGroupList', EntityGroupListDirective)
    .directive('tbEntityGroupColumn', EntityGroupColumn)
    .directive('tbEntityGroupColumns', EntityGroupColumns)
    .directive('tbEntityDetailsSidenav', EntityDetailsSidenav)
    .directive('tbEntity', Entity)
    .name;
