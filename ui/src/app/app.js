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
import './ie.support';

import 'event-source-polyfill';

import angular from 'angular';
import ngMaterial from 'angular-material';
import ngMdIcons from 'angular-material-icons';
import ngCookies from 'angular-cookies';
import angularSocialshare from 'angular-socialshare';
import 'angular-translate';
import 'angular-translate-loader-static-files';
import 'angular-translate-storage-local';
import 'angular-translate-storage-cookie';
import 'angular-translate-handler-log';
import 'angular-translate-interpolation-messageformat';
import 'md-color-picker';
import mdPickers from 'mdPickers';
import ngSanitize from 'angular-sanitize';
import vAccordion from 'v-accordion';
import ngAnimate from 'angular-animate';
import 'angular-websocket';
import uiRouter from 'angular-ui-router';
import angularJwt from 'angular-jwt';
import 'angular-drag-and-drop-lists';
import mdDataTable from 'angular-material-data-table';
import ngTouch from 'angular-touch';
import 'angular-carousel';
import 'clipboard';
import 'ngclipboard';
import 'react';
import 'react-dom';
import 'material-ui';
import 'react-schema-form';
import react from 'ngreact';
import '@flowjs/ng-flow/dist/ng-flow-standalone.min';

import thingsboardLocales from './locale/locale.constant';
import thingsboardLogin from './login';
import thingsboardDatakeyConfigDialog from './components/datakey-config-dialog.controller';
import thingsboardDialogs from './dialog';
import thingsboardMenu from './services/menu.service';
import thingsboardRaf from './common/raf.provider';
import thingsboardUtils from './common/utils.service';
import thingsboardDashboardUtils from './common/dashboard-utils.service';
import thingsboardTypes from './common/types.constant';
import thingsboardApiTime from './api/time.service';
import thingsboardKeyboardShortcut from './components/keyboard-shortcut.filter';
import thingsboardHelp from './help/help.directive';
import thingsboardToast from './services/toast';
import thingsboardHome from './layout';
import thingsboardApiLogin from './api/login.service';
import thingsboardApiDevice from './api/device.service';
import thingsboardApiUser from './api/user.service';
import thingsboardApiEntityRelation from './api/entity-relation.service';
import thingsboardApiAsset from './api/asset.service';
import thingsboardApiAttribute from './api/attribute.service';
import thingsboardApiEntity from './api/entity.service';
import thingsboardApiAlarm from './api/alarm.service';
import thingsboardApiEntityGroup from './api/entity-group.service';
import thingsboardApiWhiteLabeling from './api/white-labeling.service';

import 'typeface-roboto';
import 'font-awesome/css/font-awesome.min.css';
import 'angular-material/angular-material.min.css';
import 'angular-material-icons/angular-material-icons.css';
import 'angular-gridster/dist/angular-gridster.min.css';
import 'v-accordion/dist/v-accordion.min.css'
import 'md-color-picker/dist/mdColorPicker.min.css';
import 'mdPickers/dist/mdPickers.min.css';
import 'angular-hotkeys/build/hotkeys.min.css';
import 'angular-carousel/dist/angular-carousel.min.css';
import '../scss/main.scss';

import AppConfig from './app.config';
import GlobalInterceptor from './global-interceptor.service';
import AppRun from './app.run';

angular.module('thingsboard', [
    ngMaterial,
    ngMdIcons,
    ngCookies,
    angularSocialshare,
    'pascalprecht.translate',
    'mdColorPicker',
    mdPickers,
    ngSanitize,
    vAccordion,
    ngAnimate,
    'ngWebSocket',
    angularJwt,
    'dndLists',
    mdDataTable,
    ngTouch,
    'angular-carousel',
    'ngclipboard',
    react.name,
    'flow',
    thingsboardLocales,
    thingsboardLogin,
    thingsboardDatakeyConfigDialog,
    thingsboardDialogs,
    thingsboardMenu,
    thingsboardRaf,
    thingsboardUtils,
    thingsboardDashboardUtils,
    thingsboardTypes,
    thingsboardApiTime,
    thingsboardKeyboardShortcut,
    thingsboardHelp,
    thingsboardToast,
    thingsboardHome,
    thingsboardApiLogin,
    thingsboardApiDevice,
    thingsboardApiUser,
    thingsboardApiEntityRelation,
    thingsboardApiAsset,
    thingsboardApiAttribute,
    thingsboardApiEntity,
    thingsboardApiAlarm,
    thingsboardApiEntityGroup,
    thingsboardApiWhiteLabeling,
    uiRouter])
    .config(AppConfig)
    .factory('globalInterceptor', GlobalInterceptor)
    .run(AppRun);
