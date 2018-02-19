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
function WhiteLabelingService($rootScope, $q, $http, store, themeProvider, $mdTheming) {

    const DARK_FOREGROUND = {
        name: 'dark',
        '1': 'rgba(0,0,0,0.87)',
        '2': 'rgba(0,0,0,0.54)',
        '3': 'rgba(0,0,0,0.38)',
        '4': 'rgba(0,0,0,0.12)'
    };
    const LIGHT_SHADOW = '';

    const defaultWLParams = {
        logoImageUrl: defaultImageUrl,
        logoImageChecksum: '4a216f7cb9b76effe0d583ef5bddd98e377e2fa9',
        logoImageHeight: 36,
        appTitle: 'ThingsBoard PE',
        favicon: {
            url: 'static/thingsboard.ico',
            type: 'image/x-icon'
        },
        faviconChecksum: '87059b3055f7ce8b8e43f18f470ed895a316f5ec',
        paletteSettings: {
            primaryPalette: {
                type: 'tb-primary'
            },
            accentPalette: {
                type: 'tb-accent'
            }
        }
    };

    const defaultLoginWlParams = angular.copy(defaultWLParams);
    defaultLoginWlParams.logoImageHeight = 50;
    defaultLoginWlParams.pageBackgroundColor = '#eee';
    defaultLoginWlParams.darkForeground = false;

    var currentWLParams;
    var currentLoginWLParams;
    var loginWlParams;
    var userWlParams;

    var isUserWlMode = false;

    var primaryPaletteName;
    var accentPaletteName;

    var service = {
        loadLoginWhiteLabelingParams: loadLoginWhiteLabelingParams,
        loadUserWhiteLabelingParams: loadUserWhiteLabelingParams,
        logoImageUrl: logoImageUrl,
        logoImageHeight: logoImageHeight,
        appTitle: appTitle,
        faviconUrl: faviconUrl,
        faviconType: faviconType,
        getPrimaryPalette: getPrimaryPalette,
        getAccentPalette: getAccentPalette,
        whiteLabelPreview: whiteLabelPreview,
        cancelWhiteLabelPreview: cancelWhiteLabelPreview,
        getCurrentWhiteLabelParams: getCurrentWhiteLabelParams,
        getCurrentLoginWhiteLabelParams: getCurrentLoginWhiteLabelParams,
        saveWhiteLabelParams: saveWhiteLabelParams,
        saveLoginWhiteLabelParams: saveLoginWhiteLabelParams
    };

    return service;

    function getCurrentWlParams() {
        return isUserWlMode ? currentWLParams : currentLoginWLParams;
    }

    function logoImageUrl() {
        return getCurrentWlParams() ? getCurrentWlParams().logoImageUrl : '';
    }

    function logoImageHeight() {
        return getCurrentWlParams() ? getCurrentWlParams().logoImageHeight: '';
    }

    function appTitle() {
        return getCurrentWlParams() ? getCurrentWlParams().appTitle : '';
    }

    function faviconUrl() {
        return getCurrentWlParams() ? getCurrentWlParams().favicon.url : '';
    }

    function faviconType() {
        return getCurrentWlParams() ? getCurrentWlParams().favicon.type : '';
    }

    function getPrimaryPalette() {
        return themeProvider._PALETTES[primaryPaletteName];
    }

    function getAccentPalette() {
        return themeProvider._PALETTES[accentPaletteName];
    }

    function loadLoginWhiteLabelingParams() {
        var deferred = $q.defer();
        var storedLogoImageChecksum = store.get('login_logo_image_checksum');
        var storedFaviconChecksum = store.get('login_favicon_checksum');
        var url = '/api/noauth/whiteLabel/loginWhiteLabelParams';
        if (storedLogoImageChecksum) {
            url += '?logoImageChecksum='+storedLogoImageChecksum;
        }
        if (storedFaviconChecksum) {
            if (storedLogoImageChecksum) {
                url += '&'
            } else {
                url += '?'
            }
            url += 'faviconChecksum='+storedFaviconChecksum;
        }
        isUserWlMode = false;
        $http.get(url, null).then(
            (response) => {
                loginWlParams = mergeDefaults(response.data, defaultLoginWlParams);
                updateImages(loginWlParams, 'login');
                if (setLoginWlParams(loginWlParams)) {
                    applyLoginWlParams(currentLoginWLParams);
                    applyLoginThemePalettes(currentLoginWLParams.paletteSettings, currentLoginWLParams.darkForeground);
                    $rootScope.$broadcast('whiteLabelingChanged');
                }
                deferred.resolve();
            },
            () => {
                if (loginWlParams) {
                    if (setLoginWlParams(loginWlParams)) {
                        applyLoginWlParams(currentLoginWLParams);
                        applyLoginThemePalettes(currentLoginWLParams.paletteSettings, currentLoginWLParams.darkForeground);
                        $rootScope.$broadcast('whiteLabelingChanged');
                    }
                    deferred.resolve();
                } else {
                    deferred.reject();
                }
            }
        );
        return deferred.promise;
    }

    function loadUserWhiteLabelingParams() {
        var deferred = $q.defer();
        var storedLogoImageChecksum = store.get('user_logo_image_checksum');
        var storedFaviconChecksum = store.get('user_favicon_checksum');
        var url = '/api/whiteLabel/whiteLabelParams';
        if (storedLogoImageChecksum) {
            url += '?logoImageChecksum='+storedLogoImageChecksum;
        }
        if (storedFaviconChecksum) {
            if (storedLogoImageChecksum) {
                url += '&'
            } else {
                url += '?'
            }
            url += 'faviconChecksum='+storedFaviconChecksum;
        }
        $http.get(url, null).then(
            (response) => {
                isUserWlMode = true;
                userWlParams = mergeDefaults(response.data);
                updateImages(userWlParams, 'user');
                if (setWlParams(userWlParams)) {
                    wlChanged();
                }
                deferred.resolve();
            },
            () => {
                isUserWlMode = true;
                if (userWlParams) {
                    if (setWlParams(userWlParams)) {
                        wlChanged();
                    }
                    deferred.resolve();
                } else {
                    deferred.reject();
                }
            }
        );
        return deferred.promise;
    }

    function setLoginWlParams(newWlParams) {
        if (!angular.equals(currentLoginWLParams, newWlParams)) {
            currentLoginWLParams = newWlParams;
            return true;
        } else {
            return false;
        }
    }

    function setWlParams(newWlParams) {
        if (!angular.equals(currentWLParams, newWlParams)) {
            currentWLParams = newWlParams;
            return true;
        } else {
            return false;
        }
    }

    function mergeDefaults(wlParams, targetDefaultWlParams) {
        if (!targetDefaultWlParams) {
            targetDefaultWlParams = defaultWLParams;
        }
        if (!wlParams) {
            wlParams = {};
        }
        if (!wlParams.pageBackgroundColor && targetDefaultWlParams.pageBackgroundColor) {
            wlParams.pageBackgroundColor = targetDefaultWlParams.pageBackgroundColor;
        }
        if (!wlParams.logoImageUrl && !wlParams.logoImageChecksum) {
            wlParams.logoImageUrl = targetDefaultWlParams.logoImageUrl;
            wlParams.logoImageChecksum = targetDefaultWlParams.logoImageChecksum;
        }
        if (!wlParams.logoImageHeight) {
            wlParams.logoImageHeight = targetDefaultWlParams.logoImageHeight;
        }
        if (!wlParams.appTitle) {
            wlParams.appTitle = targetDefaultWlParams.appTitle;
        }
        if ((!wlParams.favicon || !wlParams.favicon.url) && !wlParams.faviconChecksum) {
            wlParams.favicon = targetDefaultWlParams.favicon;
            wlParams.faviconChecksum = targetDefaultWlParams.faviconChecksum;
        }
        if (!wlParams.paletteSettings) {
            wlParams.paletteSettings = targetDefaultWlParams.paletteSettings;
        } else {
            if (!wlParams.paletteSettings.primaryPalette || !wlParams.paletteSettings.primaryPalette.type) {
                wlParams.paletteSettings.primaryPalette = targetDefaultWlParams.paletteSettings.primaryPalette;
            }
            if (!wlParams.paletteSettings.accentPalette || !wlParams.paletteSettings.accentPalette.type) {
                wlParams.paletteSettings.accentPalette = targetDefaultWlParams.paletteSettings.accentPalette;
            }
        }
        return wlParams;
    }

    function wlChanged() {
        applyThemePalettes(currentWLParams.paletteSettings);
        $rootScope.$broadcast('whiteLabelingChanged');
    }

    function whiteLabelPreview(wLParams) {
        var url = '/api/whiteLabel/previewWhiteLabelParams';
        $http.post(url, wLParams).then(function success(response) {
            var wlParams = mergeDefaults(response.data);
            currentWLParams = wlParams;
            wlChanged();
        }, function fail() {
        });
    }

    function cancelWhiteLabelPreview() {
        currentWLParams = userWlParams;
        wlChanged();
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

    function getCurrentLoginWhiteLabelParams() {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/currentLoginWhiteLabelParams';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(checkWlParams(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveWhiteLabelParams(wlParams) {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/whiteLabelParams';
        $http.post(url, wlParams).then(function success() {
            loadUserWhiteLabelingParams().then(
                () => {
                    deferred.resolve();
                },
                () => {
                    deferred.reject();
                }
            );
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveLoginWhiteLabelParams(wlParams) {
        var deferred = $q.defer();
        var url = '/api/whiteLabel/loginWhiteLabelParams';
        $http.post(url, wlParams).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function checkWlParams(whiteLabelParams) {
        if (!whiteLabelParams) {
            whiteLabelParams = {};
        }
        if (!whiteLabelParams.paletteSettings) {
            whiteLabelParams.paletteSettings = {};
        }
        if (!whiteLabelParams.favicon) {
            whiteLabelParams.favicon = {};
        }
        return whiteLabelParams;
    }

    function updateImages(wlParams, prefix) {
        var storedLogoImageChecksum = store.get(prefix+'_logo_image_checksum');
        var storedFaviconChecksum = store.get(prefix+'_favicon_checksum');
        var logoImageChecksum = wlParams.logoImageChecksum;
        if (logoImageChecksum && !angular.equals(storedLogoImageChecksum, logoImageChecksum)) {
            var logoImageUrl = wlParams.logoImageUrl;
            store.set(prefix+'_logo_image_checksum', logoImageChecksum);
            store.set(prefix+'_logo_image_url', logoImageUrl);
        } else {
            wlParams.logoImageUrl = store.get(prefix+'_logo_image_url');
        }
        var faviconChecksum = wlParams.faviconChecksum;
        if (faviconChecksum && !angular.equals(storedFaviconChecksum, faviconChecksum)) {
            var favicon = wlParams.favicon;
            store.set(prefix+'_favicon_checksum', faviconChecksum);
            store.set(prefix+'_favicon_url', favicon.url);
            store.set(prefix+'_favicon_type', favicon.type);
        } else {
            wlParams.favicon = {
                url: store.get(prefix+'_favicon_url'),
                type: store.get(prefix+'_favicon_type'),
            };
        }
    }

    function applyThemePalettes(paletteSettings) {

        cleanupPalettes('custom-primary');
        cleanupPalettes('custom-accent');

        var primaryPalette = paletteSettings.primaryPalette;
        var accentPalette = paletteSettings.accentPalette;

        if (primaryPalette.type == 'tb-primary' &&
            accentPalette.type == 'tb-accent') {
            primaryPaletteName = primaryPalette.type;
            accentPaletteName = accentPalette.type;
            $rootScope.currentTheme = 'default';
            themeProvider.setDefaultTheme('default');
            return;
        }

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

    function applyLoginWlParams(wlParams) {
        $rootScope.loginLogo = wlParams.logoImageUrl;
        $rootScope.loginLogoHeight = wlParams.logoImageHeight;
        $rootScope.loginPageBackgroundColor = wlParams.pageBackgroundColor;
    }

    function applyLoginThemePalettes(paletteSettings, darkForeground) {
        var primaryPalette = paletteSettings.primaryPalette;
        var accentPalette = paletteSettings.accentPalette;

        if (primaryPalette.type == 'tb-primary' &&
            accentPalette.type == 'tb-accent') {
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
        var theme = themeProvider.theme(themeName)
            .primaryPalette(primaryPaletteName)
            .accentPalette(accentPaletteName)
            .backgroundPalette(backgroundPaletteName)
            .dark();

        if (darkForeground) {
            theme.foregroundPalette = DARK_FOREGROUND;
            theme.foregroundShadow = LIGHT_SHADOW;
        }

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