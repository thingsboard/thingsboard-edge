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
import './json-form.scss';

import tinycolor from 'tinycolor2';
import ObjectPath from 'objectpath';
import inspector from 'schema-inspector';
import ReactSchemaForm from './react/json-form-react.jsx';
import jsonFormTemplate from './json-form.tpl.html';
import { utils } from 'react-schema-form';

export default angular.module('thingsboard.directives.jsonForm', [])
    .directive('tbJsonForm', JsonForm)
    .value('ReactSchemaForm', ReactSchemaForm)
    .name;

/*@ngInject*/
function JsonForm($compile, $templateCache, $mdColorPicker, whiteLabelingService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(jsonFormTemplate);

        element.html(template);

        var childScope;

        var destroyModelChangeWatches = function() {
            if (scope.modelWatchHandle) {
                scope.modelWatchHandle();
            }
            if (scope.modelRefWatchHandle) {
                scope.modelRefWatchHandle();
            }
        }

        var initModelChangeWatches = function() {
            scope.modelWatchHandle = scope.$watch('model',function(newValue, prevValue) {
                if (newValue && prevValue && !angular.equals(newValue,prevValue)) {
                    scope.validate();
                    if (scope.formControl) {
                        scope.formControl.$setDirty();
                    }
                }
            }, true);
            scope.modelRefWatchHandle = scope.$watch('model',function(newValue, prevValue) {
                if (newValue && newValue != prevValue) {
                    scope.updateValues();
                }
            });
        };

        var recompile = function() {
            if (childScope) {
                childScope.$destroy();
            }
            childScope = scope.$new();
            $compile(element.contents())(childScope);
        }

        scope.isFullscreen = false;

        scope.formProps = {
            isFullscreen: false,
            option: {
                formDefaults: {
                    startEmpty: true
                }
            },
            onModelChange: function(key, val) {
                if (angular.isString(val) && val === '') {
                    val = undefined;
                }
                selectOrSet(key, scope.model, val);
                scope.formProps.model = scope.model;
            },
            onColorClick: function(event, key, val) {
                scope.showColorPicker(event, val);
            },
            primaryPalette: whiteLabelingService.getPrimaryPalette(),
            accentPalette: whiteLabelingService.getAccentPalette(),
            onToggleFullscreen: function() {
                scope.isFullscreen = !scope.isFullscreen;
                scope.formProps.isFullscreen = scope.isFullscreen;
            }
        };

        scope.showColorPicker = function (event, color) {
            $mdColorPicker.show({
                value: tinycolor(color).toRgbString(),
                defaultValue: '#fff',
                random: tinycolor.random(),
                clickOutsideToClose: false,
                hasBackdrop: false,
                multiple: true,
                preserveScope: false,

                mdColorAlphaChannel: true,
                mdColorSpectrum: true,
                mdColorSliders: true,
                mdColorGenericPalette: false,
                mdColorMaterialPalette: true,
                mdColorHistory: false,
                mdColorDefaultTab: 2,

                $event: event

            }).then(function (color) {
                if (event.data && event.data.onValueChanged) {
                    event.data.onValueChanged(tinycolor(color).toRgb());
                }
            });
        }

        scope.onFullscreenChanged = function() {}

        scope.validate = function(){
            if (scope.schema && scope.model) {
                var result = utils.validateBySchema(scope.schema, scope.model);
                if (scope.formControl) {
                    scope.formControl.$setValidity('jsonForm', result.valid);
                }
            }
        }

        scope.updateValues = function(skipRerender) {
            destroyModelChangeWatches();
            if (!skipRerender) {
                element.html(template);
            }
            var readonly = (scope.readonly && scope.readonly === true) ? true : false;
            var schema = scope.schema ? angular.copy(scope.schema) : {
                    type: 'object'
                };
            schema.strict = true;
            var form = scope.form ? angular.copy(scope.form) : [ "*" ];
            var groupInfoes = scope.groupInfoes ? angular.copy(scope.groupInfoes) : [];
            var model = scope.model || {};
            scope.model = inspector.sanitize(schema, model).data;
            scope.formProps.option.formDefaults.readonly = readonly;
            scope.formProps.schema = schema;
            scope.formProps.form = form;
            scope.formProps.groupInfoes = groupInfoes;
            scope.formProps.model = angular.copy(scope.model);
            if (!skipRerender) {
                recompile();
            }
            initModelChangeWatches();
        }

        scope.updateValues(true);

        scope.$watch('readonly',function() {
            scope.updateValues();
        });

        scope.$watch('schema',function(newValue, prevValue) {
            if (newValue && newValue != prevValue) {
                scope.updateValues();
                scope.validate();
            }
        });

        scope.$watch('form',function(newValue, prevValue) {
            if (newValue && newValue != prevValue) {
                scope.updateValues();
            }
        });

        scope.$watch('groupInfoes',function(newValue, prevValue) {
            if (newValue && newValue != prevValue) {
                scope.updateValues();
            }
        });

        scope.validate();

        recompile();

    }

    return {
        restrict: "E",
        scope: {
            schema: '=',
            form: '=',
            model: '=',
            formControl: '=',
            groupInfoes: '=',
            readonly: '='
        },
        link: linker
    };

}

function setValue(obj, key, val) {
    var changed = false;
    if (obj) {
        if (angular.isUndefined(val)) {
            if (angular.isDefined(obj[key])) {
                delete obj[key];
                changed = true;
            }
        } else {
            changed = !angular.equals(obj[key], val);
            obj[key] = val;
        }
    }
    return changed;
}

function selectOrSet(projection, obj, valueToSet) {
    var numRe = /^\d+$/;

    if (!obj) {
        obj = this;
    }

    if (!obj) {
        return false;
    }

    var parts = angular.isString(projection) ? ObjectPath.parse(projection) : projection;

    if (parts.length === 1) {
        return setValue(obj, parts[0], valueToSet);
    }

    if (angular.isUndefined(obj[parts[0]])) {
        obj[parts[0]] = parts.length > 2 && numRe.test(parts[1]) ? [] : {};
    }

    var value = obj[parts[0]];
    for (var i = 1; i < parts.length; i++) {
        if (parts[i] === '') {
            return false;
        }
        if (i === parts.length - 1) {
            return setValue(value, parts[i], valueToSet);
        } else {
            var tmp = value[parts[i]];
            if (angular.isUndefined(tmp) || tmp === null) {
                tmp = numRe.test(parts[i + 1]) ? [] : {};
                value[parts[i]] = tmp;
            }
            value = tmp;
        }
    }
    return value;
}
