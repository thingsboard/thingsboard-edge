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

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function Dialogs($q, $translate, $mdDialog, $document, customerService) {


    var service = {
        manageDeviceCredentials: manageDeviceCredentials,
        assignDevicesToCustomer: assignDevicesToCustomer
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

/*    function selectEntity($event, targetEntityType, selectEntityTitle,
                          confirmSelectTitle, placeholderText, notFoundText, requiredText, onEntitySelected) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'SelectEntityController',
            controllerAs: 'vm',
            templateUrl: selectEntityTemplate,
            locals: {targetEntityType: targetEntityType,
                selectEntityTitle: selectEntityTitle,
                confirmSelectTitle: confirmSelectTitle,
                placeholderText: placeholderText,
                notFoundText: notFoundText,
                requiredText: requiredText,
                onEntitySelected: onEntitySelected
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then((targetEntityId) => {
            deferred.resolve(targetEntityId);
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
    }*/

}
