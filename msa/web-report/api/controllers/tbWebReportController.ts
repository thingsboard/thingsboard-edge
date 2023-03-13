///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import config from 'config';
import { GenerateReportRequest, parseGenerateReportRequest, RequestState } from './tbWebReportModels';

const logger = _logger('ReportController');
const generateReportTimeout = Number(config.get('browser.generateReportTimeout'));

let activeRequestsCount = 0;

export function genDashboardReport(req: Request, res: Response, queue: TbWebReportPageQueue) {
    let request: GenerateReportRequest;
    try {
        request = parseGenerateReportRequest(req);
    } catch (e: any) {
        res.statusMessage = e.message;
        res.status(400).end();
        return;
    }
    activeRequestsCount++;
    logger.info('Generating dashboard report: baseUrl %s, dashboardId: %s. Active requests count: %s', request.baseUrl, request.dashboardId, activeRequestsCount);
    const requestState: RequestState = {
        closed: false,
        timeout: false
    };
    req.socket.on('close', () => {
        requestState.closed = true;
    });
    const timeoutTimer = setTimeout(() => {
        requestState.timeout = true;
    }, generateReportTimeout);
    queue.generateDashboardReport(requestState, request).then(
        (reportBuffer) => {
            clearTimeout(timeoutTimer);
            res.attachment(request.name + request.reportContentType.ext);
            res.contentType(request.reportContentType.contentType);
            res.send(reportBuffer);
            activeRequestsCount--;
            logger.info('Report data sent. Active requests count: %s', activeRequestsCount);
        },
        (e) => {
            clearTimeout(timeoutTimer);
            logger.error(e);
            if (requestState.timeout) {
                res.statusMessage = 'Generate report timeout!';
                res.status(503).end();
            } else {
                res.statusMessage = 'Failed to load dashboard page: ' + e;
                res.status(500).end();
            }
            activeRequestsCount--;
        }
    );
}
