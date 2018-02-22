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
export default angular.module('thingsboard.clipboard', [])
    .factory('clipboardService', ClipboardService)
    .name;

/*@ngInject*/
function ClipboardService($q) {

    var fakeHandler, fakeHandlerCallback, fakeElem;

    var service = {
        copyToClipboard: copyToClipboard
    };

    return service;

    /* eslint-disable */
    function copyToClipboard(trigger, text) {
        var deferred = $q.defer();
        const isRTL = document.documentElement.getAttribute('dir') == 'rtl';
        removeFake();
        fakeHandlerCallback = () => removeFake();
        fakeHandler = document.body.addEventListener('click', fakeHandlerCallback) || true;
        fakeElem = document.createElement('textarea');
        fakeElem.style.fontSize = '12pt';
        fakeElem.style.border = '0';
        fakeElem.style.padding = '0';
        fakeElem.style.margin = '0';
        fakeElem.style.position = 'absolute';
        fakeElem.style[ isRTL ? 'right' : 'left' ] = '-9999px';
        let yPosition = window.pageYOffset || document.documentElement.scrollTop;
        fakeElem.style.top = `${yPosition}px`;
        fakeElem.setAttribute('readonly', '');
        fakeElem.value = text;
        document.body.appendChild(fakeElem);
        var selectedText = select(fakeElem);

        let succeeded;
        try {
            succeeded = document.execCommand('copy');
        }
        catch (err) {
            succeeded = false;
        }
        if (trigger) {
            trigger.focus();
        }
        window.getSelection().removeAllRanges();
        removeFake();
        if (succeeded) {
            deferred.resolve(selectedText);
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function removeFake() {
        if (fakeHandler) {
            document.body.removeEventListener('click', fakeHandlerCallback);
            fakeHandler = null;
            fakeHandlerCallback = null;
        }
        if (fakeElem) {
            document.body.removeChild(fakeElem);
            fakeElem = null;
        }
    }

    function select(element) {
        var selectedText;

        if (element.nodeName === 'SELECT') {
            element.focus();

            selectedText = element.value;
        }
        else if (element.nodeName === 'INPUT' || element.nodeName === 'TEXTAREA') {
            var isReadOnly = element.hasAttribute('readonly');

            if (!isReadOnly) {
                element.setAttribute('readonly', '');
            }

            element.select();
            element.setSelectionRange(0, element.value.length);

            if (!isReadOnly) {
                element.removeAttribute('readonly');
            }

            selectedText = element.value;
        }
        else {
            if (element.hasAttribute('contenteditable')) {
                element.focus();
            }

            var selection = window.getSelection();
            var range = document.createRange();

            range.selectNodeContents(element);
            selection.removeAllRanges();
            selection.addRange(range);

            selectedText = selection.toString();
        }

        return selectedText;
    }

    /* eslint-enable */

}