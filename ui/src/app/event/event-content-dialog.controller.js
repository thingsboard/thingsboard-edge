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
import beautify from 'js-beautify';

/* eslint-disable angular/angularelement */

const js_beautify = beautify.js;

/*@ngInject*/
export default function EventContentDialogController($mdDialog, types, content, contentType, title, showingCallback) {

    var vm = this;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element);
    }

    vm.content = content;
    vm.title = title;

    var mode;
    if (contentType) {
        mode = types.contentType[contentType].code;
        if (contentType == types.contentType.JSON.value && vm.content) {
            vm.content = js_beautify(vm.content, {indent_size: 4});
        }
    } else {
        mode = 'java';
    }

    vm.contentOptions = {
        useWrapMode: false,
        mode: mode,
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.editor = _ace;
        }
    };

    function updateEditorSize(element) {
        var newHeight = 400;
        var newWidth = 600;
        if (vm.content && vm.content.length > 0) {
            var lines = vm.content.split('\n');
            newHeight = 16 * lines.length + 16;
            var maxLineLength = 0;
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i].replace(/\t/g, '    ').replace(/\n/g, '');
                var lineLength = line.length;
                maxLineLength = Math.max(maxLineLength, lineLength);
            }
            newWidth = 8 * maxLineLength + 16;
        }
        $('#tb-event-content', element).height(newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
        vm.editor.resize();
    }

    vm.close = close;

    function close () {
        $mdDialog.hide();
    }

}

/* eslint-enable angular/angularelement */
