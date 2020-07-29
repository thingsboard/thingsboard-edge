/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.edge', [thingsboardTypes])
    .factory('edgeService', EdgeService)
    .name;

/*@ngInject*/
function EdgeService($q, $http, types) {

    var service = {
        getEdgeInfo: getEdgeInfo
    }

    return service;

    function getEdgeInfo(tenantId) {
        var deferred = $q.defer();
        var params = {
            entityType: types.entityType.tenant,
            entityId: tenantId,
            attributeScope: types.attributesScope.server.value,
            keys: ['edgeSettings'],
            config: {},
            query: { order: '', limit: 5, page: 1, search: null }
        };
        var promise = getEntityAttributesValues(params.entityType, params.entityId, params.attributeScope, params.keys, params.config);
        if (promise) {
            promise.then(
                function success(result) {
                    deferred.resolve(result[0]);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    //TODO replace backend solution
    function getEntityAttributesValues(entityType, entityId, attributeScope, keys, config) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/values/attributes/' + attributeScope;
        if (keys && keys.length) {
            url += '?keys=' + keys;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}


