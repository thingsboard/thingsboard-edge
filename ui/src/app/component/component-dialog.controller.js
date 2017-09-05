/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
/*@ngInject*/
export default function ComponentDialogController($mdDialog, $q, $scope, componentDescriptorService, types, utils, helpLinks, isAdd, isReadOnly, componentInfo) {

    var vm = this;

    vm.isReadOnly = isReadOnly;
    vm.isAdd = isAdd;
    vm.componentInfo = componentInfo;
    if (isAdd) {
        vm.componentInfo.component = {};
    }

    vm.componentHasSchema = false;
    vm.componentDescriptors = [];

    if (vm.componentInfo.component && !vm.componentInfo.component.configuration) {
        vm.componentInfo.component.configuration = {};
    }

    vm.helpLinkIdForComponent = helpLinkIdForComponent;
    vm.save = save;
    vm.cancel = cancel;

    $scope.$watch("vm.componentInfo.component.clazz", function (newValue, prevValue) {
        if (newValue != prevValue) {
            if (newValue && prevValue) {
                vm.componentInfo.component.configuration = {};
            }
            loadComponentDescriptor();
        }
    });

    var componentDescriptorsPromise =
        vm.componentInfo.type === types.componentType.action
            ? componentDescriptorService.getPluginActionsByPluginClazz(vm.componentInfo.pluginClazz)
            : componentDescriptorService.getComponentDescriptorsByType(vm.componentInfo.type);

    componentDescriptorsPromise.then(
        function success(componentDescriptors) {
            vm.componentDescriptors = componentDescriptors;
            if (vm.componentDescriptors.length === 1 && isAdd && !vm.componentInfo.component.clazz) {
                vm.componentInfo.component.clazz = vm.componentDescriptors[0].clazz;
            }
        },
        function fail() {
        }
    );

    loadComponentDescriptor();

    function loadComponentDescriptor () {
        if (vm.componentInfo.component.clazz) {
            componentDescriptorService.getComponentDescriptorByClazz(vm.componentInfo.component.clazz).then(
                function success(componentDescriptor) {
                    vm.componentDescriptor = componentDescriptor;
                    vm.componentHasSchema = utils.isDescriptorSchemaNotEmpty(vm.componentDescriptor.configurationDescriptor);
                },
                function fail() {
                }
            );
        } else {
            vm.componentHasSchema = false;
        }
    }

    function helpLinkIdForComponent() {
        switch (vm.componentInfo.type) {
            case types.componentType.filter: {
                return helpLinks.getFilterLink(vm.componentInfo.component);
            }
            case types.componentType.processor: {
                return helpLinks.getProcessorLink(vm.componentInfo.component);
            }
            case types.componentType.action: {
                return helpLinks.getPluginActionLink(vm.componentInfo.component);
            }

        }
    }


    function cancel () {
        $mdDialog.cancel();
    }

    function save () {
        $mdDialog.hide(vm.componentInfo.component);
    }

}
