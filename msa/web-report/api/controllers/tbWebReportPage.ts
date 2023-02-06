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

import { Browser, BrowserContextOptions, CDPSession, Page, PageScreenshotOptions } from 'playwright-core';
import { performance } from 'perf_hooks';
import { _logger } from '../../config/logger';
import config from 'config';
import { GenerateReportRequest, OpenReportMessage, ReportResultMessage } from './tbWebReportModels';

const defaultPageNavigationTimeout = Number(config.get('browser.defaultPageNavigationTimeout'));
const loadDashboardResourcesTimeout = Number(config.get('browser.loadDashboardResourcesTimeout'));
const dashboardIdleWaitTime = Number(config.get('browser.dashboardIdleWaitTime'));

const heightCalculationScript = "var height = 0;\n" +
    "     var gridsterChild = document.getElementById('gridster-child');\n" +
    "     if (gridsterChild) {\n" +
    "         height = Number(document.getElementById('gridster-child').scrollHeight);\n" +
    "         var dashboardTitleElements = document.getElementsByClassName(\"tb-dashboard-title\");\n" +
    "         if (dashboardTitleElements && dashboardTitleElements.length) {\n" +
    "              height += Number(dashboardTitleElements[0].offsetHeight);\n" +
    "         }\n" +
    "     }\n" +
    "     Math.round(height);";

export class TbWebReportPage {

    private logger = _logger(`TbWebReportPage-${this.id}`);
    private page: Page;
    private session: CDPSession;
    private currentBaseUrl: string;
    private lastReportResult: ReportResultMessage | null;
    private pageHeight = 1080;

    constructor(private browser: Browser,
                private id: number) {
    }

    async init(): Promise<void> {
        const config: BrowserContextOptions = {
            ignoreHTTPSErrors: true,
            deviceScaleFactor: 1,
            isMobile: false,
            viewport: {
                width: 1920,
                height: this.pageHeight
            }
        }
        const context = await this.browser.newContext(config);
        this.page = await context.newPage();
        this.session = await context.newCDPSession(this.page);
        this.page.setDefaultNavigationTimeout(defaultPageNavigationTimeout);
        await this.page.emulateMedia({media: 'screen'});
        await this.page.exposeFunction('postWebReportResult', (result: ReportResultMessage) => {
            this.lastReportResult = result;
        });
    }

    async destroy(): Promise<void> {
        if (this.page) {
            await this.page.close();
        }
    }

    async generateDashboardReport(request: GenerateReportRequest): Promise<Buffer> {
        this.logger.info('Generating dashboard report');
        try {
            await this.session.send('Emulation.setTimezoneOverride', {timezoneId: request.timezone});
        } catch (e) {
            await this.session.send('Emulation.setTimezoneOverride', {timezoneId: 'Europe/London'});
        }
        this.lastReportResult = null;
        const startTime = performance.now();
        if (this.currentBaseUrl !== request.baseUrl) {
            const dashboardLoadResponse = await this.page.goto(request.baseUrl+'?reportView=true', {waitUntil: 'networkidle'});
            if (dashboardLoadResponse && dashboardLoadResponse.status() < 400) {
                const result = await this.waitForReportResult('init page', loadDashboardResourcesTimeout);
                if (result.success) {
                    this.currentBaseUrl = request.baseUrl;
                } else {
                    throw new Error(`Failed to init page for base url: ${request.baseUrl}!`);
                }
            } else {
                const status = dashboardLoadResponse && dashboardLoadResponse.status() || 'null';
                throw new Error(`Dashboard page load returned error status: ${status}`);
            }
        }
        let buffer: Buffer;
        try {

            await this.setPageHeight(1080);

            await this.openReport(request);
            if (dashboardIdleWaitTime > 0) {
                await this.page.waitForTimeout(dashboardIdleWaitTime);
            }
            const fullHeight: number = await this.page.evaluate(heightCalculationScript);

            const newHeight = fullHeight || 1080;

            await this.setPageHeight(newHeight);

            if (request.type === 'pdf') {
                buffer = await this.page.pdf({printBackground: true, width: '1920px', height: this.pageHeight + 'px'});
            } else {
                const options: PageScreenshotOptions = {omitBackground: false, fullPage: true, type: request.type};
                if (request.type === 'jpeg') {
                    options.quality = 100;
                }
                buffer = await this.page.screenshot(options);
            }
        } finally {
            try {
                await this.clearReport();
            } catch (e) {}
        }
        const endTime = performance.now();
        this.logger.info('Dashboard report generated in %sms.', endTime - startTime);
        return buffer;
    }

    async openReport(request: GenerateReportRequest): Promise<void> {
        const openReportMessage: OpenReportMessage = {
            timeout: loadDashboardResourcesTimeout,
            dashboardId: request.dashboardId,
            accessToken: request.accessToken,
            publicId: request.publicId,
            state: request.state,
            reportTimewindow: request.reportTimewindow ? JSON.parse(request.reportTimewindow) : undefined
        };
        await this.postWindowMessage({type: 'openReport', data: openReportMessage});
        const result = await this.waitForReportResult('open report', loadDashboardResourcesTimeout * 3);
        if (!result.success) {
            throw new Error(result.error);
        }
    }

    async clearReport(): Promise<void> {
        await this.postWindowMessage({type: 'clearReport'});
        const result = await this.waitForReportResult('clear report');
        if (!result.success) {
            throw new Error(result.error);
        }
    }

    async postWindowMessage(message: object): Promise<void> {
        const messageStr = JSON.stringify(message);
        return this.page.evaluate(`window.postMessage('${messageStr}', '*');`);
    }

    async waitForReportResult(operation: string, timeout = 3000): Promise<ReportResultMessage> {
        return new Promise<ReportResultMessage>(
            (resolve, reject) => {
                if (this.lastReportResult) {
                    const result = this.lastReportResult;
                    this.lastReportResult = null;
                    resolve(result);
                } else {
                    let waitTime = 0;
                    const waitInterval = setInterval(() => {
                        if (this.lastReportResult) {
                            clearInterval(waitInterval);
                            const result = this.lastReportResult;
                            this.lastReportResult = null;
                            resolve(result);
                        } else {
                            waitTime += 10;
                            if (waitTime >= timeout) {
                                clearInterval(waitInterval);
                                reject(`Operation '${operation}' timed out!`);
                            }
                        }
                    }, 10);
                }
            }
        );
    }

    async setPageHeight(height: number): Promise<void> {
        if (this.pageHeight !== height) {
            this.pageHeight = height;
            await this.page.setViewportSize({
                width: 1920,
                height: this.pageHeight
            });
        }
    }

}
