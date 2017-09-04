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

/* eslint-disable import/no-unresolved, import/default */

import defaultImageUrl from '../../svg/logo_title_white.svg';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.api.whiteLabeling', [])
    .factory('whiteLabelingService', WhiteLabelingService)
    .name;

/*@ngInject*/
function WhiteLabelingService($rootScope, $q, userService, $http, store, themeProvider, $mdTheming) {

    var defaultWLParams = {
        logoImageUrl: defaultImageUrl,
        logoImageHeight: 36,
        paletteSettings: {
            primaryPalette: {
                type: 'tb-primary'
            },
            accentPalette: {
                type: 'tb-accent'
            }
        }
    };

    var systemWLParams = {};
    var parentWLParams = {};
    var userWLParams = {};
    var currentWLParams = {};

    var defaultsLoaded = false;
    var userParamsLoaded = false;
    var paramResolveTasks = [];

    var primaryPaletteName = 'tb-primary';
    var accentPaletteName = 'tb-accent';

    var service = {
        logoImageUrl: logoImageUrl,
        logoImageHeight: logoImageHeight,
        whiteLabelPreview: whiteLabelPreview,
        getCurrentWhiteLabelParams: getCurrentWhiteLabelParams,
        saveWhiteLabelParams: saveWhiteLabelParams,
        cancelWhiteLabelPreview: cancelWhiteLabelPreview,
        getPrimaryPalette: getPrimaryPalette,
        getAccentPalette: getAccentPalette
    };

    if (userService.isUserLoaded()) {
        onUserLoaded();
    } else {
        $rootScope.wlUserLoadedHandle = $rootScope.$on('userLoaded', () => {
            $rootScope.wlUserLoadedHandle();
            onUserLoaded();
        });
    }

    configureTheme();

    $rootScope.whiteLabelingChangedHandle = $rootScope.$on('whiteLabelingChanged', () => {
        configureTheme();
    });

    return service;

    function onUserLoaded() {
        if (userService.isAuthenticated()) {
            loadUserWhiteLabelingParams();
            $rootScope.wlUserLoadedHandle = $rootScope.$on('userLoaded', () => {
                reloadUserWhiteLabelingParams();
            });
            $rootScope.wlUserAuthenticatedHandle = $rootScope.$on('authenticated', () => {
                reloadUserWhiteLabelingParams();
            });
        } else {
            $rootScope.wlUserAuthenticatedHandle = $rootScope.$on('authenticated', () => {
                $rootScope.wlUserAuthenticatedHandle();
                onUserLoaded();
            });
        }
    }

    function reloadUserWhiteLabelingParams() {
        loadUserWhiteLabelingParams().then(() => {
            $rootScope.$broadcast('whiteLabelingChanged');
        });
    }

    function loadUserWhiteLabelingParams() {
        var deferred = $q.defer();
        userWLParams = {};
        parentWLParams = {};
        var loadParentWlParams;
        if (userService.getAuthority() == 'SYS_ADMIN') {
            loadParentWlParams = $q.when({});
        } else {
            loadParentWlParams = loadWlParams(true);
        }
        loadParentWlParams.then(
                (wlParams) => {
                    parentWLParams = wlParams;
                    loadWlParams().then(
                        (wlParams) => {
                            userWLParams = wlParams;
                            userWhiteLabelingParamsLoaded();
                            deferred.resolve();
                        }
                    );
                }
            );
        return deferred.promise;
    }

    function loadWlParams(parent) {
        var deferred = $q.defer();
        var wlParams = {};
        loadLogoImage(parent).then((logoImageUrl) => {
                wlParams.logoImageUrl = logoImageUrl;
                loadWhiteLabelParams(parent).then(
                    (whiteLabelParams) => {
                        if (whiteLabelParams) {
                            wlParams.logoImageHeight = whiteLabelParams.logoImageHeight;
                            wlParams.paletteSettings = whiteLabelParams.paletteSettings;
                        }
                        deferred.resolve(wlParams);
                    },
                    () => {
                        deferred.resolve(wlParams);
                    }
                );
            },
            () => {
                deferred.resolve(wlParams);
            }
        );
        return deferred.promise;
    }

    function userWhiteLabelingParamsLoaded() {
        currentWLParams = userWLParams;
        userParamsLoaded = true;
        for (var i=0;i<paramResolveTasks.length;i++) {
            paramResolveTasks[i]();
        }
        paramResolveTasks.length = 0;
    }

    function loadLogoImage(parent) {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/logoImageChecksum?parent=' + ( parent ? 'true': 'false' );
        $http.get(url, null).then(function success(response) {
            var logoImageChecksum = response.data;
            if (logoImageChecksum && logoImageChecksum.length) {
                var storedChecksum = store.get(parent ? 'parent_logo_image_checksum' : 'logo_image_checksum');
                var storedLogoImageUrl = store.get( parent ? 'parent_logo_image_url' : 'logo_image_url');
                if (angular.equals(logoImageChecksum, storedChecksum) && storedLogoImageUrl && storedLogoImageUrl.length) {
                    deferred.resolve(storedLogoImageUrl);
                } else {
                    url = '/api/whiteLabel/logoImage?parent=' + ( parent ? 'true': 'false' );
                    $http.get(url, null).then(function success(response) {
                        var imageUrl = response.data;
                        store.set(parent ? 'parent_logo_image_checksum' : 'logo_image_checksum', logoImageChecksum);
                        store.set(parent ? 'parent_logo_image_url' : 'logo_image_url', imageUrl);
                        deferred.resolve(imageUrl);
                    }, function fail() {
                        deferred.reject();
                    });
                }
            } else {
                deferred.resolve(null);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function loadWhiteLabelParams(parent) {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/whiteLabelParams?parent=' + ( parent ? 'true': 'false' );
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCurrentWhiteLabelParams() {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/currentWhiteLabelParams';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSystemPaletteSettings() {
        var deferred = $q.defer();
        var url = '/api/noauth/whiteLabel/systemPaletteSettings';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveWhiteLabelParams(wlParams) {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/logoImage';
        $http.post(url, wlParams.logoImageUrl).then(function success(response) {
            var logoImageChecksum = response.data;
            store.set('logo_image_checksum', logoImageChecksum);
            store.set('logo_image_url', wlParams.logoImageUrl);
            url = '/api/whiteLabel/whiteLabelParams';
            var params = {
                logoImageHeight: wlParams.logoImageHeight,
                paletteSettings: wlParams.paletteSettings
            };
            $http.post(url, params).then(function success() {
                reloadUserWhiteLabelingParams();
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });

        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getInstantParam(paramName) {
        if (currentWLParams[paramName]) {
            return currentWLParams[paramName];
        } else if (parentWLParams[paramName]) {
            return parentWLParams[paramName];
        } else {
            return defaultWLParams[paramName];
        }
    }

    function hasPalette(wlParams, paletteType) {
        return wlParams.paletteSettings && wlParams.paletteSettings[paletteType]
               && wlParams.paletteSettings[paletteType].type;
    }

    function getInstantPalette(paletteType) {
        if (hasPalette(currentWLParams, paletteType)) {
            return currentWLParams.paletteSettings[paletteType];
        } else if (hasPalette(parentWLParams, paletteType)) {
            return parentWLParams.paletteSettings[paletteType];
        } else if (hasPalette(systemWLParams, paletteType)) {
            return systemWLParams.paletteSettings[paletteType];
        } else {
            return defaultWLParams.paletteSettings[paletteType];
        }
    }

    function getParam(paramName) {
        if (userParamsLoaded) {
            return $q.when(getInstantParam(paramName));
        } else {
            var deferred = $q.defer();
            var paramResolveTask = () => {
                deferred.resolve(getInstantParam(paramName));
            };
            paramResolveTasks.push(paramResolveTask);
            return deferred.promise;
        }
    }

    function getPaletteSettings() {
        if (userParamsLoaded) {
            var paletteSettings = {
                primaryPalette: getInstantPalette('primaryPalette'),
                accentPalette: getInstantPalette('accentPalette')
            }
            return $q.when(paletteSettings);
        } else {
            var deferred = $q.defer();
            var paramResolveTask = () => {
                var paletteSettings = {
                    primaryPalette: getInstantPalette('primaryPalette'),
                    accentPalette: getInstantPalette('accentPalette')
                }
                deferred.resolve(paletteSettings);
            };
            paramResolveTasks.push(paramResolveTask);
            return deferred.promise;
        }
    }

    function logoImageUrl() {
        return getParam('logoImageUrl');
    }

    function logoImageHeight() {
        return getParam('logoImageHeight');
    }

    function whiteLabelPreview(wLParams) {
        currentWLParams = wLParams;
        $rootScope.$broadcast('whiteLabelingChanged');
    }

    function cancelWhiteLabelPreview() {
        currentWLParams = userWLParams;
        $rootScope.$broadcast('whiteLabelingChanged');
    }

    function getPrimaryPalette() {
        return themeProvider._PALETTES[primaryPaletteName];
    }

    function getAccentPalette() {
        return themeProvider._PALETTES[accentPaletteName];
    }

    function configureTheme() {
        loadDefaults().then(() => {
            getPaletteSettings().then(
                (paletteSettings) => {
                    store.set('theme_palette_settings', angular.toJson(paletteSettings));
                    applyThemePalettes(paletteSettings);
                }
            );
        });
    }

    function loadDefaults() {
        var deferred = $q.defer();
        if (!defaultsLoaded) {
            defaultsLoaded = true;
            var paletteSettingsJson = store.get('theme_palette_settings');
            var storedPaletteSettings;
            if (paletteSettingsJson && paletteSettingsJson.length) {
                try {
                    storedPaletteSettings = angular.fromJson(paletteSettingsJson)
                } catch (e) {
                    /**/
                }
            }
            if (storedPaletteSettings) {
                applyThemePalettes(storedPaletteSettings);
            } else {
                $rootScope.currentTheme = 'default';
            }
        }
        getSystemPaletteSettings().then(
            (paletteSettings) => {
                systemWLParams.paletteSettings = {
                    primaryPalette: paletteSettings ? paletteSettings.primaryPalette : null,
                    accentPalette: paletteSettings ? paletteSettings.accentPalette : null
                };
                if (paletteSettings && paletteSettings.primaryPalette
                    && paletteSettings.accentPalette) {
                    applyLoginThemePalettes(paletteSettings);
                } else {
                    $rootScope.currentLoginTheme = 'tb-dark';
                }
                deferred.resolve();
            },
            () => {
                $rootScope.currentLoginTheme = 'tb-dark';
                deferred.resolve();
            }
        );
        return deferred.promise;
    }

    function applyThemePalettes(paletteSettings) {
        var primaryPalette = paletteSettings.primaryPalette;
        var accentPalette = paletteSettings.accentPalette;
        if (primaryPalette.type != 'custom') {
            primaryPaletteName = primaryPalette.type;
        } else {
            primaryPaletteName = 'custom-primary';
            var customPrimaryPalette = themeProvider.extendPalette(primaryPalette.extends, prepareColors(primaryPalette.colors, primaryPalette.extends));
            themeProvider.definePalette(primaryPaletteName, customPrimaryPalette);
        }
        if (accentPalette.type != 'custom') {
            accentPaletteName = accentPalette.type;
        } else {
            accentPaletteName = 'custom-accent';
            var customAccentPalette = themeProvider.extendPalette(accentPalette.extends, prepareColors(accentPalette.colors, accentPalette.extends));
            themeProvider.definePalette(accentPaletteName, customAccentPalette);
        }

        cleanupThemes('tb-custom-theme-');

        var themeName = 'tb-custom-theme-' + (Math.random()*1000).toFixed(0);
        themeProvider.theme(themeName)
            .primaryPalette(primaryPaletteName)
            .accentPalette(accentPaletteName);

        $mdTheming.generateTheme(themeName);

        $mdTheming.THEMES = angular.extend({}, themeProvider._THEMES);

        themeProvider.setDefaultTheme(themeName);

        $rootScope.currentTheme = themeName;
    }

    function applyLoginThemePalettes(paletteSettings) {
        var primaryPalette = paletteSettings.primaryPalette;
        var accentPalette = paletteSettings.accentPalette;
        if (!primaryPalette.type) {
            primaryPalette = defaultWLParams.paletteSettings.primaryPalette;
        }
        if (!accentPalette.type) {
            accentPalette = defaultWLParams.paletteSettings.accentPalette;
        }
        var primaryPaletteName;
        var accentPaletteName;
        var backgroundPaletteName;

        var primaryBackgroundColor;
        var primaryBackgroundContrastColor;

        var primaryExtends = primaryPalette.type != 'custom' ? primaryPalette.type : primaryPalette.extends;
        var primaryColors = primaryPalette.colors ? angular.copy(primaryPalette.colors) : {};
        primaryBackgroundContrastColor = primaryColors['200'] ? primaryColors['200'] : $mdTheming.PALETTES[primaryExtends]['200'].hex;
        primaryBackgroundColor = primaryColors['500'] ? primaryColors['500'] : $mdTheming.PALETTES[primaryExtends]['500'].hex;
        primaryColors['500'] = primaryBackgroundContrastColor;
        primaryPaletteName = 'custom-login-primary';
        var customLoginPrimaryPalette = themeProvider.extendPalette(primaryExtends, prepareColors(primaryColors, primaryExtends));
        themeProvider.definePalette(primaryPaletteName, customLoginPrimaryPalette);
        if (accentPalette.type != 'custom') {
            accentPaletteName = accentPalette.type;
        } else {
            accentPaletteName = 'custom-login-accent';
            var customLoginAccentPalette = themeProvider.extendPalette(accentPalette.extends, prepareColors(accentPalette.colors, accentPalette.extends));
            themeProvider.definePalette(accentPaletteName, customLoginAccentPalette);
        }
        backgroundPaletteName = 'custom-login-background';
        var backgroundPaletteColors = {
            '800':  primaryBackgroundColor
        };
        var customLoginBackgroundPalette = themeProvider.extendPalette(primaryPaletteName,
            prepareColors(backgroundPaletteColors, primaryPaletteName));
        themeProvider.definePalette(backgroundPaletteName, customLoginBackgroundPalette);

        cleanupThemes('tb-custom-login-theme-');

        var themeName = 'tb-custom-login-theme-' + (Math.random()*1000).toFixed(0);
        themeProvider.theme(themeName)
            .primaryPalette(primaryPaletteName)
            .accentPalette(accentPaletteName)
            .backgroundPalette(backgroundPaletteName)
            .dark();

        $mdTheming.generateTheme(themeName);

        $mdTheming.THEMES = angular.extend({}, themeProvider._THEMES);

        $rootScope.currentLoginTheme = themeName;
    }

    function cleanupThemes(prefix) {
        var styleElements = angular.element('style');
        for (var i=0;i<styleElements.length;i++) {
            var styleElement = styleElements[i];
            if (styleElement.hasAttribute('md-theme-style')) {
                var content = styleElement.innerHTML || styleElement.innerText || styleElement.textContent;
                if( content.indexOf(prefix) >= 0){
                    styleElement.parentNode.removeChild(styleElement);
                }
            }
        }

        for (var theme in themeProvider._THEMES) {
            if (theme.startsWith(prefix)) {
                delete themeProvider._THEMES[theme];
            }
        }
    }

    function prepareColors(origColors, extendPalette) {
        var extendPaletteInfo = themeProvider._PALETTES[extendPalette];
        var colors = {};
        for (var hue in origColors) {
            var rgbValue = colorToRgbaArray(origColors[hue]);
            colors[hue] = {
                hex: origColors[hue],
                value: rgbValue,
                contrast: extendPaletteInfo[hue].contrast
            };
        }
        return colors;
    }

    function colorToRgbaArray(clr) {
        if (angular.isArray(clr) && clr.length == 3) return clr;
        if (/^rgb/.test(clr)) {
            return clr.replace(/(^\s*rgba?\(|\)\s*$)/g, '').split(',').map(function(value, i) {
                return i == 3 ? parseFloat(value, 10) : parseInt(value, 10);
            });
        }
        if (clr.charAt(0) == '#') clr = clr.substring(1);
        if (!/^([a-fA-F0-9]{3}){1,2}$/g.test(clr)) return;

        var dig = clr.length / 3;
        var red = clr.substr(0, dig);
        var grn = clr.substr(dig, dig);
        var blu = clr.substr(dig * 2);
        if (dig === 1) {
            red += red;
            grn += grn;
            blu += blu;
        }
        return [parseInt(red, 16), parseInt(grn, 16), parseInt(blu, 16)];
    }

}