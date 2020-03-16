///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UtilsService } from '@core/services/utils.service';
import { ReportParams, ReportType } from '@shared/models/report.models';
import { Timewindow } from '@shared/models/time/time.models';
import * as _moment from 'moment';
import 'moment-timezone';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  reportView = false;
  reportTimewindow: Timewindow = null;

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private utils: UtilsService,
    private http: HttpClient
  ) {
  }

  public loadReportParams() {
    const reportView = this.utils.getQueryParam('reportView');
    if (reportView) {
      this.reportView = true;
      const reportTimewindow = this.utils.getQueryParam('reportTimewindow');
      if (reportTimewindow && reportTimewindow.length) {
        this.reportTimewindow = JSON.parse(reportTimewindow);
      }
    }
  }

  public downloadDashboardReport(dashboardId: string, reportType: ReportType, state?: string, timewindow?: Timewindow): Observable<any> {
    const url = `/api/report/${dashboardId}/download`;
    const reportParams: ReportParams = {
      type: reportType,
      timezone: _moment.tz.guess()
    };
    if (state) {
      reportParams.state = state;
    }
    if (timewindow) {
      reportParams.timewindow = timewindow;
    }
    return this.downloadReport(url, reportParams);
  }

  public downloadTestReport(reportConfig: ReportParams, reportsServerEndpointUrl?: string): Observable<any> {
    const url = '/api/report/test';
    const params: {[param: string]: string} = {};
    if (reportsServerEndpointUrl) {
      params.reportsServerEndpointUrl = reportsServerEndpointUrl;
    }
    return this.downloadReport(url, reportConfig, params);
  }

  private downloadReport(url: string, reportParams: ReportParams, params?: {[param: string]: string}): Observable<any> {
    if (!params) {
      params = {};
    }
    return this.http.post(url, reportParams, {
      params,
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = this.document.createElement('a');
        try {
          const blob = new Blob([response.body], { type: contentType });
          const href = URL.createObjectURL(blob);
          linkElement.setAttribute('href', href);
          linkElement.setAttribute('download', filename);
          const clickEvent = new MouseEvent('click',
            {
              view: this.window,
              bubbles: true,
              cancelable: false
            }
          );
          linkElement.dispatchEvent(clickEvent);
          return null;
        } catch (e) {
          throw e;
        }
      })
    );
  }
}
