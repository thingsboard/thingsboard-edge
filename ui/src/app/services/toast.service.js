/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import infoToast from './info-toast.tpl.html';
import successToast from './success-toast.tpl.html';
import errorToast from './error-toast.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function Toast($mdToast, $document) {

    var showing = false;

    var service = {
        showInfo: showInfo,
        showSuccess: showSuccess,
        showError: showError,
        hide: hide
    }

    return service;

    function showInfo(infoMessage, delay, toastParent, position) {
        showMessage(infoToast, infoMessage, delay, toastParent, position);
    }

    function showSuccess(successMessage, delay, toastParent, position) {
        showMessage(successToast, successMessage, delay, toastParent, position);
    }

    function showMessage(templateUrl, message, delay, toastParent, position) {
        if (!toastParent) {
            toastParent = angular.element($document[0].getElementById('toast-parent'));
        }
        if (!position) {
            position = 'top left';
        }
        $mdToast.show({
            hideDelay: delay || 0,
            position: position,
            controller: 'ToastController',
            controllerAs: 'vm',
            templateUrl: templateUrl,
            locals: {message: message},
            parent: toastParent
        });
    }

    function showError(errorMessage, toastParent, position) {
        if (!showing) {
            if (!toastParent) {
                toastParent = angular.element($document[0].getElementById('toast-parent'));
            }
            if (!position) {
                position = 'top left';
            }
            showing = true;
            $mdToast.show({
                hideDelay: 0,
                position: position,
                controller: 'ToastController',
                controllerAs: 'vm',
                templateUrl: errorToast,
                locals: {message: errorMessage},
                parent: toastParent
            }).then(function hide() {
                showing = false;
            }, function cancel() {
                showing = false;
            });
        }
    }

    function hide() {
        if (showing) {
            $mdToast.hide();
        }
    }

}