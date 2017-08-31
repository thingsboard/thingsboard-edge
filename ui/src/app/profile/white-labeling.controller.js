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
export default function WhiteLabelingController(userService, $scope, whiteLabelingService/*, $document, $mdDialog, $translate*/) {
    var vm = this;

    vm.whiteLabelingParams = {};

    vm.save = save;
    vm.preview = preview;

    vm.logoImageAdded = logoImageAdded;
    vm.clearLogoImage = clearLogoImage;
    vm.onFormExit = onFormExit;

    loadWhiteLabelingParams();

    function loadWhiteLabelingParams() {
        whiteLabelingService.getCurrentWhiteLabelParams().then(
            (whiteLabelingParams) => {
                vm.whiteLabelingParams = whiteLabelingParams;
            }
        );
    }

    function logoImageAdded($file) {
        var reader = new FileReader();
        reader.onload = function(event) {
            $scope.$apply(function() {
                if (event.target.result && event.target.result.startsWith('data:image/')) {
                    vm.whiteLabelForm.$setDirty();
                    vm.whiteLabelingParams.logoImageUrl = event.target.result;
                }
            });
        };
        reader.readAsDataURL($file.file);
    }

    function clearLogoImage() {
        vm.whiteLabelForm.$setDirty();
        vm.whiteLabelingParams.logoImageUrl = null;
    }

    function save() {
        whiteLabelingService.saveWhiteLabelParams(vm.whiteLabelingParams).then(() => {
            vm.whiteLabelForm.$setPristine();
        });
    }

    function preview() {
        whiteLabelingService.whiteLabelPreview(vm.whiteLabelingParams);
    }

    function onFormExit() {
        whiteLabelingService.cancelWhiteLabelPreview();
    }

}
