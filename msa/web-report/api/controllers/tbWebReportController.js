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
'use strict';

var config = require('config');
var logger = require('../../config/logger')('ReportController');

const defaultPageNavigationTimeout = Number(config.get('browser.defaultPageNavigationTimeout'));
const dashboardLoadWaitTime = Number(config.get('browser.dashboardLoadWaitTime'));

exports.genDashboardReport = function(req, res, browser) {
    var body = req.body;
    if (body.baseUrl && body.dashboardId) {
        var baseUrl = body.baseUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        var url = baseUrl;
        url += "dashboard/"+body.dashboardId;
        var token = body.token;
        var expiration = body.expiration;
        var name = body.name;
        var timewindow = null;
        var type = 'pdf'; // 'jpeg', 'png';
        var tzOffset = null;

        var reportParams = body.reportParams;
        if (reportParams) {
            if (reportParams.type && reportParams.type.length) {
                type = reportParams.type;
            }
            var urlParams = [];
            if (reportParams.state && reportParams.state.length) {
                urlParams.push("state="+reportParams.state);
            }
            if (reportParams.publicId && reportParams.publicId.length) {
                urlParams.push("publicId="+reportParams.publicId);
            }
            if (urlParams.length) {
                url += "?" + urlParams.join('&');
            }

            if (reportParams.timewindow) {
                timewindow = reportParams.timewindow;
            }
            if (typeof reportParams.tzOffset === 'number') {
                tzOffset = reportParams.tzOffset;
            }
        }

        var contentType, ext;
        if (type === 'pdf') {
            contentType = 'application/pdf';
            ext = '.pdf';
        } else if (type === 'jpeg') {
            contentType = 'image/jpeg';
            ext = '.jpg';
        } else if (type === 'png') {
            contentType = 'image/png';
            ext = '.png';
        } else {
            res.statusMessage = 'Unsupported report type format: ' + type;
            res.status(400).end();
            return;
        }
        logger.info('Generating dashboard report: %s', url);
        generateDashboardReport(browser, baseUrl, url, type, timewindow, tzOffset, token, expiration).then(
            (reportBuffer) => {
                res.attachment(name + ext);
                res.contentType(contentType);
                res.send(reportBuffer);
                logger.info('Report data sent.');
            },
            (e) => {
                logger.error(e);
                res.statusMessage = 'Failed to load dashboard page: ' + e;
                res.status(500).end();
            }
        );
    } else {
        res.statusMessage = 'Base url or Dashboard Id parameters are missing';
        res.status(400).end()
    }
};

async function generateDashboardReport(browser, baseUrl, url, type, timewindow, tzOffset, token, expiration) {
    var page = await browser.newPage();
    page.setDefaultNavigationTimeout(defaultPageNavigationTimeout);
    //page.on('console', msg => logger.info('PAGE LOG: %s', msg.text()));
    try {
        await page.setViewport({
            width: 1920,
            height: 1080,
            deviceScaleFactor: 1,
            isMobile: false,
            isLandscape: false
        });

        await page.emulateMedia('screen');

        const homeLoadResponse = await page.goto(baseUrl+'home', {waitUntil: 'networkidle2'});
        if (homeLoadResponse._status >= 400) {
            throw new Error("Home page load returned error status: " + homeLoadResponse._status);
        }

        var toEval =
            `var prefix = 'tbReportStore.';\n` +
            `sessionStorage.setItem(prefix+'jwt_token', '\"${token}\"');\n` +
            `sessionStorage.setItem(prefix+'jwt_token_expiration', ${expiration});\n` +
            `sessionStorage.setItem(prefix+'report_view', 'true');\n`;

        if (timewindow) {
            var timewindowStr = JSON.stringify(timewindow);
            toEval += `var timewindow = ${timewindowStr};\n`;
            toEval += `sessionStorage.setItem(prefix+'report_timewindow', JSON.stringify(timewindow));\n`;
        } else {
            toEval += `sessionStorage.removeItem(prefix+'report_timewindow');\n`;
        }
        if (typeof tzOffset === 'number') {
            toEval += `sessionStorage.setItem(prefix+'report_tz_offset', ${tzOffset});\n`;
        } else {
            toEval += `sessionStorage.removeItem(prefix+'report_tz_offset');\n`;
        }

        await page.evaluate(toEval);

        const dashboardLoadResponse = await page.goto(url, {waitUntil: 'networkidle2'});
        if (dashboardLoadResponse._status < 400) {
            await page.waitFor(dashboardLoadWaitTime);
        } else {
            throw new Error("Dashboard page load returned error status: " + dashboardLoadResponse._status);
        }

        toEval = "var height = 0;\n" +
            "     var gridsterChild = document.getElementById('gridster-child');\n" +
            "     if (gridsterChild) {\n" +
            "         height = Number(document.getElementById('gridster-child').offsetHeight);\n" +
            "         var dashboardTitleElements = document.getElementsByClassName(\"tb-dashboard-title\");\n" +
            "         if (dashboardTitleElements && dashboardTitleElements.length) {\n" +
            "              height += Number(dashboardTitleElements[0].offsetHeight);\n" +
            "         }\n" +
            "     }\n" +
            "     Math.round(height);";

        const fullHeight = await page.evaluate(toEval);

        await page.setViewport({
            width: 1920,
            height: fullHeight || 1080,
            deviceScaleFactor: 1,
            isMobile: false,
            isLandscape: false
        });
        var buffer;
        if (type === 'pdf') {
            buffer = await page.pdf({printBackground: true, width: '1920px', height: fullHeight + 'px'});
        } else {
            var options = {omitBackground: false, fullPage: true, type: type};
            if (type === 'jpeg') {
                options.quality = 100;
            }
            buffer = await page.screenshot(options);
        }
    } finally {
        await page.close();
    }
    return buffer;
}
