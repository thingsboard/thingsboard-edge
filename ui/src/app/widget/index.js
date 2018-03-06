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
import './widget-editor.scss';

import 'angular-hotkeys';
import 'angular-ui-ace';

import uiRouter from 'angular-ui-router';
import thingsboardApiUser from '../api/user.service';
import thingsboardApiWidget from '../api/widget.service';
import thingsboardTypes from '../common/types.constant';
import thingsboardToast from '../services/toast';
import thingsboardConfirmOnExit from '../components/confirm-on-exit.directive';
import thingsboardDashboard from '../components/dashboard.directive';
import thingsboardExpandFullscreen from '../components/expand-fullscreen.directive';
import thingsboardCircularProgress from '../components/circular-progress.directive';

import WidgetLibraryRoutes from './widget-library.routes';
import WidgetLibraryController from './widget-library.controller';
import SelectWidgetTypeController from './select-widget-type.controller';
import WidgetEditorController from './widget-editor.controller';
import WidgetsBundleController from './widgets-bundle.controller';
import WidgetsBundleDirective from './widgets-bundle.directive';
import SaveWidgetTypeAsController from './save-widget-type-as.controller';

export default angular.module('thingsboard.widget-library', [
    uiRouter,
    thingsboardApiWidget,
    thingsboardApiUser,
    thingsboardTypes,
    thingsboardToast,
    thingsboardConfirmOnExit,
    thingsboardDashboard,
    thingsboardExpandFullscreen,
    thingsboardCircularProgress,
    'cfp.hotkeys',
    'ui.ace'
])
    .config(WidgetLibraryRoutes)
    .controller('WidgetLibraryController', WidgetLibraryController)
    .controller('SelectWidgetTypeController', SelectWidgetTypeController)
    .controller('WidgetEditorController', WidgetEditorController)
    .controller('WidgetsBundleController', WidgetsBundleController)
    .controller('SaveWidgetTypeAsController', SaveWidgetTypeAsController)
    .directive('tbWidgetsBundle', WidgetsBundleDirective)
    .name;
