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
export default angular.module('thingsboard.api.componentDescriptor', [])
    .factory('componentDescriptorService', ComponentDescriptorService).name;

/*@ngInject*/
function ComponentDescriptorService($http, $q) {

    var componentsByType = {};
    var componentsByClazz = {};

    var service = {
        getComponentDescriptorsByTypes: getComponentDescriptorsByTypes
    }

    return service;

    function getComponentDescriptorsByTypes(componentTypes, ruleChainType) {
        var deferred = $q.defer();
        var result = [];
        if (!componentsByType[ruleChainType]) {
            componentsByType[ruleChainType] = {};
        }
        for (var i=componentTypes.length-1;i>=0;i--) {
            var componentType = componentTypes[i];
            if (componentsByType[ruleChainType][componentType]) {
                result = result.concat(componentsByType[ruleChainType][componentType]);
                componentTypes.splice(i, 1);
            }
        }
        if (!componentTypes.length) {
            deferred.resolve(result);
        } else {
            var url = '/api/components?componentTypes=' + componentTypes.join(',') + '&ruleChainType=' + ruleChainType;
            $http.get(url, null).then(function success(response) {
                var components = response.data;
                for (var i = 0; i < components.length; i++) {
                    var component = components[i];
                    var componentsList = componentsByType[ruleChainType][component.type];
                    if (!componentsList) {
                        componentsList = [];
                        componentsByType[ruleChainType][component.type] = componentsList;
                    }
                    componentsList.push(component);
                    componentsByClazz[component.clazz] = component;
                }
                result = result.concat(components);
                deferred.resolve(components);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred.promise;
    }
}
