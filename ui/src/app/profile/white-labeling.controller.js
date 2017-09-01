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

import tinycolor from 'tinycolor2';

/* eslint-disable import/no-unresolved, import/default */

import paletteTemplate from './palette-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WhiteLabelingController(userService, $scope, $mdDialog, $document, $q, whiteLabelingService, $mdTheming, $filter) {
    var vm = this;

    vm.whiteLabelingParams = {};
    vm.primaryPalettes = [];
    vm.accentPalettes = [];

    var palettes = [];
    for (var paletteKey in $mdTheming.PALETTES) {
        if (!paletteKey.startsWith('tb-') && !paletteKey.startsWith('custom-')) {
            var palette = {
                type: paletteKey
            };
            palettes.push(palette);
        }
    }
    vm.primaryPalettes = angular.copy(palettes);
    vm.accentPalettes = angular.copy(palettes);

    vm.paletteName = (palette) => {
        if (palette) {
            return palette.type.toUpperCase().replace('-', ' ');
        } else {
            return "";
        }
    };

    vm.paletteStyle = (palette) => {
        if (palette && palette.type) {
            var key = palette.type == 'custom' ? palette.extends : palette.type;
            var paletteInfo = $mdTheming.PALETTES[key];
            var hex = palette.colors && palette.colors['500']
                    ? palette.colors['500'] : paletteInfo['500'].hex;
            var contrast = paletteInfo['500'].contrast;
            return {
                backgroundColor: hex,
                color: rgba(contrast)
            };
        } else {
            return {};
        }
    };

    vm.paletteTypeChanged = (paletteType, palettes) => {
        var palette = vm.whiteLabelingParams.paletteSettings[paletteType];
        if (palette && palette.type == 'custom') {
            var customPaletteResult = $filter('filter')(palettes, {type: 'custom'}, true);
            if (customPaletteResult && customPaletteResult.length) {
                vm.whiteLabelingParams.paletteSettings[paletteType] = angular.copy(customPaletteResult[0]);
            }
        } else if (palette) {
            delete palette.extends;
            delete palette.colors;
        }
    };

    vm.save = save;
    vm.preview = preview;

    vm.logoImageAdded = logoImageAdded;
    vm.clearLogoImage = clearLogoImage;
    vm.onFormExit = onFormExit;

    vm.editPrimaryPalette = editPrimaryPalette;
    vm.editAccentPalette = editAccentPalette;

    loadWhiteLabelingParams();

    function loadWhiteLabelingParams() {
        whiteLabelingService.getCurrentWhiteLabelParams().then(
            (whiteLabelingParams) => {
                vm.whiteLabelingParams = whiteLabelingParams;
                updateCustomPalette('primaryPalette', vm.primaryPalettes);
                updateCustomPalette('accentPalette', vm.accentPalettes);
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

    function editPrimaryPalette($event) {
        showEditPaletteDialog($event, vm.whiteLabelingParams.paletteSettings.primaryPalette)
            .then((colors) => {
                updatePaletteColors(vm.whiteLabelingParams.paletteSettings.primaryPalette, colors, vm.primaryPalettes);
            });
    }

    function editAccentPalette($event) {
        showEditPaletteDialog($event, vm.whiteLabelingParams.paletteSettings.accentPalette)
            .then((colors) => {
                updatePaletteColors(vm.whiteLabelingParams.paletteSettings.accentPalette, colors, vm.accentPalettes);
            });
    }

    function updatePaletteColors(palette, colors, palettes) {
        if (colors) {
            palette.colors = colors;
            if (palette.type != 'custom') {
                palette.extends = palette.type;
                palette.type = 'custom';
            }
            var customPaletteResult = $filter('filter')(palettes, {type: 'custom'}, true);
            if (!customPaletteResult || !customPaletteResult.length) {
                palettes.push(angular.copy(palette));
            } else {
                var index = palettes.indexOf(customPaletteResult[0]);
                if (index > -1) {
                    palettes[index] = angular.copy(palette);
                }
            }
        } else {
            delete palette.colors;
            if (palette.type == 'custom') {
                palette.type = palette.extends;
                delete palette.extends;
            }
        }
    }

    function updateCustomPalette(paletteType, palettes) {
        var palette = vm.whiteLabelingParams.paletteSettings[paletteType];
        if (palette && palette.type == 'custom') {
            var customPaletteResult = $filter('filter')(palettes, {type: 'custom'}, true);
            if (!customPaletteResult || !customPaletteResult.length) {
                palettes.push(angular.copy(palette));
            } else {
                var index = palettes.indexOf(customPaletteResult[0]);
                if (index > -1) {
                    palettes[index] = angular.copy(palette);
                }
            }
        }
    }

    function showEditPaletteDialog($event, palette) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: PaletteDialogController,
            controllerAs: 'vm',
            templateUrl: paletteTemplate,
            parent: angular.element($document[0].body),
            locals: {
                palette: angular.copy(palette)
            },
            fullscreen: true,
            skipHide: true,
            targetEvent: $event
        }).then((colors) => {
            if (angular.equals(colors, {})) {
                colors = null;
            }
            vm.whiteLabelForm.$setDirty();
            deferred.resolve(colors);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

}

function PaletteDialogController($scope, $mdDialog, $mdTheming, $mdColorPicker, $translate, palette) {

    var vm = this;

    vm.palette = palette;
    vm.colors = palette.colors || {};

    var key = palette.type == 'custom' ? palette.extends : palette.type;

    vm.paletteInfo = $mdTheming.PALETTES[key];
    for (var hue in vm.paletteInfo) {
        if (!vm.colors[hue]) {
            var hex = vm.paletteInfo[hue].hex;
            vm.colors[hue] = hex;
        }
    }

    vm.hueStyle = (hue) => {
        if (hue) {
            var hex = vm.colors[hue];
            var contrast = vm.paletteInfo[hue].contrast;
            return {
                backgroundColor: hex,
                color: rgba(contrast)
            };
        } else {
            return {};
        }
    };

    vm.hueName = (hue) => {
        if (hue == '500') {
            return $translate.instant('white-labeling.primary-background');
        }
        if (hue == '600') {
            return $translate.instant('white-labeling.secondary-background');
        }
        if (hue == '300') {
            return $translate.instant('white-labeling.hue1');
        }
        if (hue == '800') {
            return $translate.instant('white-labeling.hue2');
        }
        if (hue == 'A100') {
            return $translate.instant('white-labeling.hue3');
        }
        return '';
    };

    vm.editColor = function ($event, hue) {

        $mdColorPicker.show({
            value: vm.colors[hue],
            defaultValue: vm.colors[hue],
            random: tinycolor.random(),
            clickOutsideToClose: false,
            hasBackdrop: false,
            skipHide: true,
            preserveScope: false,

            mdColorAlphaChannel: false,
            mdColorSpectrum: true,
            mdColorSliders: true,
            mdColorGenericPalette: false,
            mdColorMaterialPalette: true,
            mdColorHistory: false,
            mdColorDefaultTab: 2,

            $event: $event
        }).then(function (color) {
            vm.colors[hue] = tinycolor(color).toHexString();
            $scope.theForm.$setDirty();
        });
    }

    vm.savePalette = savePalette;
    vm.cancel = cancel;

    function normalizeColors(colors) {
        for (var hue in colors) {
            var origHex = vm.paletteInfo[hue].hex;
            if (vm.colors[hue] == origHex) {
                delete vm.colors[hue];
            }
        }
        return colors;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function savePalette() {
        $scope.theForm.$setPristine();
        normalizeColors();
        $mdDialog.hide(normalizeColors(vm.colors));
    }
}

function rgba(rgbArray) {
    if ( !rgbArray ) return "rgb('0,0,0')";
    var opacity;
    if (rgbArray.length == 4) {
        rgbArray = angular.copy(rgbArray);
        opacity ? rgbArray.pop() : opacity = rgbArray.pop();
    }
    return opacity && (typeof opacity == 'number' || (typeof opacity == 'string' && opacity.length)) ?
    'rgba(' + rgbArray.join(',') + ',' + opacity + ')' :
    'rgb(' + rgbArray.join(',') + ')';
}