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

/* eslint-disable import/no-unresolved, import/default */

import converterFieldsetTemplate from './converter-fieldset.tpl.html';
import customDecoderTestTemplate from './custom-decoder-test.tpl.html';
import jsDecoderTemplate from './js-decoder.tpl.txt';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ConverterDirective($compile, $templateCache, $translate, $mdDialog, $document, toast, types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(converterFieldsetTemplate);
        element.html(template);

        scope.types = types;

        scope.converterTypeChanged = () => {
            if (scope.converter.type == types.converterType.CUSTOM.value) {
                if (!scope.converter.configuration) {
                    scope.converter.configuration = {};
                }
                if (!scope.converter.configuration.decoder || !scope.converter.configuration.decoder.length) {
                    scope.converter.configuration.decoder = jsDecoderTemplate;
                }
                /*if (!scope.converter.configuration.encoder || !scope.converter.configuration.encoder.length) {
                    scope.converter.configuration.encoder = '// Encode downlink messages sent as\n'+
                        '// object to an array or buffer of bytes.\n'+
                        'var bytes = [];\n\n'+
                        '// if (port === 1) bytes[0] = object.led ? 1 : 0;\n\n'+
                        'return bytes;\n';
                }*/
            }
        }

        scope.onConverterIdCopied = function() {
            toast.showSuccess($translate.instant('converter.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.$watch('converter', function(newVal) {
            if (newVal) {
                if (!scope.converter.id) {
                    scope.converter.type = types.converterType.CUSTOM.value;
                    scope.converterTypeChanged();
                }
            }
        });

        scope.openCustomDecoderTestDialog = function ($event) {
            if ($event) {
                $event.stopPropagation();
            }
            var decoder = angular.copy(scope.converter.configuration.decoder);
            var onShowingCallback = {
                onShowed: () => {
                }
            };
            $mdDialog.show({
                controller: 'CustomDecoderTestController',
                controllerAs: 'vm',
                templateUrl: customDecoderTestTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    decoder: decoder,
                    onShowingCallback: onShowingCallback
                },
                fullscreen: true,
                skipHide: true,
                targetEvent: $event,
                onComplete: () => {
                    onShowingCallback.onShowed();
                }
            }).then(
                (decoder) => {
                    scope.converter.configuration.decoder = decoder;
                    scope.theForm.$setDirty();
                }
            );
        };

        $compile(element.contents())(scope);

    };
    return {
        restrict: "E",
        link: linker,
        scope: {
            converter: '=',
            isEdit: '=',
            theForm: '=',
            onExportConverter: '&',
            onDeleteConverter: '&'
        }
    };
}
