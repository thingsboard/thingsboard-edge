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
export default angular.module('thingsboard.api.blobEntity', [])
    .factory('blobEntityService', BlobEntityService)
    .name;

/*@ngInject*/
function BlobEntityService($http, $q, $document, $window, customerService) {

    var service = {
        getBlobEntityInfo: getBlobEntityInfo,
        getBlobEntities: getBlobEntities,
        getBlobEntitiesByIds: getBlobEntitiesByIds,
        deleteBlobEntity: deleteBlobEntity,
        downloadBlobEntity: downloadBlobEntity
    };

    return service;

    function getBlobEntityInfo(blobEntityId, config) {
        var deferred = $q.defer();
        var url = '/api/blobEntity/info/' + blobEntityId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getBlobEntities(pageLink, type, applyCustomersInfo, config) {
        var deferred = $q.defer();
        var url = `/api/blobEntities?limit=${pageLink.limit}`;
        if (type) {
            url += '&type=' + type;
        }
        if (angular.isDefined(pageLink.startTime) && pageLink.startTime != null) {
            url += '&startTime=' + pageLink.startTime;
        }
        if (angular.isDefined(pageLink.endTime) && pageLink.endTime != null) {
            url += '&endTime=' + pageLink.endTime;
        }
        if (angular.isDefined(pageLink.idOffset) && pageLink.idOffset != null) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomersInfo(response.data.data).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getBlobEntitiesByIds(blobEntityIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<blobEntityIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += blobEntityIds[i];
        }
        var url = '/api/blobEntities?blobEntityIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var entities = response.data;
            entities.sort(function (entity1, entity2) {
                var id1 =  entity1.id.id;
                var id2 =  entity2.id.id;
                var index1 = blobEntityIds.indexOf(id1);
                var index2 = blobEntityIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(entities);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteBlobEntity(blobEntityId) {
        var deferred = $q.defer();
        var url = '/api/blobEntity/' + blobEntityId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function downloadBlobEntity(blobEntityId) {
        var deferred = $q.defer();
        var url = '/api/blobEntity/' + blobEntityId +  '/download';
        $http({
            method: 'GET',
            url: url,
            params: {},
            responseType: 'arraybuffer'
        }).success(function (data, status, headers) {
            headers = headers();
            var filename = headers['x-filename'];
            var contentType = headers['content-type'];
            var linkElement = $document[0].createElement('a');
            try {
                var blob = new Blob([data], { type: contentType }); //eslint-disable-line
                var url = $window.URL.createObjectURL(blob);
                linkElement.setAttribute('href', url);
                linkElement.setAttribute("download", filename);
                var clickEvent = new MouseEvent("click", { //eslint-disable-line
                    "view": $window,
                    "bubbles": true,
                    "cancelable": false
                });
                linkElement.dispatchEvent(clickEvent);
                deferred.resolve();
            } catch (ex) {
                deferred.reject(ex);
            }
        }).error(function (data) {
            deferred.reject(data);
        });
        return deferred.promise;
    }
}
