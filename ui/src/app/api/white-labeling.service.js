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
        appTitle: 'ThingsBoard EE',
        faviconUrl: 'static/thingsboard.ico',
        faviconType: 'image/x-icon',
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
    var systemParamsLoaded = false;
    var userParamsLoaded = false;
    var paramResolveTasks = [];
    var systemParamResolveTasks = [];

    var primaryPaletteName = 'tb-primary';
    var accentPaletteName = 'tb-accent';

    var service = {
        logoImageUrl: logoImageUrl,
        logoImageHeight: logoImageHeight,
        appTitle: appTitle,
        faviconUrl: faviconUrl,
        faviconType: faviconType,
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

    configureWhiteLabeling(true);

    return service;

    function wlChanged(loadPalette) {
        configureWhiteLabeling(loadPalette).then(
            () => {
                $rootScope.$broadcast('whiteLabelingChanged');
            }
        );
    }

    function onUserLoaded() {
        if (userService.isAuthenticated()) {
            reloadUserWhiteLabelingParams();
            $rootScope.wlUserLoadedHandle = $rootScope.$on('userLoaded', () => {
                reloadUserWhiteLabelingParams();
            });
            $rootScope.wlUserAuthenticatedHandle = $rootScope.$on('authenticated', () => {
                reloadUserWhiteLabelingParams();
            });
            $rootScope.wlUserAuthenticatedHandle = $rootScope.$on('unauthenticated', () => {
                resetUserWhiteLabelingParams();
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
            wlChanged(true);
        });
    }

    function resetUserWhiteLabelingParams() {
        userWLParams = {};
        parentWLParams = {};
        currentWLParams = userWLParams;
        userParamsLoaded = false;
        wlChanged(false);
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
                            wlParams = checkWlParams(whiteLabelParams);
                            wlParams.logoImageUrl = logoImageUrl;
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

    function checkWlParams(whiteLabelParams) {
        if (!whiteLabelParams) {
            whiteLabelParams = {};
        }
        if (!whiteLabelParams.paletteSettings) {
            whiteLabelParams.paletteSettings = {};
        } else if (angular.isString(whiteLabelParams.paletteSettings)) {
            whiteLabelParams.paletteSettings = angular.fromJson(whiteLabelParams.paletteSettings);
        }
        if (whiteLabelParams.faviconUrl && whiteLabelParams.faviconUrl.length) {
            whiteLabelParams.faviconType = extractTypeFromDataUrl(whiteLabelParams.faviconUrl);
        }
        return whiteLabelParams;
    }

    function extractTypeFromDataUrl(dataUrl) {
        var type;
        if (dataUrl) {
            var res = dataUrl.split(";");
            if (res && res.length) {
                res = res[0];
                res = res.split(":");
                if (res && res.length > 1) {
                    type = res[1];
                }
            }
        }
        return type;
    }

    function userWhiteLabelingParamsLoaded() {
        currentWLParams = userWLParams;
        userParamsLoaded = true;
        for (var i=0;i<paramResolveTasks.length;i++) {
            paramResolveTasks[i]();
        }
        paramResolveTasks.length = 0;
    }

    function systemWhiteLabelingParamsLoaded() {
        systemParamsLoaded = true;
        for (var i=0;i<systemParamResolveTasks.length;i++) {
            systemParamResolveTasks[i]();
        }
        systemParamResolveTasks.length = 0;
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
            deferred.resolve(checkWlParams(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSystemWhiteLabelParams() {
        var deferred = $q.defer();
        var url = '/api/noauth/whiteLabel/systemWhiteLabelParams';
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
            var params = angular.copy(wlParams);
            delete params.logoImageUrl;
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
        } else if (systemWLParams[paramName]) {
            return systemWLParams[paramName];
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

    function getParam(paramName, systemByDefault) {
        var deferred;
        if (userParamsLoaded) {
            return $q.when(getInstantParam(paramName));
        } else if (systemByDefault) {
            if (systemParamsLoaded) {
                return $q.when(getInstantParam(paramName));
            } else {
                deferred = $q.defer();
                var systemParamResolveTask = () => {
                    deferred.resolve(getInstantParam(paramName));
                };
                systemParamResolveTasks.push(systemParamResolveTask);
                return deferred.promise;
            }
        } else {
            deferred = $q.defer();
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

    function faviconUrl() {
        return getParam('faviconUrl', true);
    }

    function faviconType() {
        return getParam('faviconType', true);
    }

    function logoImageUrl() {
        return getParam('logoImageUrl');
    }

    function logoImageHeight() {
        return getParam('logoImageHeight');
    }

    function appTitle() {
        return getParam('appTitle', true);
    }

    function whiteLabelPreview(wLParams) {
        currentWLParams = wLParams;
        wlChanged(true);
    }

    function cancelWhiteLabelPreview() {
        currentWLParams = userWLParams;
        wlChanged(true);
    }

    function getPrimaryPalette() {
        return themeProvider._PALETTES[primaryPaletteName];
    }

    function getAccentPalette() {
        return themeProvider._PALETTES[accentPaletteName];
    }

    function configureWhiteLabeling(loadPalette) {
        var deferred = $q.defer();
        loadDefaults().then(() => {
            if (loadPalette) {
                getPaletteSettings().then(
                    (paletteSettings) => {
                        store.set('theme_palette_settings', angular.toJson(paletteSettings));
                        applyThemePalettes(paletteSettings);
                        deferred.resolve();
                    }
                );
            } else {
                deferred.resolve();
            }
        });
        return deferred.promise;
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
        getSystemWhiteLabelParams().then(
            (wlParams) => {
                systemWLParams = checkWlParams(wlParams);
                applyLoginThemePalettes(systemWLParams.paletteSettings);
                if (userService.getAuthority() == 'SYS_ADMIN') {
                    systemWLParams = {};
                }
                systemWhiteLabelingParamsLoaded();
                deferred.resolve();
            },
            () => {
                $rootScope.currentLoginTheme = 'tb-dark';
                systemWhiteLabelingParamsLoaded();
                deferred.resolve();
            }
        );
        return deferred.promise;
    }

    function applyThemePalettes(paletteSettings) {

        cleanupPalettes('custom-primary');
        cleanupPalettes('custom-accent');

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

        $mdTheming.PALETTES = angular.extend({}, themeProvider._PALETTES);
        $mdTheming.THEMES = angular.extend({}, themeProvider._THEMES);

        themeProvider.setDefaultTheme(themeName);

        $rootScope.currentTheme = themeName;
    }

    function applyLoginThemePalettes(paletteSettings) {
        var primaryPalette = paletteSettings.primaryPalette;
        var accentPalette = paletteSettings.accentPalette;
        if (!primaryPalette || !primaryPalette.type) {
            primaryPalette = defaultWLParams.paletteSettings.primaryPalette;
        }
        if (!accentPalette || !accentPalette.type) {
            accentPalette = defaultWLParams.paletteSettings.accentPalette;
        }
        if (primaryPalette == defaultWLParams.paletteSettings.primaryPalette &&
            accentPalette == defaultWLParams.paletteSettings.accentPalette) {
            $rootScope.currentLoginTheme = 'tb-dark';
            return;
        }

        cleanupPalettes('custom-login-');

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

        $mdTheming.PALETTES = angular.extend({}, themeProvider._PALETTES);
        $mdTheming.THEMES = angular.extend({}, themeProvider._THEMES);

        $rootScope.currentLoginTheme = themeName;
    }

    function cleanupPalettes(prefix) {
        for (var palette in themeProvider._PALETTES) {
            if (palette.startsWith(prefix)) {
                delete themeProvider._PALETTES[palette];
            }
        }
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