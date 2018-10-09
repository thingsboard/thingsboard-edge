/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
function CustomTranslationService($rootScope, $q, $http, $translateProvider) {

    var service = {
        updateCustomTranslations: updateCustomTranslations,
        getCurrentCustomTranslation: getCurrentCustomTranslation,
        saveCustomTranslation: saveCustomTranslation
    };

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

    function updateCustomTranslations() {
        var deferred = $q.defer();
        loadCustomTranslation().then(
            (response) => {
                Object.keys(response.translationMap).forEach(function(key) {
                    if (response.translationMap[key]) {
                        var translationMap;
                        try {
                            translationMap = angular.fromJson(response.translationMap[key]);
                        } catch (e) {
                            //
                        }
                        $translateProvider.translations(key, translationMap);
                    }
                });
                deferred.resolve();
            },
            () => {
                deferred.reject();
            }
        )
        return deferred.promise;
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
                updateCustomTranslations().then(
                    () => {
                        deferred.resolve();
                    },
                    () => {
                        deferred.reject();
                    }
                );
            },
            () => {
                deferred.reject();
            });
        return deferred.promise;
    }
}
