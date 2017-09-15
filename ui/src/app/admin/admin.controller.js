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

import './outgoing-mail-settings.scss';

/*@ngInject*/
export default function AdminController(adminService, userService, toast, $scope, $rootScope, $state, $translate, types) {

    var vm = this;
    vm.types = types;
    vm.save = save;
    vm.sendTestMail = sendTestMail;
    vm.saveMailTemplates = saveMailTemplates;
    vm.isTenantAdmin = isTenantAdmin;

    vm.useSystemMailSettings = false;
    vm.selectedTab = 0;

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
        plugins: ['textcolor colorpicker link table'],
        toolbar: 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor | link | table | alignleft aligncenter alignright alignjustify  | numlist bullist outdent indent  | removeformat',
        font_formats: 'Helvetica Neue="Helvetica Neue",helvetica,arial,sans-serif;Helvetica=helvetica,arial,sans-serif;Times New Roman=times new roman,times,serif;Sans Serif=sans-serif,helvetica,arial;Arial=arial,helvetica,sans-serif;Courier New=courier new,courier,monospace;',
        fontsize_formats: '8pt 10pt 11pt 12pt 14pt 18pt 24pt 36pt',
        height: 200,
        branding: false,
        setup: function(ed)
        {
            ed.on('init', function()
            {
                ed.execCommand("fontName", false, "Helvetica Neue");
                ed.execCommand("fontSize", false, "11pt");
            });
        }
    };

    loadSettings();

    function loadSettings() {
        adminService.getAdminSettings($state.$current.data.key).then(function success(settings) {
            vm.settings = settings;
            if (isTenantAdmin()) {
                vm.useSystemMailSettings =
                    angular.isDefined(vm.settings.jsonValue.useSystemMailSettings) ? vm.settings.jsonValue.useSystemMailSettings : true;
            }
        });
        if ($state.$current.data.key == "mail") {
            loadMailTemplatesSettings();
        }
    }

    function loadMailTemplatesSettings() {
        adminService.getAdminSettings('mailTemplates', true).then(function success(settings) {
            vm.mailTemplateSettings = settings;
            if (isTenantAdmin()) {
                vm.useSystemMailTemplateSettings =
                    angular.isDefined(vm.mailTemplateSettings.jsonValue.useSystemMailSettings) ? vm.mailTemplateSettings.jsonValue.useSystemMailSettings : true;
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

    function saveMailTemplates() {
        if (isTenantAdmin()) {
            vm.mailTemplateSettings.jsonValue.useSystemMailSettings = vm.useSystemMailTemplateSettings;
        }
        adminService.saveAdminSettings(vm.mailTemplateSettings).then(function success(settings) {
            vm.mailTemplateSettings = settings;
            vm.templateSettingsForm.$setPristine();
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
