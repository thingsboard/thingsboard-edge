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
import './import-dialog.scss';

/*@ngInject*/
export default function ImportDialogController($scope, $mdDialog, toast, importTitle, importFileLabel) {

    var vm = this;

    vm.cancel = cancel;
    vm.importFromJson = importFromJson;
    vm.fileAdded = fileAdded;
    vm.clearFile = clearFile;

    vm.importTitle = importTitle;
    vm.importFileLabel = importFileLabel;


    function cancel() {
        $mdDialog.cancel();
    }

    function fileAdded($file) {
        if ($file.getExtension() === 'json') {
            var reader = new FileReader();
            reader.onload = function(event) {
                $scope.$apply(function() {
                    if (event.target.result) {
                        $scope.theForm.$setDirty();
                        var importJson = event.target.result;
                        if (importJson && importJson.length > 0) {
                            try {
                                vm.importData = angular.fromJson(importJson);
                                vm.fileName = $file.name;
                            } catch (err) {
                                vm.fileName = null;
                                toast.showError(err.message);
                            }
                        }
                    }
                });
            };
            reader.readAsText($file.file);
        }
    }

    function clearFile() {
        $scope.theForm.$setDirty();
        vm.fileName = null;
        vm.importData = null;
    }

    function importFromJson() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(vm.importData);
    }
}
