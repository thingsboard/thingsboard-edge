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

import { Browser, BrowserContextOptions, CDPSession, Page, PageScreenshotOptions } from 'playwright-core';
import { performance } from 'perf_hooks';
import { _logger } from '../../config/logger';
import config from 'config';

const defaultPageNavigationTimeout = Number(config.get('browser.defaultPageNavigationTimeout'));

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
                height: 1080
            }
        }
        const context = await this.browser.newContext(config);
        this.page = await context.newPage();
        this.session = await context.newCDPSession(this.page);
        this.page.setDefaultNavigationTimeout(defaultPageNavigationTimeout);
        await this.page.emulateMedia({media: 'screen'});
    }

    async generateDashboardReport(url: string, type: 'png' | 'jpeg' | 'pdf', timezone = 'Europe/London'): Promise<Buffer> {
        this.logger.info('Generating dashboard report');
        try {
            await this.session.send('Emulation.setTimezoneOverride', {timezoneId: timezone});
        } catch (e) {
            await this.session.send('Emulation.setTimezoneOverride', {timezoneId: 'Europe/London'});
        }
        const startTime = performance.now();
        const dashboardLoadResponse = await this.page.goto(url, {waitUntil: 'networkidle'});
        if (dashboardLoadResponse && dashboardLoadResponse.status() < 400) {
            await this.page.waitForSelector('section.tb-dashboard-container');
            await this.page.waitForFunction('Array.from(document.querySelectorAll(\'tb-widget>div.tb-widget-loading\')).every(item => item.style.display === \'none\')', {polling: 100});
            const endTime = performance.now();
            this.logger.debug(`Open page time: ${endTime - startTime}ms`);
        } else {
            const status = dashboardLoadResponse && dashboardLoadResponse.status() || 'null';
            throw new Error(`Dashboard page load returned error status: ${status}`);
        }

        const fullHeight: number = await this.page.evaluate(heightCalculationScript);

        await this.page.setViewportSize({
            width: 1920,
            height: fullHeight || 1080
        });

        let buffer: Buffer;

        if (type === 'pdf') {
            buffer = await this.page.pdf({printBackground: true, width: '1920px', height: fullHeight + 'px'});
        } else {
            const options: PageScreenshotOptions = {omitBackground: false, fullPage: true, type: type};
            if (type === 'jpeg') {
                options.quality = 100;
            }
            buffer = await this.page.screenshot(options);
        }
        await this.page.setContent('<body></body>');
        const endTime = performance.now();
        this.logger.info('Dashboard report generated in %sms.', endTime - startTime);
        return buffer;
    }

}
