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
(function () {

    Date.prototype.timezoneOffset = new Date().getTimezoneOffset();

    Date.setTimezoneOffset = function(timezoneOffset) {
        return this.prototype.timezoneOffset = timezoneOffset;
    };

    Date.getTimezoneOffset = function(timezoneOffset) { //eslint-disable-line
        return this.prototype.timezoneOffset;
    };

    Date.prototype.setTimezoneOffset = function(timezoneOffset) {
        return this.timezoneOffset = timezoneOffset;
    };

    Date.prototype.getTimezoneOffset = function() {
        return this.timezoneOffset;
    };

    Date.prototype.toString = function() {
        var offsetDate, offsetTime;
        offsetTime = this.timezoneOffset * 60 * 1000;
        offsetDate = new Date(this.getTime() - offsetTime);
        return offsetDate.toUTCString();
    };

    ['Milliseconds', 'Seconds', 'Minutes', 'Hours', 'Date', 'Month', 'FullYear', 'Year', 'Day'].forEach((function(_this) { //eslint-disable-line
        return function(key) {
            Date.prototype["get" + key] = function() {
                var offsetDate, offsetTime;
                offsetTime = this.timezoneOffset * 60 * 1000;
                offsetDate = new Date(this.getTime() - offsetTime);
                return offsetDate["getUTC" + key]();
            };
            return Date.prototype["set" + key] = function(value) {
                var offsetDate, offsetTime, time;
                offsetTime = this.timezoneOffset * 60 * 1000;
                offsetDate = new Date(this.getTime() - offsetTime);
                offsetDate["setUTC" + key](value);
                time = offsetDate.getTime() + offsetTime;
                this.setTime(time);
                return time;
            };
        };
    })(this));

})();