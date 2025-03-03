///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Inject, Injectable, NgZone } from '@angular/core';
import {
  TelemetryPluginCmdsWrapper,
  TelemetrySubscriber,
  WebsocketDataMsg
} from '@shared/models/telemetry/telemetry.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
import { WebsocketService } from '@core/ws/websocket.service';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class NotificationWebsocketService extends WebsocketService<TelemetrySubscriber> {

  constructor(private telemetryWebsocketService: TelemetryWebsocketService,
              protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws/plugins/telemetry', new TelemetryPluginCmdsWrapper(), window);
  }

  public subscribe(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.subscribe(subscriber);
  }

  public update(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.update(subscriber);
  }

  public unsubscribe(subscriber: TelemetrySubscriber) {
    this.telemetryWebsocketService.unsubscribe(subscriber);
  }

  processOnMessage(message: WebsocketDataMsg) {
    this.telemetryWebsocketService.processOnMessage(message);
  }
}
