/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import EntityGroupRoutes from './entity-group.routes';
import {EntityGroupsController, EntityGroupCardController} from './entity-groups.controller';
import EntityGroupController from './entity-group.controller';
import EntityGroupDirective from './entity-group.directive';
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
    .directive('tbEntityGroupColumn', EntityGroupColumn)
    .directive('tbEntityGroupColumns', EntityGroupColumns)
    .directive('tbEntityDetailsSidenav', EntityDetailsSidenav)
    .directive('tbEntity', Entity)
    .name;
