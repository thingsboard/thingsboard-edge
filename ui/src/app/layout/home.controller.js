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
import $ from 'jquery';

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function HomeController(types, loginService, userService, deviceService, whiteLabelingService, Fullscreen, $scope, $element, $rootScope, $document, $state,
                                       $window, $log, $mdMedia, $animate, $timeout) {

    var siteSideNav = $('.tb-site-sidenav', $element);

    var vm = this;

    vm.Fullscreen = Fullscreen;

    if (angular.isUndefined($rootScope.searchConfig)) {
        $rootScope.searchConfig = {
            searchEnabled: false,
            searchByEntitySubtype: false,
            searchEntityType: null,
            showSearch: false,
            searchText: "",
            searchEntitySubtype: ""
        };
    }

    vm.isShowSidenav = false;
    vm.isLockSidenav = false;

    vm.displaySearchMode = displaySearchMode;
    vm.displayEntitySubtypeSearch = displayEntitySubtypeSearch;
    vm.openSidenav = openSidenav;
    vm.goBack = goBack;
    vm.searchTextUpdated = searchTextUpdated;
    vm.sidenavClicked = sidenavClicked;
    vm.toggleFullscreen = toggleFullscreen;
    vm.openSearch = openSearch;
    vm.closeSearch = closeSearch;

    $scope.$on('$stateChangeSuccess', function (evt, to, toParams, from) {
        watchEntitySubtype(false);
        if (angular.isDefined(to.data.searchEnabled)) {
            $scope.searchConfig.searchEnabled = to.data.searchEnabled;
            $scope.searchConfig.searchByEntitySubtype = to.data.searchByEntitySubtype;
            $scope.searchConfig.searchEntityType = to.data.searchEntityType;
            if ($scope.searchConfig.searchEnabled === false || to.name !== from.name) {
                $scope.searchConfig.showSearch = false;
                $scope.searchConfig.searchText = "";
                $scope.searchConfig.searchEntitySubtype = "";
            }
        } else {
            $scope.searchConfig.searchEnabled = false;
            $scope.searchConfig.searchByEntitySubtype = false;
            $scope.searchConfig.searchEntityType = null;
            $scope.searchConfig.showSearch = false;
            $scope.searchConfig.searchText = "";
            $scope.searchConfig.searchEntitySubtype = "";
        }
        watchEntitySubtype($scope.searchConfig.searchByEntitySubtype);
    });

    $scope.$on('whiteLabelingChanged', () => {
        loadLogo();
    });

    vm.isGtSm = $mdMedia('gt-sm');
    if (vm.isGtSm) {
        vm.isLockSidenav = true;
        $animate.enabled(siteSideNav, false);
    }

    $scope.$watch(function() { return $mdMedia('gt-sm'); }, function(isGtSm) {
        vm.isGtSm = isGtSm;
        vm.isLockSidenav = isGtSm;
        vm.isShowSidenav = isGtSm;
        if (!isGtSm) {
            $timeout(function() {
                $animate.enabled(siteSideNav, true);
            }, 0, false);
        } else {
            $animate.enabled(siteSideNav, false);
        }
    });

    loadLogo();

    function loadLogo() {
        vm.logoSvg = whiteLabelingService.logoImageUrl();
        vm.logoHeight = whiteLabelingService.logoImageHeight();
    }

    function watchEntitySubtype(enableWatch) {
        if ($scope.entitySubtypeWatch) {
            $scope.entitySubtypeWatch();
        }
        if (enableWatch) {
            $scope.entitySubtypeWatch = $scope.$watch('searchConfig.searchEntitySubtype', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    $scope.$broadcast('searchEntitySubtypeUpdated');
                }
            });
        }
    }

    function displaySearchMode() {
        return $scope.searchConfig.searchEnabled &&
            $scope.searchConfig.showSearch;
    }

    function displayEntitySubtypeSearch() {
        return $scope.searchConfig.searchByEntitySubtype && vm.isGtSm;
    }

    function toggleFullscreen() {
        if (Fullscreen.isEnabled()) {
            Fullscreen.cancel();
        } else {
            Fullscreen.all();
        }
    }

    function openSearch() {
        if ($scope.searchConfig.searchEnabled) {
            $scope.searchConfig.showSearch = true;
            $timeout(() => {
                angular.element('#tb-search-text-input', $element).focus();
            });
        }
    }

    function closeSearch() {
        if ($scope.searchConfig.searchEnabled) {
            $scope.searchConfig.showSearch = false;
            if ($scope.searchConfig.searchText.length) {
                $scope.searchConfig.searchText = '';
                searchTextUpdated();
            }
        }
    }

    function searchTextUpdated() {
        $scope.$broadcast('searchTextUpdated');
    }

    function openSidenav() {
        vm.isShowSidenav = true;
    }

    function goBack() {
        $window.history.back();
    }

    function closeSidenav() {
        vm.isShowSidenav = false;
    }

    function sidenavClicked() {
        if (!vm.isLockSidenav) {
            closeSidenav();
        }
    }

}

/* eslint-enable angular/angularelement */