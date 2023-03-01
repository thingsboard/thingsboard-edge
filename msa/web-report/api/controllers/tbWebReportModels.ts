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

import { Request } from 'express';

export interface RequestState {
    closed: boolean;
    timeout: boolean;
}

export type reportType = 'png' | 'jpeg' | 'pdf';

export interface GenerateReportRequest {
    baseUrl: string;
    dashboardId: string;
    type: reportType;
    name: string;
    accessToken?: string;
    publicId?: string;
    state?: string;
    reportTimewindow?: string;
    timezone: string;
    reportContentType: ReportContentType;
}

export interface ReportContentType {
    contentType: string;
    ext: string;
}

export interface ReportResultMessage {
    success: boolean;
    error?: string;
}

export interface OpenReportMessage {
    dashboardId: string;
    timeout: number;
    accessToken?: string;
    publicId?: string;
    state?: string;
    reportTimewindow?: object;
}

export const reportContentTypeMap = new Map<reportType, ReportContentType>(
    [
        [
            'pdf',
            {
                contentType: 'application/pdf',
                ext: '.pdf'
            }
        ],
        [
            'jpeg',
            {
                contentType: 'image/jpeg',
                ext: '.jpg'
            }
        ],
        [
            'png',
            {
                contentType: 'image/png',
                ext: '.png'
            }
        ]
    ]
);

export function parseGenerateReportRequest(req: Request): GenerateReportRequest {
    const body = req.body;
    if (body.baseUrl && body.dashboardId) {
        let baseUrl = body.baseUrl;
        let type: reportType = 'pdf';
        let state: string | undefined;
        let publicId: string | undefined;
        let reportTimewindow: string | undefined;
        let timezone = 'Europe/London';
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        const reportParams = body.reportParams;
        if (reportParams) {
            if (reportParams.type && reportParams.type.length) {
                type = reportParams.type;
            }
            state = reportParams.state;
            publicId = reportParams.publicId;
            if (reportParams.timewindow) {
                reportTimewindow = JSON.stringify(reportParams.timewindow);
            }
            if (typeof reportParams.timezone === 'string') {
                timezone = reportParams.timezone;
            }
        }
        const reportContentType = reportContentTypeMap.get(type);
        if (!reportContentType) {
            throw new Error(`Unsupported report type format: ${type}`);
        }
        const generateReportRequest: GenerateReportRequest = {
            accessToken: body.token,
            dashboardId: body.dashboardId,
            name: body.name,
            baseUrl,
            type,
            reportContentType,
            publicId,
            state,
            reportTimewindow,
            timezone
        };
        return generateReportRequest;
    } else {
        throw new Error('Base url or Dashboard Id parameters are missing');
    }
}
