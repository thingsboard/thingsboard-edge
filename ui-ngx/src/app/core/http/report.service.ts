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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UtilsService } from '@core/services/utils.service';
import { ReportParams, ReportType } from '@shared/models/report.models';
import { getDefaultTimezone, Timewindow } from '@shared/models/time/time.models';
import { from, Observable, of, Subject } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { OpenReportMessage, ReportResultMessage, WindowMessage } from '@shared/models/window-message.model';
import { CmdUpdateMsg, TelemetryPluginCmdsWrapper } from '@shared/models/telemetry/telemetry.models';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class ReportService {

  reportView = false;
  reportTimewindow: Timewindow = null;
  openReportSubject: Subject<void> = new Subject<void>();

  accessToken: string;
  publicId: string;

  private readonly onWindowMessageListener = this.onWindowMessage.bind(this);

  private currentDashboardId: string;
  private receiveWsData: Map<number, boolean> = new Map();
  private lastWsCommandTimeMs = 0;
  private waitForMaps: Set<string> = new Set();
  private lastWaitMapTimeMs = 0;
  private waitForWidgets = 0;
  private lastWaitWidgetsTimeMs = 0;

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private utils: UtilsService,
    private http: HttpClient,
    private router: Router,
    private authService: AuthService
  ) {
  }

  public loadReportParams(): boolean {
    const reportView = this.utils.getQueryParam('reportView') === 'true';
    if (reportView) {
      this.reportView = true;
      this.authService.loadUserFromAccessToken(null).subscribe(
        () => {
          window.addEventListener('message', this.onWindowMessageListener);
          if ((this.window as any).postWebReportResult) {
            this.postReportResult({success: true});
          } else {
            const interval = setInterval(() => {
              if ((this.window as any).postWebReportResult) {
                clearInterval(interval);
                this.postReportResult({success: true});
              }
            }, 20);
          }
        }
      );
    }
    return this.reportView;
  }

  public onSendWsCommands(cmds: TelemetryPluginCmdsWrapper) {
    for (const key of Object.keys(cmds)) {
      if (!key.toLowerCase().includes('unsubscribe')) {
        cmds[key].forEach((cmdComand: any) => {
          if (typeof cmdComand.cmdId === 'number') {
            if (!this.receiveWsData.has(cmdComand.cmdId)) {
              this.receiveWsData.set(cmdComand.cmdId, false);
              this.lastWsCommandTimeMs = this.utils.currentPerfTime();
            }
          }
        });
      }
    }
  }

  public onWsCmdUpdateMessage(message: CmdUpdateMsg) {
    if (message.cmdId !== undefined) {
      this.receiveWsData.set(message.cmdId, true);
    }
  }

  public onDashboardLoaded(widgetsCount: number) {
    this.waitForWidgets = widgetsCount;
    this.lastWaitWidgetsTimeMs = this.utils.currentPerfTime();
  }

  public onWaitForMap(): string {
    const uuid = this.utils.guid();
    this.waitForMaps.add(uuid);
    this.lastWaitMapTimeMs = this.utils.currentPerfTime();
    return uuid;
  }

  public onMapLoaded(uuid: string) {
    this.waitForMaps.delete(uuid);
  }

  public downloadDashboardReport(dashboardId: string, reportType: ReportType, state?: string, timewindow?: Timewindow): Observable<any> {
    const url = `/api/report/${dashboardId}/download`;
    const defaultTz = getDefaultTimezone();
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
  }

  public downloadTestReport(reportConfig: ReportParams, reportsServerEndpointUrl?: string): Observable<any> {
    const url = '/api/report/test';
    const params: {[param: string]: string} = {};
    if (reportsServerEndpointUrl) {
      params.reportsServerEndpointUrl = reportsServerEndpointUrl;
    }
    return this.downloadReport(url, reportConfig, params);
  }

  private onWindowMessage(event: MessageEvent) {
    if (event.data) {
      let message: WindowMessage;
      try {
        message = JSON.parse(event.data);
      } catch (e) {
      }
      if (message && message.type) {
        switch (message.type) {
          case 'openReport':
            const openReportMessage: OpenReportMessage = message.data;
            this.openReport(openReportMessage).subscribe((result) => {
              this.postReportResult(result);
            });
            break;
          case 'clearReport':
            this.clearReport().subscribe((result) => {
              const resultMessage: ReportResultMessage = {
                success: result
              };
              if (!result) {
                resultMessage.error = 'Navigation failed while clear report!';
              }
              this.postReportResult(resultMessage);
            });
            break;
        }
      }
    }
  }

  private postReportResult(result: ReportResultMessage) {
    if ((this.window as any).postWebReportResult) {
      (this.window as any).postWebReportResult(result);
    } else {
      this.window.postMessage(JSON.stringify({ type: 'reportResult', data: result}), '*');
    }
  }

  private openReport(openReportMessage: OpenReportMessage): Observable<ReportResultMessage> {
    if (openReportMessage && (openReportMessage.accessToken || openReportMessage.publicId) && openReportMessage.dashboardId) {
      return this.loadUser(openReportMessage.accessToken, openReportMessage.publicId).pipe(
        mergeMap((authenticated) => {
          if (authenticated) {
            if (openReportMessage.reportTimewindow) {
              this.reportTimewindow = openReportMessage.reportTimewindow;
            } else {
              this.reportTimewindow = null;
            }
            this.currentDashboardId = openReportMessage.dashboardId;
            let url = `/dashboard/${openReportMessage.dashboardId}`;
            const params = [];
            if (openReportMessage.state) {
              params.push(`state=${openReportMessage.state}`);
            }
            if (params.length) {
              url += `?${params.join('&')}`;
            }
            this.openReportSubject.next();
            this.waitForWidgets = 0;
            this.receiveWsData.clear();
            this.waitForMaps.clear();
            this.lastWaitWidgetsTimeMs = 0;
            this.lastWsCommandTimeMs = this.utils.currentPerfTime();
            this.lastWaitMapTimeMs = this.utils.currentPerfTime();
            return from(this.router.navigateByUrl(url, {replaceUrl: true})).pipe(
              mergeMap((result) => {
                if (result) {
                  return this.waitForReportReady(openReportMessage.timeout).pipe(
                    mergeMap(() => this.waitForReportReady(openReportMessage.timeout)),
                    map(() => ({ success: true })),
                    catchError((e) => of({ success: false, error: e }))
                  );
                } else {
                  return of({
                    success: false,
                    error: 'Failed to navigate to target dashboard!'
                  });
                }
              })
            );
          } else {
            return of({ success: false, error: 'Authentication failed!' });
          }
        })
      );
    } else {
      return of({ success: false, error: 'Invalid message arguments provided!' });
    }
  }

  private waitForReportReady(timeout = 3000): Observable<any> {
    return from(this.waitForReportPage(timeout)).pipe(
      mergeMap(() => from(this.waitForWebsocketData(timeout))),
      mergeMap(() => from(this.waitForMapsLoaded(timeout)))
    );
  }

  private waitForReportPage(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if (this.lastWaitWidgetsTimeMs && this.isReportPageDomReady()
            && (this.utils.currentPerfTime() - this.lastWaitWidgetsTimeMs >= 300)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for report page timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private waitForWebsocketData(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if ((!this.receiveWsData.size || Array.from(this.receiveWsData.values()).every(val => val)) &&
            (this.utils.currentPerfTime() - this.lastWsCommandTimeMs >= 100)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for websocket data timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private waitForMapsLoaded(timeout = 3000): Promise<void> {
    return new Promise<void>(
      (resolve, reject) => {
        let waitTime = 0;
        const waitInterval = setInterval(() => {
          if (!this.waitForMaps.size && (this.utils.currentPerfTime() - this.lastWaitMapTimeMs >= 100)) {
            clearInterval(waitInterval);
            resolve();
          } else {
            waitTime += 10;
            if (waitTime >= timeout) {
              clearInterval(waitInterval);
              reject('Wait for map tiles timed out!');
            }
          }
        }, 10);
      }
    );
  }

  private isReportPageDomReady(): boolean {
    if ($('section.tb-dashboard-container gridster#gridster-child').not('tb-widget-container gridster#gridster-child').length) {
      const widgets = Array.from($('tb-widget>div.tb-widget-loading'));
      if (widgets.length >= this.waitForWidgets && widgets.every(item => item.style.display === 'none')) {
        return true;
      }
    }
    return false;
  }

  private clearReport(): Observable<boolean> {
    if (this.publicId) {
      this.publicId = null;
      this.authService.logout();
      return of (true);
    } else {
      return from(this.router.navigateByUrl('/', {replaceUrl: true}));
    }
  }

  private loadUser(accessToken: string, publicId?: string): Observable<boolean> {
    if (publicId && publicId.length) {
      if (this.publicId !== publicId) {
        return this.authService.loadUserFromPublicId(publicId).pipe(
          map((payload) => {
            return payload !== null;
          }),
          tap((authenticated) => {
            if (authenticated) {
              this.publicId = publicId;
              this.accessToken = null;
            }
          })
        );
      } else {
        return of(true);
      }
    } else if (this.accessToken !== accessToken) {
      return this.authService.loadUserFromAccessToken(accessToken).pipe(
        map((payload) => {
          return payload !== null;
        }),
        tap((authenticated) => {
          if (authenticated) {
            this.accessToken = accessToken;
            this.publicId = null;
          }
        })
      );
    } else {
      return of(true);
    }
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
