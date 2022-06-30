///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { _logger } from '../../config/logger';
import { TbWebReportPageQueue } from './tbWebReportPageQueue';
import { Request, Response } from 'express';

const logger = _logger('ReportController');

let activeRequestsCount = 0;

export function genDashboardReport(req: Request, res: Response, queue: TbWebReportPageQueue) {
    const body = req.body;
    if (body.baseUrl && body.dashboardId) {
        let baseUrl = body.baseUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        let dashboardUrl = baseUrl;
        dashboardUrl += "dashboard/" + body.dashboardId;
        let url = dashboardUrl;
        const token = body.token;
        const name = body.name;
        let timewindow = null;
        let type = 'pdf'; // 'jpeg', 'png';
        let timezone = null;

        const reportParams = body.reportParams;
        if (reportParams) {
            if (reportParams.type && reportParams.type.length) {
                type = reportParams.type;
            }
            const urlParams = [];
            urlParams.push("reportView=true");
            urlParams.push("accessToken="+token);

            if (reportParams.state && reportParams.state.length) {
                urlParams.push("state="+reportParams.state);
            }
            if (reportParams.publicId && reportParams.publicId.length) {
                urlParams.push("publicId="+reportParams.publicId);
            }
            if (reportParams.timewindow) {
                timewindow = reportParams.timewindow;
                const timewindowStr = JSON.stringify(timewindow);
                urlParams.push("reportTimewindow="+encodeURIComponent(timewindowStr));
            }
            if (typeof reportParams.timezone === 'string') {
                timezone = reportParams.timezone;
            }

            if (urlParams.length) {
                url += "?" + urlParams.join('&');
            }
        }

        let contentType: string, ext: string;
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
        activeRequestsCount++;
        logger.info('Generating dashboard report: %s. Active requests count: %s', dashboardUrl, activeRequestsCount);
        queue.generateDashboardReport(url, type, timezone).then(
            (reportBuffer) => {
                res.attachment(name + ext);
                res.contentType(contentType);
                res.send(reportBuffer);
                activeRequestsCount--;
                logger.info('Report data sent. Active requests count: %s', activeRequestsCount);
            },
            (e) => {
                logger.error(e);
                res.statusMessage = 'Failed to load dashboard page: ' + e;
                res.status(500).end();
                activeRequestsCount--;
            }
        );
    } else {
        res.statusMessage = 'Base url or Dashboard Id parameters are missing';
        res.status(400).end()
    }
}
