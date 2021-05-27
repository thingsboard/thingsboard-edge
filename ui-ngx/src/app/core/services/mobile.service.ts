///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { catchError } from 'rxjs/operators';

const dashboardStateNameHandler = 'tbMobileDashboardStateNameHandler';
const mobileHandler = 'tbMobileHandler';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class MobileService {

  private readonly mobileApp;
  private readonly mobileChannel;

  constructor(@Inject(WINDOW) private window: Window) {
    const w = (this.window as any);
    this.mobileChannel = w.flutter_inappwebview;
    this.mobileApp = isDefined(this.mobileChannel);
  }

  public isMobileApp(): boolean {
    return this.mobileApp;
  }

  public handleDashboardStateName(name: string) {
    if (this.mobileApp) {
      this.mobileChannel.callHandler(dashboardStateNameHandler, name);
    }
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

}
