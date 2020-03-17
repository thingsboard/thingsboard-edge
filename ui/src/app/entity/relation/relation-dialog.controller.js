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
import 'brace/ext/language_tools';
import 'brace/ext/searchbox';
import 'brace/mode/json';
import 'brace/theme/github';
import beautify from 'js-beautify';

import './relation-dialog.scss';

const js_beautify = beautify.js;

/*@ngInject*/
export default function RelationDialogController($scope, $mdDialog, types, entityRelationService, isAdd, direction, relation, showingCallback) {

    var vm = this;

    vm.types = types;
    vm.isAdd = isAdd;
    vm.direction = direction;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element);
    }

    vm.relationAdditionalInfoOptions = {
        useWrapMode: false,
        mode: 'json',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.editor = _ace;
        }
    };

    vm.relation = relation;
    if (vm.isAdd) {
        vm.relation.type = types.entityRelationType.contains;
        vm.targetEntityId = {};
    } else {
        if (vm.direction == vm.types.entitySearchDirection.from) {
            vm.targetEntityId = vm.relation.to;
        } else {
            vm.targetEntityId = vm.relation.from;
        }
    }

    vm.save = save;
    vm.cancel = cancel;

    vm.additionalInfo = '';

    if (vm.relation.additionalInfo) {
        vm.additionalInfo = angular.toJson(vm.relation.additionalInfo);
        vm.additionalInfo = js_beautify(vm.additionalInfo, {indent_size: 4});
    }

    $scope.$watch('vm.additionalInfo', () => {
        $scope.theForm.$setValidity("additionalInfo", true);
    });

    function updateEditorSize(element) {
        var newHeight = 200;
        angular.element('#tb-relation-additional-info', element).height(newHeight.toString() + "px");
        vm.editor.resize();
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        if (vm.isAdd) {
            if (vm.direction == vm.types.entitySearchDirection.from) {
                vm.relation.to = vm.targetEntityId;
            } else {
                vm.relation.from = vm.targetEntityId;
            }
        }
        $scope.theForm.$setPristine();

        var valid = true;
        if (vm.additionalInfo && vm.additionalInfo.length) {
            try {
                vm.relation.additionalInfo = angular.fromJson(vm.additionalInfo);
            } catch(e) {
                valid = false;
            }
        } else {
            vm.relation.additionalInfo = null;
        }

        $scope.theForm.$setValidity("additionalInfo", valid);

        if (valid) {
            entityRelationService.saveRelation(vm.relation).then(
                function success() {
                    $mdDialog.hide();
                }
            );
        }
    }

}
