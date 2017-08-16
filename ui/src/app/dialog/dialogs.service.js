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

/* eslint-disable import/no-unresolved, import/default */

import deviceCredentialsTemplate from './../device/device-credentials.tpl.html';
import assignDevicesToCustomerTemplate from './../device/assign-to-customer.tpl.html';
import assignAssetsToCustomerTemplate from './../asset/assign-to-customer.tpl.html';
import selectEntityGroupTemplate from './select-entity-group.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function Dialogs($q, $translate, $mdDialog, $document, deviceService, assetService, customerService) {


    var service = {
        manageDeviceCredentials: manageDeviceCredentials,
        assignDevicesToCustomer: assignDevicesToCustomer,
        unassignDeviceFromCustomer: unassignDeviceFromCustomer,
        unassignDevicesFromCustomer: unassignDevicesFromCustomer,
        makeDevicePublic: makeDevicePublic,
        assignAssetsToCustomer: assignAssetsToCustomer,
        unassignAssetFromCustomer: unassignAssetFromCustomer,
        unassignAssetsFromCustomer: unassignAssetsFromCustomer,
        makeAssetPublic: makeAssetPublic,
        selectEntityGroup: selectEntityGroup,
        confirm: confirm
    }

    return service;

    function manageDeviceCredentials($event, device, isReadOnly) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'ManageDeviceCredentialsController',
            controllerAs: 'vm',
            templateUrl: deviceCredentialsTemplate,
            locals: {deviceId: device.id.id, isReadOnly: isReadOnly},
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
        }, function () {
        });
    }

    function assignDevicesToCustomer($event, deviceIds) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        customerService.getCustomers({limit: pageSize, textSearch: ''}).then(
            function success(_customers) {
                var customers = {
                    pageSize: pageSize,
                    data: _customers.data,
                    nextPageLink: _customers.nextPageLink,
                    selection: null,
                    hasNext: _customers.hasNext,
                    pending: false
                };
                if (customers.hasNext) {
                    customers.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AssignDeviceToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: assignDevicesToCustomerTemplate,
                    locals: {deviceIds: deviceIds, customers: customers},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    deferred.resolve();
                }, function () {
                    deferred.reject();
                });
            },
            function fail() {
                deferred.reject();
            });
        return deferred.promise;
    }

    function unassignDeviceFromCustomer($event, device, isPublic) {
        var deferred = $q.defer();
        var title;
        var content;
        var label;
        if (isPublic) {
            title = $translate.instant('device.make-private-device-title', {deviceName: device.name});
            content = $translate.instant('device.make-private-device-text');
            label = $translate.instant('device.make-private');
        } else {
            title = $translate.instant('device.unassign-device-title', {deviceName: device.name});
            content = $translate.instant('device.unassign-device-text');
            label = $translate.instant('device.unassign-device');
        }
        confirm($event, title, content, label).then(
            () => {
                deviceService.unassignDeviceFromCustomer(device.id.id).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function unassignDevicesFromCustomer($event, deviceIds) {
        var deferred = $q.defer();
        confirm($event, $translate.instant('device.unassign-devices-title', {count: deviceIds.length}, 'messageformat'),
                        $translate.instant('device.unassign-devices-text'),
                        $translate.instant('device.unassign-device')).then(
            () => {
                var tasks = [];
                deviceIds.forEach((deviceId) => {
                    tasks.push(deviceService.unassignDeviceFromCustomer(deviceId));
                });
                $q.all(tasks).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function makeDevicePublic($event, device) {
        var deferred = $q.defer();
        confirm($event, $translate.instant('device.make-public-device-title', {deviceName: device.name}),
                        $translate.instant('device.make-public-device-text'),
                        $translate.instant('device.make-public')).then(
            () => {
                deviceService.makeDevicePublic(device.id.id).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function assignAssetsToCustomer($event, assetIds) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        customerService.getCustomers({limit: pageSize, textSearch: ''}).then(
            function success(_customers) {
                var customers = {
                    pageSize: pageSize,
                    data: _customers.data,
                    nextPageLink: _customers.nextPageLink,
                    selection: null,
                    hasNext: _customers.hasNext,
                    pending: false
                };
                if (customers.hasNext) {
                    customers.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AssignAssetToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: assignAssetsToCustomerTemplate,
                    locals: {assetIds: assetIds, customers: customers},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    deferred.resolve();
                }, function () {
                    deferred.reject();
                });
            },
            function fail() {
                deferred.reject();
            });
        return deferred.promise;
    }

    function unassignAssetFromCustomer($event, asset, isPublic) {
        var deferred = $q.defer();
        var title;
        var content;
        var label;
        if (isPublic) {
            title = $translate.instant('asset.make-private-asset-title', {assetName: asset.name});
            content = $translate.instant('asset.make-private-asset-text');
            label = $translate.instant('asset.make-private');
        } else {
            title = $translate.instant('asset.unassign-asset-title', {assetName: asset.name});
            content = $translate.instant('asset.unassign-asset-text');
            label = $translate.instant('asset.unassign-asset');
        }
        confirm($event, title, content, label).then(
            () => {
                assetService.unassignAssetFromCustomer(asset.id.id).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function unassignAssetsFromCustomer($event, assetIds) {
        var deferred = $q.defer();
        confirm($event, $translate.instant('asset.unassign-assets-title', {count: assetIds.length}, 'messageformat'),
            $translate.instant('asset.unassign-assets-text'),
            $translate.instant('asset.unassign-asset')).then(
            () => {
                var tasks = [];
                assetIds.forEach((assetId) => {
                    tasks.push(assetService.unassignAssetFromCustomer(assetId));
                });
                $q.all(tasks).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function makeAssetPublic($event, asset) {
        var deferred = $q.defer();
        confirm($event, $translate.instant('asset.make-public-asset-title', {assetName: asset.name}),
            $translate.instant('asset.make-public-asset-text'),
            $translate.instant('asset.make-public')).then(
            () => {
                assetService.makeAssetPublic(asset.id.id).then(
                    () => {
                        deferred.resolve();
                    }
                );
            }
        );
        return deferred.promise;
    }

    function selectEntityGroup($event, targetGroupType, selectEntityGroupTitle,
                               confirmSelectTitle, placeholderText, notFoundText, requiredText, onEntityGroupSelected, excludeGroupIds) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'SelectEntityGroupController',
            controllerAs: 'vm',
            templateUrl: selectEntityGroupTemplate,
            locals: {targetGroupType: targetGroupType,
                selectEntityGroupTitle: selectEntityGroupTitle,
                confirmSelectTitle: confirmSelectTitle,
                placeholderText: placeholderText,
                notFoundText: notFoundText,
                requiredText: requiredText,
                onEntityGroupSelected: onEntityGroupSelected,
                excludeGroupIds: excludeGroupIds
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then((targetEntityGroupId) => {
            deferred.resolve(targetEntityGroupId);
        }, () => {
            deferred.reject();
        });
        return deferred.promise;
    }

    function confirm($event, title, content, label) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        return $mdDialog.show(confirm);
    }

}
