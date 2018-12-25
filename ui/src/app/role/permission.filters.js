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

/*@ngInject*/
export function HasGenericPermission(securityTypes, userPermissionsService) {
    return function (resource, operation) {
        return hasGenericPermission(resource, operation);
    };

    function hasGenericPermission(resource, operation) {
        if (angular.isArray(resource)) {
            return hasGenericResourcesPermission(resource, operation);
        } else if (angular.isArray(operation)) {
            return hasGenericOperationsPermission(resource, operation);
        } else {
            return userPermissionsService.hasGenericPermission(securityTypes.resource[resource], securityTypes.operation[operation]);
        }
    }

    function hasGenericResourcesPermission(resources, operation) {
        for (var i=0;i<resources.length;i++) {
            var resource = resources[i];
            if (!hasGenericPermission(resource, operation)) {
                return false;
            }
        }
        return true;
    }

    function hasGenericOperationsPermission(resource, operations) {
        for (var i=0;i<operations.length;i++) {
            var operation = operations[i];
            if (!hasGenericPermission(resource, operation)) {
                return false;
            }
        }
        return true;
    }

}

/*@ngInject*/
export function HasEntityGroupPermission(securityTypes, userPermissionsService) {
    return function (entityGroup, operation) {
        return userPermissionsService.hasEntityGroupPermission(securityTypes.operation[operation], entityGroup);
    }
}

/*@ngInject*/
export function HasGroupEntityPermission(securityTypes, userPermissionsService) {
    return function (entityGroup, operation) {
        return userPermissionsService.hasGroupEntityPermission(securityTypes.operation[operation], entityGroup);
    }
}
