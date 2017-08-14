/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*@ngInject*/
export default function SelectEntityGroupController($scope, $mdDialog,
                                                    targetGroupType, selectEntityGroupTitle, confirmSelectTitle, placeholderText,
                                                    notFoundText, requiredText, onEntityGroupSelected, excludeGroupIds) {

    var vm = this;

    vm.targetGroupType = targetGroupType;
    vm.selectEntityGroupTitle = selectEntityGroupTitle;
    vm.confirmSelectTitle = confirmSelectTitle;
    vm.placeholderText = placeholderText;
    vm.notFoundText = notFoundText;
    vm.requiredText = requiredText;
    vm.onEntityGroupSelected = onEntityGroupSelected;
    vm.excludeGroupIds = excludeGroupIds;

    vm.selectEntityGroup = selectEntityGroup;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function selectEntityGroup() {
        $scope.theForm.$setPristine();
        if (onEntityGroupSelected) {
            onEntityGroupSelected(vm.targetEntityGroupId).then(
                () => {
                    $mdDialog.hide();
                }
            );
        } else {
            $mdDialog.hide(vm.targetEntityGroupId);
        }
    }
}
