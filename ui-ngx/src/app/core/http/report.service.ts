///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UtilsService } from '@core/services/utils.service';
import { ReportParams, ReportType } from '@shared/models/report.models';
import { getDefaultTimezone, Timewindow } from '@shared/models/time/time.models';
import { Observable } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';

// @dynamic
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
    return getDefaultTimezone().pipe(
      mergeMap((defaultTz) => {
        const reportParams: ReportParams = {
          type: reportType,
          timezone: defaultTz
        };
        if (state) {
          reportParams.state = state;
        }
        if (timewindow) {
          reportParams.timewindow = timewindow;
        }
        return this.downloadReport(url, reportParams);
      })
    );
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
