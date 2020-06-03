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
/* eslint-disable import/no-unresolved, import/default */

import attributeDialogEditJsonTemplate from './attribute-dialog-edit-json.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import AttributeDialogEditJsonController from './attribute-dialog-edit-json.controller';

/*@ngInject*/
export default function AddAttributeDialogController($scope, $mdDialog, types, attributeService, entityType, entityId, attributeScope) {

    let vm = this;

    vm.attribute = {};

    vm.valueTypes = types.valueType;

    vm.valueType = types.valueType.string;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        $scope.theForm.$setPristine();
        attributeService.saveEntityAttributes(entityType, entityId, attributeScope, [vm.attribute]).then(
            function success() {
                $mdDialog.hide();
            }
        );
    }

    $scope.$watch('vm.valueType', function () {
        if (vm.valueType === types.valueType.boolean) {
            vm.attribute.value = false;
        } else if (vm.valueType === types.valueType.json) {
            vm.attribute.value = {};
        } else {
            vm.attribute.value = null;
        }
    });

    vm.addJSON = ($event) => {
        showJsonDialog($event, vm.attribute.value, false).then((response) => {
            vm.attribute.value = response;
        })
    };

    function showJsonDialog($event, jsonValue, readOnly) {
        if ($event) {
            $event.stopPropagation();
        }
        return $mdDialog.show({
            controller: AttributeDialogEditJsonController,
            controllerAs: 'vm',
            templateUrl: attributeDialogEditJsonTemplate,
            locals: {
                jsonValue: jsonValue,
                readOnly: readOnly
            },
            targetEvent: $event,
            fullscreen: true,
            multiple: true,
        });
    }
}
