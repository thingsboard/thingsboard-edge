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
import Flow from '@flowjs/ng-flow/dist/ng-flow-standalone.min';
import UrlHandler from './url.handler';

/*@ngInject*/
export default function AppRun($rootScope, $mdTheming, $window, $injector, $location, $log, $state, $mdDialog, $filter, store,
                               whiteLabelingService, loginService, userService, $translate) {

    $window.Flow = Flow;
    var frame = null;
    try {
        frame = $window.frameElement;
    } catch(e) {
        // ie11 fix
    }

    var unauthorizedDialog = null;
    var forbiddenDialog = null;

    $mdTheming.generateTheme('default');
    $mdTheming.generateTheme('tb-dark');

    $rootScope.iframeMode = false;

    var tbReportView = store.get('tb_report_view');
    if (tbReportView) {
        $rootScope.reportView = true;
    }

    var favicon = angular.element('link[rel="icon"]');

    if (frame) {
        $rootScope.iframeMode = true;
        var dataWidgetAttr = angular.element(frame).attr('data-widget');
        if (dataWidgetAttr) {
            $rootScope.editWidgetInfo = angular.fromJson(dataWidgetAttr);
            $rootScope.widgetEditMode = true;
        }
    }

    initWatchers();


    var skipStateChange = false;

    function initWatchers() {
        $rootScope.unauthenticatedHandle = $rootScope.$on('unauthenticated', function (event, doLogout) {
            if (doLogout) {
                gotoPublicModule('login');
            } else {
                UrlHandler($injector, $location);
            }
        });

        $rootScope.authenticatedHandle = $rootScope.$on('authenticated', function () {
            UrlHandler($injector, $location);
        });

        $rootScope.forbiddenHandle = $rootScope.$on('forbidden', function () {
            showForbiddenDialog();
        });

        $rootScope.stateChangeStartHandle = $rootScope.$on('$stateChangeStart', function (evt, to, params) {

            if (skipStateChange) {
                skipStateChange = false;
                return;
            }

            function waitForUserLoaded() {
                if ($rootScope.userLoadedHandle) {
                    $rootScope.userLoadedHandle();
                }
                $rootScope.userLoadedHandle = $rootScope.$on('userLoaded', function () {
                    $rootScope.userLoadedHandle();
                    $state.go(to.name, params);
                });
            }

            function reloadUserFromPublicId() {
                userService.setUserFromJwtToken(null, null, false);
                waitForUserLoaded();
                userService.reloadUser();
            }

            var locationSearch = $location.search();
            var publicId = locationSearch.publicId;
            var activateToken = locationSearch.activateToken;

            if (to.url === '/createPassword?activateToken' && activateToken && activateToken.length) {
                userService.setUserFromJwtToken(null, null, false);
            }

            if (userService.isUserLoaded() === true) {
                if (userService.isAuthenticated()) {
                    if (userService.isPublic()) {
                        if (userService.parsePublicId() !== publicId) {
                            evt.preventDefault();
                            if (publicId && publicId.length > 0) {
                                reloadUserFromPublicId();
                            } else {
                                userService.logout();
                            }
                            return;
                        }
                    }
                    if (userService.forceDefaultPlace(to, params)) {
                        evt.preventDefault();
                        gotoDefaultPlace(params);
                    } else {
                        var authority = userService.getAuthority();
                        if (to.module === 'public') {
                            evt.preventDefault();
                            gotoDefaultPlace(params);
                        } else if (angular.isDefined(to.auth) &&
                            to.auth.indexOf(authority) === -1) {
                            evt.preventDefault();
                            showForbiddenDialog();
                        } else if (to.redirectTo) {
                            evt.preventDefault();
                            var redirect;
                            if(angular.isObject(to.redirectTo)) {
                                redirect = to.redirectTo[authority];
                            } else {
                                redirect = to.redirectTo;
                            }
                            $state.go(redirect, params)
                        }
                    }
                } else {
                    if (publicId && publicId.length > 0) {
                        evt.preventDefault();
                        reloadUserFromPublicId();
                    } else if (to.module === 'private') {
                        evt.preventDefault();
                        if (to.url === '/home' || to.url === '/') {
                            gotoPublicModule('login', params);
                        } else {
                            showUnauthorizedDialog();
                        }
                    } else {
                        evt.preventDefault();
                        gotoPublicModule(to.name, params);
                    }
                }
            } else {
                evt.preventDefault();
                waitForUserLoaded();
            }
        });

        updateFavicon();
        updatePageTitle();

        $rootScope.stateChangeSuccessHandle = $rootScope.$on('$stateChangeSuccess', function (evt, to, params) {
            if (userService.isPublic() && to.name === 'home.dashboards.dashboard') {
                $location.search('publicId', userService.getPublicId());
                userService.updateLastPublicDashboardId(params.dashboardId);
            }
            updatePageTitle(to.data.pageTitle);
        });

        $rootScope.appTitleWhiteLabelingChangedHandle = $rootScope.$on('whiteLabelingChanged', () => {
            updateFavicon();
            var pageTitle = $state.current.data ? $state.current.data.pageTitle : '';
            updatePageTitle(pageTitle);
        });
    }

    function updateFavicon() {
        favicon.attr('type', whiteLabelingService.faviconType());
        favicon.attr('href', whiteLabelingService.faviconUrl());
    }

    function updatePageTitle(pageTitle) {
        var appTitle = whiteLabelingService.appTitle();
        if (angular.isDefined(pageTitle)) {
            $translate(pageTitle).then(
                (translation) => {
                    $rootScope.pageTitle = `${appTitle} | ${translation}`;
                }, (translationId) => {
                    $rootScope.pageTitle = `${appTitle} | ${translationId}`;
                });
        } else {
            $rootScope.pageTitle = appTitle;
        }
    }

    function gotoDefaultPlace(params) {
        userService.gotoDefaultPlace(params);
    }

    function gotoPublicModule(name, params) {
        whiteLabelingService.loadLoginWhiteLabelingParams().then(
            () => {
                skipStateChange = true;
                $state.go(name, params);
            },
            () => {
                skipStateChange = true;
                $state.go(name, params);
            }
        );
    }

    function showUnauthorizedDialog() {
        if (unauthorizedDialog === null) {
            $translate(['access.unauthorized-access',
                        'access.unauthorized-access-text',
                        'access.unauthorized',
                        'action.cancel',
                        'action.sign-in']).then(function (translations) {
                if (unauthorizedDialog === null) {
                    unauthorizedDialog = $mdDialog.confirm()
                        .title(translations['access.unauthorized-access'])
                        .textContent(translations['access.unauthorized-access-text'])
                        .ariaLabel(translations['access.unauthorized'])
                        .cancel(translations['action.cancel'])
                        .ok(translations['action.sign-in']);
                    $mdDialog.show(unauthorizedDialog).then(function () {
                        unauthorizedDialog = null;
                        gotoPublicModule('login');
                    }, function () {
                        unauthorizedDialog = null;
                    });
                }
            });
        }
    }

    function showForbiddenDialog() {
        if (forbiddenDialog === null) {
            $translate(['access.access-forbidden',
                'access.access-forbidden-text',
                'access.access-forbidden',
                'action.cancel',
                'action.sign-in']).then(function (translations) {
                if (forbiddenDialog === null) {
                    forbiddenDialog = $mdDialog.confirm()
                        .title(translations['access.access-forbidden'])
                        .htmlContent(translations['access.access-forbidden-text'])
                        .ariaLabel(translations['access.access-forbidden'])
                        .cancel(translations['action.cancel'])
                        .ok(translations['action.sign-in']);
                    $mdDialog.show(forbiddenDialog).then(function () {
                        forbiddenDialog = null;
                        userService.logout();
                    }, function () {
                        forbiddenDialog = null;
                    });
                }
            });
        }
    }
}
