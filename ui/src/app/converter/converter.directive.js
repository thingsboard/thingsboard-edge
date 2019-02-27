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
/* eslint-disable import/no-unresolved, import/default */

import converterFieldsetTemplate from './converter-fieldset.tpl.html';
import converterTestTemplate from './converter-test.tpl.html';
import jsDecoderTemplate from './js-decoder.tpl.txt';
import jsEncoderTemplate from './js-encoder.tpl.txt';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ConverterDirective($compile, $templateCache, $translate, $mdDialog, $document, toast, types, converterService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(converterFieldsetTemplate);
        element.html(template);

        scope.types = types;

        scope.converterTypeChanged = () => {
            if (scope.converter.type) {
                if (!scope.converter.configuration) {
                    scope.converter.configuration = {};
                }
                if (scope.converter.type == types.converterType.UPLINK.value) {
                    delete scope.converter.configuration.encoder;
                    if (!scope.converter.configuration.decoder || !scope.converter.configuration.decoder.length) {
                        scope.converter.configuration.decoder = jsDecoderTemplate;
                    }
                } else if (scope.converter.type == types.converterType.DOWNLINK.value) {
                    delete scope.converter.configuration.decoder;
                    if (!scope.converter.configuration.encoder || !scope.converter.configuration.encoder.length) {
                        scope.converter.configuration.encoder = jsEncoderTemplate;
                    }
                }
            }
        }

        scope.onConverterIdCopied = function() {
            toast.showSuccess($translate.instant('converter.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.$watch('converter', function(newVal) {
            if (newVal) {
                if (!scope.converter.id) {
                    scope.converter.type = types.converterType.UPLINK.value;
                }
                scope.converterTypeChanged();
            }
        });

        scope.openConverterTestDialog = function ($event, isDecoder) {
            if ($event) {
                $event.stopPropagation();
            }
            if (scope.converter.id) {
                converterService.getLatestConverterDebugInput(scope.converter.id.id).then(
                    (debugIn) => {
                        showConverterTestDialog($event, isDecoder, debugIn);
                    },
                    () => {}
                );
            } else {
                showConverterTestDialog($event, isDecoder, null);
            }
        };

        function showConverterTestDialog($event, isDecoder, debugIn) {
            var funcBody;
            if (isDecoder) {
                funcBody = angular.copy(scope.converter.configuration.decoder);
            } else {
                funcBody = angular.copy(scope.converter.configuration.encoder);
            }
            var onShowingCallback = {
                onShowed: () => {
                }
            };
            $mdDialog.show({
                controller: 'ConverterTestController',
                controllerAs: 'vm',
                templateUrl: converterTestTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    isDecoder: isDecoder,
                    funcBody: funcBody,
                    onShowingCallback: onShowingCallback,
                    debugIn: debugIn
                },
                fullscreen: true,
                multiple: true,
                targetEvent: $event,
                onComplete: () => {
                    onShowingCallback.onShowed();
                }
            }).then(
                (funcBody) => {
                    if (isDecoder) {
                        scope.converter.configuration.decoder = funcBody;
                    } else {
                        scope.converter.configuration.encoder = funcBody;
                    }
                    scope.theForm.$setDirty();
                }
            );
        }

        $compile(element.contents())(scope);

    };
    return {
        restrict: "E",
        link: linker,
        scope: {
            converter: '=',
            isEdit: '=',
            theForm: '=',
            isCreate: '<',
            onExportConverter: '&',
            onDeleteConverter: '&'
        }
    };
}
