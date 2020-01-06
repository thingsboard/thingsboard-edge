/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import './json-content.scss';

import 'brace/ext/language_tools';
import 'brace/ext/searchbox';
import 'brace/mode/json';
import 'brace/mode/text';
import 'brace/snippets/json';
import 'brace/snippets/text';

import fixAceEditor from './ace-editor-fix';

/* eslint-disable import/no-unresolved, import/default */

import jsonContentTemplate from './json-content.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import beautify from 'js-beautify';

const js_beautify = beautify.js;

export default angular.module('thingsboard.directives.jsonContent', [])
    .directive('tbJsonContent', JsonContent)
    .name;

/*@ngInject*/
function JsonContent($compile, $templateCache, $timeout, toast, types, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(jsonContentTemplate);
        element.html(template);

        scope.label = attrs.label;

        scope.validationTriggerArg = attrs.validationTriggerArg;

        scope.contentValid = true;

        scope.json_editor;

        scope.onFullscreenChanged = function () {
            updateEditorSize();
        };

        scope.beautifyJson = function () {
            var res = js_beautify(scope.contentBody, {indent_size: 4, wrap_line_length: 60});
            scope.contentBody = res;
        };

        function updateEditorSize() {
            if (scope.json_editor) {
                scope.json_editor.resize();
                scope.json_editor.renderer.updateFull();
            }
        }

        function updateEditorPlaceholder() {
            var shouldShow = !scope.json_editor.session.getValue().length;
            var node = scope.json_editor.renderer.emptyMessageNode;
            if (!shouldShow && node) {
                scope.json_editor.renderer.scroller.removeChild(scope.json_editor.renderer.emptyMessageNode);
                scope.json_editor.renderer.emptyMessageNode = null;
            } else if (shouldShow && !node) {
                var placeholderElement = angular.element("<textarea></textarea>");
                placeholderElement.text(scope.tbPlaceholder);
                placeholderElement.addClass("ace_invisible ace_emptyMessage");
                placeholderElement.css("padding", "0 9px");
                placeholderElement.css("width", "100%");
                placeholderElement.css("border", "none");
                var rows = scope.tbPlaceholder.split('\n').length;
                placeholderElement.attr("rows", rows);
                node = scope.json_editor.renderer.emptyMessageNode = placeholderElement[0];
                scope.json_editor.renderer.scroller.appendChild(node);
            }
        }

        function createPlaceholder() {
            scope.json_editor.on("input", updateEditorPlaceholder);
            $timeout(updateEditorPlaceholder, 100);
        }

        var mode;
        if (scope.contentType) {
            mode = types.contentType[scope.contentType].code;
        } else {
            mode = 'text';
        }

        scope.jsonEditorOptions = {
            useWrapMode: true,
            mode: mode,
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function (_ace) {
                scope.json_editor = _ace;
                scope.json_editor.session.on("change", function () {
                    scope.cleanupJsonErrors();
                });
                fixAceEditor(_ace);
                if (scope.tbPlaceholder && scope.tbPlaceholder.length) {
                    createPlaceholder();
                }
            }
        };

        scope.$watch('contentType', () => {
            var mode;
            if (scope.contentType) {
                mode = types.contentType[scope.contentType].code;
            } else {
                mode = 'text';
            }
            if (scope.json_editor) {
                scope.json_editor.session.setMode('ace/mode/' + mode);
            }
        });

        scope.cleanupJsonErrors = function () {
            toast.hide();
        };

        scope.updateValidity = function () {
            ngModelCtrl.$setValidity('contentBody', scope.contentValid);
        };

        scope.$watch('contentBody', function (newContent, oldContent) {
            ngModelCtrl.$setViewValue(scope.contentBody);
            if (!angular.equals(newContent, oldContent)) {
                scope.contentValid = true;
            }
            scope.updateValidity();
        });

        ngModelCtrl.$render = function () {
            scope.contentBody = ngModelCtrl.$viewValue;
        };

        scope.showError = function (error) {
            var toastParent = angular.element('#tb-json-panel', element);
            toast.showError(error, toastParent, 'bottom left');
        };

        scope.validate = function () {
            try {
                if (scope.validateContent) {
                    if (scope.contentType == types.contentType.JSON.value) {
                        angular.fromJson(scope.contentBody);
                    }
                }
                return true;
            } catch (e) {
                var details = utils.parseException(e);
                var errorInfo = 'Error:';
                if (details.name) {
                    errorInfo += ' ' + details.name + ':';
                }
                if (details.message) {
                    errorInfo += ' ' + details.message;
                }
                scope.showError(errorInfo);
                return false;
            }
        };

        scope.$on('form-submit', function (event, args) {
            if (!scope.readonly) {
                if (!args || scope.validationTriggerArg && scope.validationTriggerArg == args) {
                    scope.cleanupJsonErrors();
                    scope.contentValid = true;
                    scope.updateValidity();
                    scope.contentValid = scope.validate();
                    scope.updateValidity();
                }
            }
        });

        scope.$on('update-ace-editor-size', function () {
            updateEditorSize();
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            contentType: '=',
            validateContent: '=?',
            readonly:'=ngReadonly',
            fillHeight:'=?',
            tbPlaceholder:'=?'
        },
        link: linker
    };
}
