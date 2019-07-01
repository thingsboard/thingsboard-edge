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

import './self-registration.scss';

/*@ngInject*/
export default function selfRegistrationController($scope, $rootScope, selfRegistrationService, $translate, $timeout,
                                                   toast, whiteLabelingService, types, securityTypes) {
    var vm = this;

    vm.securityTypes = securityTypes;
    vm.types = types;

    vm.selfRegistrationParams = {};

    initSelfRegistration();

    function initSelfRegistration() {
        selfRegistrationService.getSelfRegistrationParams().then(function (settings) {
            if (settings) {
                vm.selfRegistrationParams = settings;
                if (vm.selfRegistrationParams.domainName !== null)
                    vm.registerLink = selfRegistrationService.getRegistrationLink(settings.domainName);
                if (vm.selfRegistrationParams.signUpTextMessage !== null){
                    vm.selfRegistrationParams.signUpTextMessage = convertHTMLToText(vm.selfRegistrationParams.signUpTextMessage);
                }
                if (vm.selfRegistrationParams.permissions === null)
                    vm.selfRegistrationParams.permissions = [];

                vm.selfRegistrationForm.$setPristine();
                listedChangePermission();
            }
        });

        vm.tinyMceOptions = {
            plugins: ['textcolor colorpicker link table image imagetools code fullscreen'],
            menubar: "edit insert tools view format table",
            toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor | link | table | image | alignleft aligncenter alignright alignjustify  | numlist bullist outdent indent  | removeformat | code | fullscreen',
            font_formats: 'Helvetica Neue="Helvetica Neue",helvetica,arial,sans-serif;Helvetica=helvetica,arial,sans-serif;Times New Roman=times new roman,times,serif;Sans Serif=sans-serif,helvetica,arial;Arial=arial,helvetica,sans-serif;Courier New=courier new,courier,monospace;',
            fontsize_formats: '8pt 10pt 11pt 12pt 14pt 18pt 24pt 36pt',
            height: 280,
            autofocus: false,
            branding: false
        };
    }

    vm.save = save;
    vm.onActivationLinkCopied = onActivationLinkCopied;

    function onActivationLinkCopied(){
        toast.showSuccess($translate.instant('user.activation-link-copied-message'), 750, angular.element('#registration-link-content'), 'top left');
    }

    function listedChangePermission() {
        var listener = $scope.$watch("vm.selfRegistrationParams.permissions", function(newVal, prevVal) {
            if ($rootScope.loading && !angular.equals(newVal, prevVal)) {
                vm.selfRegistrationForm.$setDirty();
                listener();
            }
        }, true);
    }

    function save() {
        var saveObject = {};
        saveObject = angular.copy(vm.selfRegistrationParams);
        if(saveObject.signUpTextMessage !== "" && saveObject.signUpTextMessage !== null) {
            saveObject.signUpTextMessage = convertTextToHTML(saveObject.signUpTextMessage);
        }

        selfRegistrationService.saveSelfRegistrationParams(saveObject).then(function () {
            if (saveObject.domainName !== "")
                vm.registerLink = selfRegistrationService.getRegistrationLink(saveObject.domainName);
            vm.selfRegistrationForm.$setPristine();

            listedChangePermission();
        });
    }

    function convertTextToHTML(str){
        return str.replace(/\n/g, '<br/>');
    }

    function convertHTMLToText(str){
        return str.replace(/<br\s*[\/]?>/gi, "\n");
    }
}
