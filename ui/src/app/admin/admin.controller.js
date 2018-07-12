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
import './mail-template-settings.scss';

/*@ngInject*/
export default function AdminController(adminService, userService, toast, $scope, $rootScope, $state, $translate, types) {

    var vm = this;
    vm.types = types;
    vm.save = save;
    vm.sendTestMail = sendTestMail;
    vm.isTenantAdmin = isTenantAdmin;

    vm.useSystemMailSettings = false;

    vm.smtpProtocols = ('smtp smtps').split(' ').map(function (protocol) {
        return protocol;
    });

    vm.mailTemplate = types.mailTemplate.test.value;

    $translate('admin.test-mail-sent').then(function (translation) {
        vm.testMailSent = translation;
    }, function (translationId) {
        vm.testMailSent = translationId;
    });

    vm.tinyMceOptions = {
        plugins: ['textcolor colorpicker link table image imagetools code fullscreen'],
        menubar: "edit insert tools view format table",
        toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor | link | table | image | alignleft aligncenter alignright alignjustify  | numlist bullist outdent indent  | removeformat | code | fullscreen',
        font_formats: 'Helvetica Neue="Helvetica Neue",helvetica,arial,sans-serif;Helvetica=helvetica,arial,sans-serif;Times New Roman=times new roman,times,serif;Sans Serif=sans-serif,helvetica,arial;Arial=arial,helvetica,sans-serif;Courier New=courier new,courier,monospace;',
        fontsize_formats: '8pt 10pt 11pt 12pt 14pt 18pt 24pt 36pt',
        height: 280,
        autofocus: false,
        branding: false,
        setup: function(ed)
        {
            ed.on('init', function()
            {
                //ed.execCommand("fontName", false, "Helvetica Neue");
                //ed.execCommand("fontSize", false, "11pt");
            });
        }
    };

    loadSettings();

    function loadSettings() {

        var systemByDefault = $state.$current.data.key == "mailTemplates";

        adminService.getAdminSettings($state.$current.data.key, systemByDefault).then(function success(settings) {
            vm.settings = settings;
            if (isTenantAdmin()) {
                vm.useSystemMailSettings =
                    angular.isDefined(vm.settings.jsonValue.useSystemMailSettings) ? vm.settings.jsonValue.useSystemMailSettings : true;
            }
        });
    }

    function save() {
        if (isTenantAdmin()) {
            vm.settings.jsonValue.useSystemMailSettings = vm.useSystemMailSettings;
        }
        adminService.saveAdminSettings(vm.settings).then(function success(settings) {
            vm.settings = settings;
            vm.settingsForm.$setPristine();
        });
    }

    function sendTestMail() {
        adminService.sendTestMail(vm.settings).then(function success() {
            toast.showSuccess($translate.instant('admin.test-mail-sent'));
        });
    }

    function isTenantAdmin() {
        return userService.getAuthority() == 'TENANT_ADMIN';
    }

}
