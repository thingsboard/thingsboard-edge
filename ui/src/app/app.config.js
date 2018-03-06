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
import injectTapEventPlugin from 'react-tap-event-plugin';
import UrlHandler from './url.handler';
import addLocaleKorean from './locale/locale.constant-ko';
import addLocaleChinese from './locale/locale.constant-zh';
import addLocaleRussian from './locale/locale.constant-ru';
import addLocaleSpanish from './locale/locale.constant-es';
import addLocaleFrench from './locale/locale.constant-fr';

/* eslint-disable import/no-unresolved, import/default */

import mdiIconSet from '../svg/mdi.svg';

/* eslint-enable import/no-unresolved, import/default */

const PRIMARY_BACKGROUND_COLOR = "#305680";//#2856b6";//"#3f51b5";
const SECONDARY_BACKGROUND_COLOR = "#527dad";
const HUE3_COLOR = "#a7c1de";

/*@ngInject*/
export default function AppConfig($provide,
                                  $urlRouterProvider,
                                  $locationProvider,
                                  $mdIconProvider,
                                  $mdThemingProvider,
                                  $httpProvider,
                                  $translateProvider,
                                  storeProvider,
                                  locales) {

    injectTapEventPlugin();
    $locationProvider.html5Mode(true);
    $urlRouterProvider.otherwise(UrlHandler);
    storeProvider.setCaching(false);

    $translateProvider.useSanitizeValueStrategy(null);
    $translateProvider.useMissingTranslationHandler('tbMissingTranslationHandler');
    $translateProvider.addInterpolation('$translateMessageFormatInterpolation');
    $translateProvider.fallbackLanguage('en_US');

    addLocaleKorean(locales);
    addLocaleChinese(locales);
    addLocaleRussian(locales);
    addLocaleSpanish(locales);
    addLocaleFrench(locales);

    for (var langKey in locales) {
        var translationTable = locales[langKey];
        $translateProvider.translations(langKey, translationTable);
    }

    var lang = $translateProvider.resolveClientLocale();
    if (lang) {
        lang = lang.toLowerCase();
        if (lang.startsWith('ko')) {
            $translateProvider.preferredLanguage('ko_KR');
        } else if (lang.startsWith('zh')) {
            $translateProvider.preferredLanguage('zh_CN');
        } else if (lang.startsWith('es')) {
            $translateProvider.preferredLanguage('es_ES');
        } else if (lang.startsWith('ru')) {
            $translateProvider.preferredLanguage('ru_RU');
        } else if (lang.startsWith('fr')) {
            $translateProvider.preferredLanguage('fr_FR');
        } else {
            $translateProvider.preferredLanguage('en_US');
        }
    } else {
        $translateProvider.preferredLanguage('en_US');
    }

    $httpProvider.interceptors.push('globalInterceptor');

    $provide.decorator("$exceptionHandler", ['$delegate', '$injector', function ($delegate/*, $injector*/) {
        return function (exception, cause) {
/*            var rootScope = $injector.get("$rootScope");
            var $window = $injector.get("$window");
            var utils = $injector.get("utils");
            if (rootScope.widgetEditMode) {
                var parentScope = $window.parent.angular.element($window.frameElement).scope();
                var data = utils.parseException(exception);
                parentScope.$emit('widgetException', data);
                parentScope.$apply();
            }*/
            $delegate(exception, cause);
        };
    }]);

    $mdIconProvider.iconSet('mdi', mdiIconSet);

    configureTheme();

    function blueGrayTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('blue-grey');
        var tbAccentPalette = $mdThemingProvider.extendPalette('orange', {
            'contrastDefaultColor': 'light'
        });

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-primary')
            .dark();
    }

    function indigoTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('indigo', {
            '500': PRIMARY_BACKGROUND_COLOR,
            '600': SECONDARY_BACKGROUND_COLOR,
            'A100': HUE3_COLOR
        });

        var tbAccentPalette = $mdThemingProvider.extendPalette('deep-orange');

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        var tbDarkPrimaryPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '500': '#9fa8da'
        });

        var tbDarkPrimaryBackgroundPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '800': PRIMARY_BACKGROUND_COLOR
        });

        $mdThemingProvider.definePalette('tb-dark-primary', tbDarkPrimaryPalette);
        $mdThemingProvider.definePalette('tb-dark-primary-background', tbDarkPrimaryBackgroundPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-dark-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-dark-primary-background')
            .dark();
    }

    function configureTheme() {
        //white-labeling
        $mdThemingProvider.generateThemesOnDemand(true);
        $provide.value('themeProvider', $mdThemingProvider);

        var theme = 'indigo';

        if (theme === 'blueGray') {
            blueGrayTheme();
        } else {
            indigoTheme();
        }

        $mdThemingProvider.setDefaultTheme('default');
        $mdThemingProvider.alwaysWatchTheme(true);
    }

}