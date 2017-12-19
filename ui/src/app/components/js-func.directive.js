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
import './js-func.scss';

import ace from 'brace';
import 'brace/ext/language_tools';
import $ from 'jquery';
import thingsboardToast from '../services/toast';
import thingsboardUtils from '../common/utils.service';
import thingsboardExpandFullscreen from './expand-fullscreen.directive';

/* eslint-disable import/no-unresolved, import/default */

import jsFuncTemplate from './js-func.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.jsFunc', [thingsboardToast, thingsboardUtils, thingsboardExpandFullscreen])
    .directive('tbJsFunc', JsFunc)
    .name;

/*@ngInject*/
function JsFunc($compile, $templateCache, toast, utils, $translate) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(jsFuncTemplate);
        element.html(template);

        scope.functionName = attrs.functionName;
        scope.functionArgs = scope.$eval(attrs.functionArgs);
        scope.validationArgs = scope.$eval(attrs.validationArgs);
        scope.resultType = attrs.resultType;
        if (!scope.resultType || scope.resultType.length === 0) {
            scope.resultType = "nocheck";
        }

        scope.validationTriggerArg = attrs.validationTriggerArg;

        scope.functionValid = true;

        var Range = ace.acequire("ace/range").Range;
        scope.js_editor;
        scope.errorMarkers = [];


        scope.functionArgsString = '';
        for (var i in scope.functionArgs) {
            if (scope.functionArgsString.length > 0) {
                scope.functionArgsString += ', ';
            }
            scope.functionArgsString += scope.functionArgs[i];
        }

        scope.onFullscreenChanged = function () {
            updateEditorSize();
        };

        function updateEditorSize() {
            if (scope.js_editor) {
                scope.js_editor.resize();
                scope.js_editor.renderer.updateFull();
            }
        }

        scope.jsEditorOptions = {
            useWrapMode: true,
            mode: 'javascript',
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function (_ace) {
                scope.js_editor = _ace;
                scope.js_editor.session.on("change", function () {
                    scope.cleanupJsErrors();
                });
            }
        };

        scope.cleanupJsErrors = function () {
            toast.hide();
            for (var i = 0; i < scope.errorMarkers.length; i++) {
                scope.js_editor.session.removeMarker(scope.errorMarkers[i]);
            }
            scope.errorMarkers = [];
            if (scope.errorAnnotationId && scope.errorAnnotationId > -1) {
                var annotations = scope.js_editor.session.getAnnotations();
                annotations.splice(scope.errorAnnotationId, 1);
                scope.js_editor.session.setAnnotations(annotations);
                scope.errorAnnotationId = -1;
            }
        }

        scope.updateValidity = function () {
            ngModelCtrl.$setValidity('functionBody', scope.functionValid);
        };

        scope.$watch('functionBody', function (newFunctionBody, oldFunctionBody) {
            ngModelCtrl.$setViewValue(scope.functionBody);
            if (!angular.equals(newFunctionBody, oldFunctionBody)) {
                scope.functionValid = true;
            }
            scope.updateValidity();
        });

        ngModelCtrl.$render = function () {
            scope.functionBody = ngModelCtrl.$viewValue;
        };

        scope.showError = function (error) {
            var toastParent = $('#tb-javascript-panel', element);
            var dialogContent = toastParent.closest('md-dialog-content');
            if (dialogContent.length > 0) {
                toastParent = dialogContent;
            }
            toast.showError(error, toastParent, 'bottom left');
        }

        scope.validate = function () {
            try {
                var toValidate = new Function(scope.functionArgsString, scope.functionBody);
                if (scope.noValidate) {
                    return true;
                }
                var res;
                var validationError;
                for (var i=0;i<scope.validationArgs.length;i++) {
                    try {
                        res = toValidate.apply(this, scope.validationArgs[i]);
                        validationError = null;
                        break;
                    } catch (e) {
                        validationError = e;
                    }
                }
                if (validationError) {
                    throw validationError;
                }
                if (scope.resultType != 'nocheck') {
                    if (scope.resultType === 'any') {
                        if (angular.isUndefined(res)) {
                            scope.showError($translate.instant('js-func.no-return-error'));
                            return false;
                        }
                    } else {
                        var resType = typeof res;
                        if (resType != scope.resultType) {
                            scope.showError($translate.instant('js-func.return-type-mismatch', {type: scope.resultType}));
                            return false;
                        }
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
                if (details.lineNumber) {
                    errorInfo += '<br>Line ' + details.lineNumber;
                    if (details.columnNumber) {
                        errorInfo += ' column ' + details.columnNumber;
                    }
                    errorInfo += ' of script.';
                }
                scope.showError(errorInfo);
                if (scope.js_editor && details.lineNumber) {
                    var line = details.lineNumber - 1;
                    var column = 0;
                    if (details.columnNumber) {
                        column = details.columnNumber;
                    }

                    var errorMarkerId = scope.js_editor.session.addMarker(new Range(line, 0, line, Infinity), "ace_active-line", "screenLine");
                    scope.errorMarkers.push(errorMarkerId);
                    var annotations = scope.js_editor.session.getAnnotations();
                    var errorAnnotation = {
                        row: line,
                        column: column,
                        text: details.message,
                        type: "error"
                    };
                    scope.errorAnnotationId = annotations.push(errorAnnotation) - 1;
                    scope.js_editor.session.setAnnotations(annotations);
                }
                return false;
            }
        };

        scope.$on('form-submit', function (event, args) {
            if (!args || scope.validationTriggerArg && scope.validationTriggerArg == args) {
                scope.validationArgs = scope.$eval(attrs.validationArgs);
                scope.cleanupJsErrors();
                scope.functionValid = true;
                scope.updateValidity();
                scope.functionValid = scope.validate();
                scope.updateValidity();
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
            disabled:'=ngDisabled',
            noValidate: '=?',
            fillHeight:'=?'
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
