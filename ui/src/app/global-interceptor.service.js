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
/*@ngInject*/
export default function GlobalInterceptor($rootScope, $q, $injector) {

    var toast;
    var translate;
    var userService;
    var types;
    var http;

    var internalUrlPrefixes = [
        '/api/auth/token',
        '/api/plugins/rpc'
    ];

    var service = {
        request: request,
        requestError: requestError,
        response: response,
        responseError: responseError
    }

    return service;

    function getToast() {
        if (!toast) {
            toast = $injector.get("toast");
        }
        return toast;
    }

    function getTranslate() {
        if (!translate) {
            translate = $injector.get("$translate");
        }
        return translate;
    }

    function getUserService() {
        if (!userService) {
            userService = $injector.get("userService");
        }
        return userService;
    }

    function getTypes() {
        if (!types) {
            types = $injector.get("types");
        }
        return types;
    }

    function getHttp() {
        if (!http) {
            http = $injector.get("$http");
        }
        return http;
    }

    function rejectionErrorCode(rejection) {
        if (rejection && rejection.data && rejection.data.errorCode) {
            return rejection.data.errorCode;
        } else {
            return undefined;
        }
    }

    function isTokenBasedAuthEntryPoint(url) {
        return  url.startsWith('/api/') &&
               !url.startsWith(getTypes().entryPoints.login) &&
               !url.startsWith(getTypes().entryPoints.tokenRefresh) &&
               !url.startsWith(getTypes().entryPoints.nonTokenBased);
    }

    function refreshTokenAndRetry(request) {
        return getUserService().refreshJwtToken().then(function success() {
            getUserService().updateAuthorizationHeader(request.config.headers);
            return getHttp()(request.config);
        }, function fail(message) {
            $rootScope.$broadcast('unauthenticated');
            request.status = 401;
            request.data = {};
            request.data.message = message || getTranslate().instant('access.unauthorized');
            return $q.reject(request);
        });
    }

    function isInternalUrlPrefix(url) {
        for (var index in internalUrlPrefixes) {
            if (url.startsWith(internalUrlPrefixes[index])) {
                return true;
            }
        }
        return false;
    }

    function request(config) {
        var rejected = false;
        if (config.url.startsWith('/api/')) {
            var isLoading = !isInternalUrlPrefix(config.url);
            updateLoadingState(config, isLoading);
            if (isTokenBasedAuthEntryPoint(config.url)) {
                if (!getUserService().updateAuthorizationHeader(config.headers) &&
                    !getUserService().refreshTokenPending()) {
                    updateLoadingState(config, false);
                    rejected = true;
                    getUserService().clearJwtToken(false);
                    return $q.reject({ data: {message: getTranslate().instant('access.unauthorized')}, status: 401, config: config});
                } else if (!getUserService().isJwtTokenValid()) {
                    return $q.reject({ refreshTokenPending: true, config: config });
                }
            }
        }
        if (!rejected) {
            return config;
        }
    }

    function requestError(rejection) {
        if (rejection.config.url.startsWith('/api/')) {
            updateLoadingState(rejection.config, false);
        }
        return $q.reject(rejection);
    }

    function response(response) {
        if (response.config.url.startsWith('/api/')) {
            updateLoadingState(response.config, false);
        }
        return response;
    }

    function responseError(rejection) {
        if (rejection.config.url.startsWith('/api/')) {
            updateLoadingState(rejection.config, false);
        }
        var unhandled = false;
        var ignoreErrors = rejection.config.ignoreErrors;
        if (rejection.refreshTokenPending || rejection.status === 401) {
            var errorCode = rejectionErrorCode(rejection);
            if (rejection.refreshTokenPending || (errorCode && errorCode === getTypes().serverErrorCode.jwtTokenExpired)) {
                return refreshTokenAndRetry(rejection);
            } else {
                unhandled = true;
            }
        } else if (rejection.status === 403) {
            if (!ignoreErrors) {
                $rootScope.$broadcast('forbidden');
            }
        } else if (rejection.status === 0 || rejection.status === -1) {
            getToast().showError(getTranslate().instant('error.unable-to-connect'));
        } else if (!rejection.config.url.startsWith('/api/plugins/rpc')) {
            if (rejection.status === 404) {
                if (!ignoreErrors) {
                    getToast().showError(rejection.config.method + ": " + rejection.config.url + "<br/>" +
                        rejection.status + ": " + rejection.statusText);
                }
            } else {
                unhandled = true;
            }
        }

        if (unhandled && !ignoreErrors) {
            if (rejection.data && !rejection.data.message) {
                getToast().showError(rejection.data);
            } else if (rejection.data && rejection.data.message) {
                getToast().showError(rejection.data.message);
            } else {
                getToast().showError(getTranslate().instant('error.unhandled-error-code', {errorCode: rejection.status}));
            }
        }
        return $q.reject(rejection);
    }

    function updateLoadingState(config, isLoading) {
        if (!config || angular.isUndefined(config.ignoreLoading) || !config.ignoreLoading) {
            $rootScope.loading = isLoading;
        }
    }
}
