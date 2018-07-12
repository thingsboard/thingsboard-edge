/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import tinycolor from 'tinycolor2';

/* eslint-disable import/no-unresolved, import/default */

import paletteTemplate from './palette-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

const faviconTypes = ['image/x-icon', 'image/png', 'image/gif', 'image/vnd.microsoft.icon'];
const maxFaviconSize = 262144;
const maxLogoSize = 4194304;

/*@ngInject*/
export default function WhiteLabelingController($state, userService, $scope, $mdDialog, $document, $q,
                                                $translate, toast, whiteLabelingService, $mdTheming, $filter) {
    var vm = this;

    vm.isLoginWl = $state.current.data.isLoginWl;

    vm.maxFaviconSizeKb = maxFaviconSize / 1024;
    vm.maxLogoSizeKb = maxLogoSize / 1024;

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

    vm.faviconImageAdded = faviconImageAdded;
    vm.clearFaviconImage = clearFaviconImage;
    vm.logoImageAdded = logoImageAdded;
    vm.clearLogoImage = clearLogoImage;
    vm.onFormExit = onFormExit;

    vm.editPrimaryPalette = editPrimaryPalette;
    vm.editAccentPalette = editAccentPalette;

    loadWhiteLabelingParams();

    function loadWhiteLabelingParams() {
        var loadWlPromise = vm.isLoginWl ? whiteLabelingService.getCurrentLoginWhiteLabelParams() : whiteLabelingService.getCurrentWhiteLabelParams();
        loadWlPromise.then(
            (whiteLabelingParams) => {
                vm.whiteLabelingParams = whiteLabelingParams;
                if (!vm.whiteLabelingParams.paletteSettings) {
                    vm.whiteLabelingParams.paletteSettings = {};
                }
                updateCustomPalette('primaryPalette', vm.primaryPalettes);
                updateCustomPalette('accentPalette', vm.accentPalettes);
            }
        );
    }

    function faviconImageAdded($file) {
        var reader = new FileReader();
        reader.onload = function(event) {
            $scope.$apply(function() {
                var dataUrl = event.target.result;
                var type = extractType(dataUrl);
                if (type && faviconTypes.indexOf(type) > -1) {
                    vm.whiteLabelForm.$setDirty();
                    vm.whiteLabelingParams.favicon.url = dataUrl;
                    vm.whiteLabelingParams.favicon.type = extractTypeFromDataUrl(dataUrl);
                } else {
                    toast.showError($translate.instant('white-labeling.favicon-type-error'));
                }
            });
        };
        if ($file.file.size > maxFaviconSize) {
            toast.showError($translate.instant('white-labeling.favicon-size-error', {kbSize: vm.maxFaviconSizeKb}));
            return false;
        } else {
            reader.readAsDataURL($file.file);
        }
    }

    function extractTypeFromDataUrl(dataUrl) {
        var type;
        if (dataUrl) {
            var res = dataUrl.split(";");
            if (res && res.length) {
                res = res[0];
                res = res.split(":");
                if (res && res.length > 1) {
                    type = res[1];
                }
            }
        }
        return type;
    }

    function clearFaviconImage() {
        vm.whiteLabelForm.$setDirty();
        vm.whiteLabelingParams.favicon.url = null;
        vm.whiteLabelingParams.favicon.type = null;
        vm.whiteLabelingParams.faviconChecksum = null;
    }

    function logoImageAdded($file) {
        var reader = new FileReader();
        reader.onload = function(event) {
            $scope.$apply(function() {
                var dataUrl = event.target.result;
                var type = extractType(dataUrl);
                if (type && type.startsWith('image/')) {
                    vm.whiteLabelForm.$setDirty();
                    vm.whiteLabelingParams.logoImageUrl = dataUrl;
                } else {
                    toast.showError($translate.instant('white-labeling.logo-type-error'));
                }
            });
        };
        if ($file.file.size > maxLogoSize) {
            toast.showError($translate.instant('white-labeling.logo-size-error', {kbSize: vm.maxLogoSizeKb}));
            return false;
        } else {
            reader.readAsDataURL($file.file);
        }
    }

    function clearLogoImage() {
        vm.whiteLabelForm.$setDirty();
        vm.whiteLabelingParams.logoImageUrl = null;
        vm.whiteLabelingParams.logoImageChecksum = null;
    }

    function extractType(dataUrl) {
        var type;
        if (dataUrl) {
            var res = dataUrl.split(";");
            if (res && res.length) {
                res = res[0];
                res = res.split(":");
                if (res && res.length > 1) {
                    type = res[1];
                }
            }
        }
        return type;
    }

    function save() {
        var savePromise = vm.isLoginWl ? whiteLabelingService.saveLoginWhiteLabelParams(vm.whiteLabelingParams) :
                        whiteLabelingService.saveWhiteLabelParams(vm.whiteLabelingParams);
        savePromise.then(() => {
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