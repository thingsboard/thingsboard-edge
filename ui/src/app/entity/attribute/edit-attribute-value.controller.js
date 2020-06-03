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
/* eslint-enable import/no-unresolved, import/default */

import AttributeDialogEditJsonController from "./attribute-dialog-edit-json.controller";
import attributeDialogEditJsonTemplate from "./attribute-dialog-edit-json.tpl.html";

/*@ngInject*/
export default function EditAttributeValueController($scope, $mdDialog, $q, $element, $document, types, attributeValue, save) {

    $scope.valueTypes = types.valueType;

    $scope.model = {
        value: attributeValue
    };

    if ($scope.model.value === true || $scope.model.value === false) {
        $scope.valueType = types.valueType.boolean;
    } else if (angular.isNumber($scope.model.value)) {
        if ($scope.model.value.toString().indexOf('.') == -1) {
            $scope.valueType = types.valueType.integer;
        } else {
            $scope.valueType = types.valueType.double;
        }
    } else if (angular.isObject($scope.model.value)) {
        $scope.valueType = types.valueType.json;
    } else {
        $scope.valueType = types.valueType.string;
    }

    $scope.submit = submit;
    $scope.dismiss = dismiss;
    $scope.editJSON = editJSON;

    function dismiss() {
        $element.remove();
    }

    function update() {
        if ($scope.editDialog.$invalid) {
            return $q.reject();
        }
        if (angular.isFunction(save)) {
            return $q.when(save($scope.model));
        }
        return $q.resolve();
    }

    function submit() {
        update().then(function () {
            $scope.dismiss();
        });
    }


    $scope.$watch('valueType', function (newVal, prevVal) {
        if (newVal !== prevVal) {
            if ($scope.valueType === types.valueType.boolean) {
                $scope.model.value = false;
            } else if ($scope.valueType === types.valueType.json) {
                $scope.model.value = {};
            } else {
                $scope.model.value = null;
            }
        }
    });

    function editJSON($event) {
        $scope.hideDialog = true;
        showJsonDialog($event, $scope.model.value, false).then((response) => {
            $scope.hideDialog = false;
            if (!angular.equals(response, $scope.model.value)) {
                $scope.editDialog.$setDirty();
            }
            $scope.model.value = response;
        })
    }

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
            multiple: true
        });
    }
}
