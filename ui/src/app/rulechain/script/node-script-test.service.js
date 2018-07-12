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
/* eslint-disable import/no-unresolved, import/default */

import nodeScriptTestTemplate from './node-script-test.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function NodeScriptTest($q, $mdDialog, $document, ruleChainService) {

    var service = {
        testNodeScript: testNodeScript
    };

    return service;

    function testNodeScript($event, script, scriptType, functionTitle, functionName, argNames, ruleNodeId) {
        var deferred = $q.defer();
        if ($event) {
            $event.stopPropagation();
        }

        var msg, metadata, msgType;
        if (ruleNodeId) {
            ruleChainService.getLatestRuleNodeDebugInput(ruleNodeId).then(
                (debugIn) => {
                    if (debugIn) {
                        if (debugIn.data) {
                            msg = angular.fromJson(debugIn.data);
                        }
                        if (debugIn.metadata) {
                            metadata = angular.fromJson(debugIn.metadata);
                        }
                        msgType = debugIn.msgType;
                    }
                    openTestScriptDialog($event, script, scriptType, functionTitle,
                                         functionName, argNames, msg, metadata, msgType).then(
                        (script) => {
                            deferred.resolve(script);
                        },
                        () => {
                            deferred.reject();
                        }
                    );
                },
                () => {
                    deferred.reject();
                }
            );
        } else {
            openTestScriptDialog($event, script, scriptType, functionTitle,
                functionName, argNames).then(
                (script) => {
                    deferred.resolve(script);
                },
                () => {
                    deferred.reject();
                }
            );
        }
        return deferred.promise;
    }

    function openTestScriptDialog($event, script, scriptType, functionTitle, functionName, argNames, msg, metadata, msgType) {
        var deferred = $q.defer();
        if (!msg) {
            msg = {
                temperature: 22.4,
                humidity: 78
            };
        }
        if (!metadata) {
            metadata = {
                deviceType: "default",
                deviceName: "Test Device",
                ts: new Date().getTime() + ""
            };
        }
        if (!msgType) {
            msgType = "POST_TELEMETRY_REQUEST";
        }

        var onShowingCallback = {
            onShowed: () => {
            }
        };

        var inputParams = {
            script: script,
            scriptType: scriptType,
            functionName: functionName,
            argNames: argNames
        };

        $mdDialog.show({
            controller: 'NodeScriptTestController',
            controllerAs: 'vm',
            templateUrl: nodeScriptTestTemplate,
            parent: angular.element($document[0].body),
            locals: {
                msg: msg,
                metadata: metadata,
                msgType: msgType,
                functionTitle: functionTitle,
                inputParams: inputParams,
                onShowingCallback: onShowingCallback
            },
            fullscreen: true,
            skipHide: true,
            targetEvent: $event,
            onComplete: () => {
                onShowingCallback.onShowed();
            }
        }).then(
            (script) => {
                deferred.resolve(script);
            },
            () => {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

}