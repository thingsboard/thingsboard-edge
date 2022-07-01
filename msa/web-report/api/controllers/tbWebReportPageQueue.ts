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

import { Browser } from 'puppeteer';
import { TbWebReportPage } from './tbWebReportPage';
import { RequestState } from './tbWebReportController';

export class TbWebReportPageQueue {

    private pages: Array<TbWebReportPage> = [];

    constructor(private browser: Browser,
                private maxPageCount: number) {
    }

    async init(): Promise<void> {
        for (let i = 0; i < this.maxPageCount; i++) {
            const page = new TbWebReportPage(this.browser, i+1);
            await page.init();
            this.pages.push(page);
        }
    }

    async generateDashboardReport(requestState: RequestState, url: string, type: 'png' | 'jpeg' | 'webp' | 'pdf', timezone: string): Promise<Buffer> {
        const page = this.pages.pop();
        if (page) {
            return await this.doGenerateDashboardReport(page, url, type, timezone);
        } else {
            return new Promise<Buffer>(
                (resolve, reject) => {
                    const waitInterval = setInterval(() => {
                        if (requestState.closed || requestState.timeout) {
                            clearInterval(waitInterval);
                            reject(requestState.closed ? 'Request cancelled!' : 'Generate report timeout!');
                        } else {
                            const page = this.pages.pop();
                            if (page) {
                                clearInterval(waitInterval);
                                this.doGenerateDashboardReport(page, url, type, timezone).then(
                                    (buffer) => {
                                        resolve(buffer);
                                    },
                                    (e) => {
                                        reject(e);
                                    }
                                );
                            }
                        }
                    }, 100);
                }
            )
        }
    }

    private async doGenerateDashboardReport(page: TbWebReportPage, url: string, type: 'png' | 'jpeg' | 'webp' | 'pdf', timezone: string): Promise<Buffer> {
        try {
            return await page.generateDashboardReport(url, type, timezone);
        } finally {
            this.pages.push(page);
        }
    }
}
