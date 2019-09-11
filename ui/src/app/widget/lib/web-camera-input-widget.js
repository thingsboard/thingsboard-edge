/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import './web-camera-input-widget.scss';

/* eslint-disable import/no-unresolved, import/default */
import webCameraWidgetTemplate from './web-camera-input-widget.tpl.html';
/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.webCameraWidget', [])
    .directive('tbWebCameraWidget', webCameraWidget)
    .name;

/*@ngInject*/
function webCameraWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: WebCameraWidgetController,
        controllerAs: 'vm',
        templateUrl: webCameraWidgetTemplate
    };
}

function WebCameraWidgetController($element, $scope, $window, types, utils, attributeService, Fullscreen) {
    let vm = this;

    vm.videoInput = [];
    vm.videoDevice = "";
    vm.previewPhoto = "";
    vm.isShowCamera = false;
    vm.isPreviewPhoto = false;

    let streamDevice = null;
    let indexWebCamera = 0;
    let videoElement = null;
    let canvas = null;
    let photoCamera = null;
    let dataKeyType = "";

    vm.getStream = getStream;
    vm.createPhoto = createPhoto;
    vm.takePhoto = takePhoto;
    vm.switchWebCamera = switchWebCamera;
    vm.cancelPhoto = cancelPhoto;
    vm.closeCamera = closeCamera;
    vm.savePhoto = savePhoto;

    vm.isEntityDetected = false;
    vm.dataKeyDetected = false;
    vm.isCameraSupport = false;
    vm.isDeviceDetect = false;

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx && vm.ctx.datasources && vm.ctx.datasources.length) {
            let datasource = vm.ctx.datasources[0];
            if (datasource.type === types.datasourceType.entity) {
                if (datasource.entityType && datasource.entityId) {
                    if (vm.ctx.settings.widgetTitle && vm.ctx.settings.widgetTitle.length) {
                        $scope.titleTemplate = utils.customTranslation(vm.ctx.settings.widgetTitle, vm.ctx.settings.widgetTitle);
                    } else {
                        $scope.titleTemplate = vm.ctx.widgetConfig.title;
                    }
                    vm.isEntityDetected = true;
                }
            }
            if (datasource.dataKeys.length) {
                $scope.currentKey = datasource.dataKeys[0].name;
                dataKeyType = datasource.dataKeys[0].type;
                vm.dataKeyDetected = true;
            }
            if (hasGetUserMedia()) {
                vm.isCameraSupport = true;
                getDevices().then(gotDevices).then(() => {
                    vm.isDeviceDetect = !!vm.videoInput.length;
                });
            }
        }
    });

    function hasGetUserMedia() {
        return !!($window.navigator.mediaDevices && $window.navigator.mediaDevices.getUserMedia);
    }

    function takePhoto(){
        vm.isShowCamera = true;
        videoElement = $element[0].querySelector('#videoStream');
        photoCamera = $element[0].querySelector('#photoCamera');
        canvas = $element[0].querySelector('canvas');
        Fullscreen.enable(photoCamera);
        getStream();
    }

    function cancelPhoto() {
        vm.isPreviewPhoto = false;
        vm.previewPhoto = "";
    }

    function switchWebCamera() {
        indexWebCamera = (indexWebCamera+1)%vm.videoInput.length;
        vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        getStream();
    }

    function getDevices() {
        return $window.navigator.mediaDevices.enumerateDevices();
    }

    function gotDevices(deviceInfos) {
        for (const deviceInfo of deviceInfos) {
            let device = {
                deviceId: deviceInfo.deviceId,
                label: ""
            };
            if (deviceInfo.kind === 'videoinput') {
                device.label = deviceInfo.label || `Camera ${vm.videoInput.length + 1}`;
                vm.videoInput.push(device);
            }
        }
    }

    function getStream() {
        if (streamDevice !== null) {
            streamDevice.getTracks().forEach(track => {
                track.stop();
            });
        }
        const constraints = {
            video: {deviceId: vm.videoDevice !== "" ? {exact: vm.videoDevice} : undefined}
        };
        return $window.navigator.mediaDevices.getUserMedia(constraints).then(gotStream);
    }

    function gotStream(stream) {
        streamDevice = stream;
        if(vm.videoDevice === ""){
            indexWebCamera = vm.videoInput.findIndex(option => option.label === stream.getVideoTracks()[0].label);
            indexWebCamera = indexWebCamera === -1 ? 0 : indexWebCamera;
            vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        }
        videoElement.srcObject = stream;
    }

    function createPhoto() {
        canvas.width = videoElement.videoWidth;
        canvas.height = videoElement.videoHeight;
        canvas.getContext('2d').drawImage(videoElement, 0, 0);
        vm.previewPhoto = canvas.toDataURL('image/png');
        vm.isPreviewPhoto = true;
    }

    function closeCamera(){
        Fullscreen.cancel(photoCamera);
        vm.isShowCamera = false;
        if (streamDevice !== null) {
            streamDevice.getTracks().forEach(track => {
                track.stop();
            });
        }
        streamDevice = null;
        videoElement.srcObject = null;
    }

    function savePhoto(){
        let promiseData = null;
        let datasource = vm.ctx.datasources[0];
        let saveData = [{
            key: datasource.dataKeys[0].name,
            value: vm.previewPhoto
        }];
        if(dataKeyType === types.dataKeyType.attribute){
            promiseData = attributeService.saveEntityAttributes(datasource.entityType, datasource.entityId, types.attributesScope.server.value, saveData);
        } else if(dataKeyType === types.dataKeyType.timeseries){
            promiseData = attributeService.saveEntityTimeseries(datasource.entityType, datasource.entityId, "scope", saveData);
        }
        promiseData.then(()=>{
            vm.isPreviewPhoto = false;
            closeCamera();
        })
    }
}
