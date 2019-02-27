/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
export default angular.module('thingsboard.api.customTranslation', [])
    .factory('customTranslationService', CustomTranslationService)
    .name;

/*@ngInject*/
function CustomTranslationService($rootScope, $q, $http, $translateProvider, $translate, userService) {

    var service = {
        updateCustomTranslations: updateCustomTranslations,
        getCurrentCustomTranslation: getCurrentCustomTranslation,
        saveCustomTranslation: saveCustomTranslation,
        downloadLocaleJson: downloadLocaleJson
    };

    service.translationMap = null;

    return service;

    function loadCustomTranslation() {
        var deferred = $q.defer();
        var url = '/api/customTranslation/customTranslation';
        $http.get(url, null).then(
            function success(response) {
                deferred.resolve(response.data);
            },
            function fail() {
                deferred.reject();
            });
        return deferred.promise;
    }

    function updateCustomTranslations(forceUpdate) {
        if (userService.isUserLoaded() === true) {
            if (userService.isAuthenticated()) {
                if (service.translateLoadPromise) {
                    return;
                }
                service.translateLoadPromise = (service.translationMap && !forceUpdate) ? $q.when({ translationMap: service.translationMap }) : loadCustomTranslation();
                service.translateLoadPromise.then(
                    (response) => {
                        service.translateLoadPromise = null;
                        service.translationMap = response.translationMap;
                        var langKey = $translate.use();
                        var translationMap;
                        if (response.translationMap[langKey]) {
                            try {
                                translationMap = angular.fromJson(response.translationMap[langKey]);
                            } catch (e) {
                                //
                            }
                        }
                        if (forceUpdate) {
                            var targetLocale = PUBLIC_PATH + 'locale/locale.constant-'+langKey+'.json'; //eslint-disable-line
                            $http.get(targetLocale, null).then(function success(response) {
                                var localeJson = response.data;
                                $translateProvider.translations(langKey, localeJson);
                                $translateProvider.translations(langKey, translationMap);
                            });
                        } else {
                            $translateProvider.translations(langKey, translationMap);
                        }
                    },
                    () => {
                        service.translateLoadPromise = null;
                    }
                )
            }
        } else {
            if (!service.userLoadedHandle) {
                service.userLoadedHandle = $rootScope.$on('userLoaded', function () {
                    service.userLoadedHandle();
                    service.userLoadedHandle = null;
                    updateCustomTranslations();
                });
            }
        }
    }

    function getCurrentCustomTranslation() {
        var deferred = $q.defer();
        var url = '/api/customTranslation/currentCustomTranslation';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveCustomTranslation(customTranslation) {
        var deferred = $q.defer();
        var url = '/api/customTranslation/customTranslation';
        $http.post(url, customTranslation).then(
            () => {
                updateCustomTranslations(true);
                deferred.resolve();
            },
            () => {
                deferred.reject();
            });
        return deferred.promise;
    }

    function downloadLocaleJson(langKey) {
        var deferred = $q.defer();
        var engLocale = PUBLIC_PATH + 'locale/locale.constant-en_US.json'; //eslint-disable-line
        $http.get(engLocale, null).then(function success(response) {
            var engJson = response.data;
            var targetLocale = PUBLIC_PATH + 'locale/locale.constant-'+langKey+'.json'; //eslint-disable-line
            var targetLocalePromise = langKey === 'en_US' ? $q.when({data: engLocale}) : $http.get(targetLocale, null);
            targetLocalePromise.then(function success(response) {
                var targetJson = response.data;
                var localeJson = angular.merge(engJson, targetJson);
                if (service.translationMap && service.translationMap[langKey]) {
                    var translationMap;
                    try {
                        translationMap = angular.fromJson(service.translationMap[langKey]);
                    } catch (e) {
                        //
                    }
                    if (translationMap) {
                        localeJson = angular.merge(localeJson, translationMap);
                    }
                }
                var data = angular.toJson(localeJson, 2);
                var fileName = 'locale-'+langKey;
                downloadJson(data, fileName);
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });

        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    /* eslint-disable no-undef, angular/window-service, angular/document-service */
    function downloadJson(data, filename) {
        if (!filename) {
            filename = 'download';
        }
        filename += '.' + 'json';
        var blob = new Blob([data], {type: 'text/json'});
        // FOR IE:
        if (window.navigator && window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            var e = document.createEvent('MouseEvents'),
                a = document.createElement('a');
            a.download = filename;
            a.href = window.URL.createObjectURL(blob);
            a.dataset.downloadurl = ['text/json', a.download, a.href].join(':');
            e.initEvent('click', true, false, window,
                0, 0, 0, 0, 0, false, false, false, false, 0, null);
            a.dispatchEvent(e);
        }
    }
    /* eslint-enable no-undef, angular/window-service, angular/document-service */
}
