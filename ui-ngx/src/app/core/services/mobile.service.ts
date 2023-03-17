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
import { WINDOW } from '@core/services/window.service';
import { isDefined } from '@core/utils';
import { MobileActionResult, WidgetMobileActionResult, WidgetMobileActionType } from '@shared/models/widget.models';
import { from, of } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';
import { catchError, tap } from 'rxjs/operators';
import { OpenDashboardMessage, ReloadUserMessage, WindowMessage } from '@shared/models/window-message.model';
import { Params, Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

const dashboardStateNameHandler = 'tbMobileDashboardStateNameHandler';
const dashboardLoadedHandler = 'tbMobileDashboardLoadedHandler';
const dashboardLayoutHandler = 'tbMobileDashboardLayoutHandler';
const navigationHandler = 'tbMobileNavigationHandler';
const mobileHandler = 'tbMobileHandler';
const recaptchaHandler = 'tbMobileRecaptchaHandler';
const recaptchaLoadedHandler = 'tbMobileRecaptchaLoadedHandler';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class MobileService {

  private readonly mobileApp;
  private readonly mobileChannel;

  private readonly onWindowMessageListener = this.onWindowMessage.bind(this);

  private reloadUserObservable: Observable<boolean>;
  private lastDashboardId: string;
  private toggleLayoutFunction: () => void;
  private resetRecaptchaFunction: () => void;

  constructor(@Inject(WINDOW) private window: Window,
              private router: Router,
              private authService: AuthService) {
    const w = (this.window as any);
    this.mobileChannel = w.flutter_inappwebview;
    this.mobileApp = isDefined(this.mobileChannel);
    if (this.mobileApp) {
      window.addEventListener('message', this.onWindowMessageListener);
    }
  }

  public isMobileApp(): boolean {
    return this.mobileApp;
  }

  public handleDashboardStateName(name: string) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(dashboardStateNameHandler, name);
    }
  }

  public onDashboardLoaded(hasRightLayout: boolean, rightLayoutOpened: boolean) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(dashboardLoadedHandler, hasRightLayout, rightLayoutOpened);
    }
  }

  public onDashboardRightLayoutChanged(opened: boolean) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(dashboardLayoutHandler, opened);
    }
  }

  public registerToggleLayoutFunction(toggleLayoutFunction: () => void) {
    this.toggleLayoutFunction = toggleLayoutFunction;
  }

  public unregisterToggleLayoutFunction() {
    this.toggleLayoutFunction = null;
  }

  public handleWidgetMobileAction<T extends MobileActionResult>(type: WidgetMobileActionType, ...args: any[]):
    Observable<WidgetMobileActionResult<T>> {
    if (this.mobileApp) {
      return from(
        this.mobileChannel.callHandler(mobileHandler, type, ...args) as Promise<WidgetMobileActionResult<T>>).pipe(
        catchError((err: Error) => {
          return of({
            hasError: true,
            error: err?.message ? err.message : `Failed to execute mobile action ${type}`
          } as WidgetMobileActionResult<any>);
        })
      );
    } else {
      return of(null);
    }
  }

  public handleMobileNavigation(path?: string, params?: Params) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(navigationHandler, path, params);
    }
  }

  public onRecaptchaLoaded() {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(recaptchaLoadedHandler);
    }
  }

  public handleReCaptchaResponse(recaptchaResponse: string) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(recaptchaHandler, recaptchaResponse);
    }
  }

  public registerResetRecaptchaFunction(resetRecaptchaFunction: () => void) {
    this.resetRecaptchaFunction = resetRecaptchaFunction;
  }

  public unregisterResetRecaptchaFunction() {
    this.resetRecaptchaFunction = null;
  }

  private onWindowMessage(event: MessageEvent) {
    if (event.data) {
      let message: WindowMessage;
      try {
        message = JSON.parse(event.data);
      } catch (e) {}
      if (message && message.type) {
        switch (message.type) {
          case 'openDashboardMessage':
            const openDashboardMessage: OpenDashboardMessage = message.data;
            this.openDashboard(openDashboardMessage);
            break;
          case 'reloadUserMessage':
            const reloadUserMessage: ReloadUserMessage = message.data;
            this.reloadUser(reloadUserMessage);
            break;
          case 'toggleDashboardLayout':
            if (this.toggleLayoutFunction) {
              this.toggleLayoutFunction();
            }
            break;
          case 'resetRecaptcha':
            if (this.resetRecaptchaFunction) {
              this.resetRecaptchaFunction();
            }
        }
      }
    }
  }

  private openDashboard(openDashboardMessage: OpenDashboardMessage) {
    if (openDashboardMessage && openDashboardMessage.dashboardId) {
      if (this.reloadUserObservable) {
        this.reloadUserObservable.subscribe(
          (authenticated) => {
            if (authenticated) {
              this.doDashboardNavigation(openDashboardMessage);
            }
          }
        );
      } else {
        this.doDashboardNavigation(openDashboardMessage);
      }
    }
  }

  private doDashboardNavigation(openDashboardMessage: OpenDashboardMessage) {
    let url = `/dashboard/${openDashboardMessage.dashboardId}`;
    const params = [];
    if (openDashboardMessage.state) {
      params.push(`state=${openDashboardMessage.state}`);
    }
    if (openDashboardMessage.embedded) {
      params.push(`embedded=true`);
    }
    if (openDashboardMessage.hideToolbar) {
      params.push(`hideToolbar=true`);
    }
    if (this.lastDashboardId === openDashboardMessage.dashboardId) {
      params.push(`reload=${new Date().getTime()}`);
    }
    if (params.length) {
      url += `?${params.join('&')}`;
    }
    this.lastDashboardId = openDashboardMessage.dashboardId;
    this.router.navigateByUrl(url, {replaceUrl: true});
  }

  private reloadUser(reloadUserMessage: ReloadUserMessage) {
    if (reloadUserMessage && reloadUserMessage.accessToken && reloadUserMessage.refreshToken) {
      this.reloadUserObservable = this.authService.setUserFromJwtToken(reloadUserMessage.accessToken,
        reloadUserMessage.refreshToken, true).pipe(
        tap(
          () => {
            this.reloadUserObservable = null;
          }
        )
      );
    }
  }

}
