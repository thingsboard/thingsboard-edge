/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
import $ from 'jquery';
import 'brace/ext/language_tools';
import 'brace/mode/java';
import 'brace/theme/github';

/* eslint-disable angular/angularelement */

import './audit-log-details-dialog.scss';

/*@ngInject*/
export default function AuditLogDetailsDialogController($mdDialog, types, auditLog, showingCallback) {

    var vm = this;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element, vm.actionData, 'tb-audit-log-action-data');
        vm.actionDataEditor.resize();
        if (vm.displayFailureDetails) {
            updateEditorSize(element, vm.actionFailureDetails, 'tb-audit-log-failure-details');
            vm.failureDetailsEditor.resize();
        }
    };

    vm.types = types;
    vm.auditLog = auditLog;
    vm.displayFailureDetails = auditLog.actionStatus == types.auditLogActionStatus.FAILURE.value;
    vm.actionData = auditLog.actionDataText;
    vm.actionFailureDetails = auditLog.actionFailureDetails;

    vm.actionDataContentOptions = {
        useWrapMode: false,
        mode: 'java',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.actionDataEditor = _ace;
        }
    };

    vm.failureDetailsContentOptions = {
        useWrapMode: false,
        mode: 'java',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.failureDetailsEditor = _ace;
        }
    };

    function updateEditorSize(element, content, editorId) {
        var newHeight = 200;
        var newWidth = 600;
        if (content && content.length > 0) {
            var lines = content.split('\n');
            newHeight = 16 * lines.length + 16;
            var maxLineLength = 0;
            for (var i in lines) {
                var line = lines[i].replace(/\t/g, '    ').replace(/\n/g, '');
                var lineLength = line.length;
                maxLineLength = Math.max(maxLineLength, lineLength);
            }
            newWidth = 8 * maxLineLength + 16;
        }
        $('#'+editorId, element).height(newHeight.toString() + "px").css('min-height', newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
    }

    vm.close = close;

    function close () {
        $mdDialog.hide();
    }

}

/* eslint-enable angular/angularelement */
