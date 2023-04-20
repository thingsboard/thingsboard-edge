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

import { CmdWrapper, WsService, WsSubscriber } from '@shared/models/websocket/websocket.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { NgZone } from '@angular/core';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { WebsocketNotificationMsg } from '@shared/models/websocket/notification-ws.models';
import { CmdUpdateMsg } from '@shared/models/telemetry/telemetry.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ReportService } from '@core/http/report.service';
import Timeout = NodeJS.Timeout;

const RECONNECT_INTERVAL = 2000;
const WS_IDLE_TIMEOUT = 90000;
const MAX_PUBLISH_COMMANDS = 10;

export abstract class WebsocketService<T extends WsSubscriber> implements WsService<T> {

  isActive = false;
  isOpening = false;
  isOpened = false;
  isReconnect = false;

  socketCloseTimer: Timeout;
  reconnectTimer: Timeout;

  lastCmdId = 0;
  subscribersCount = 0;
  subscribersMap = new Map<number, T>();

  reconnectSubscribers = new Set<T>();

  notificationUri: string;

  dataStream: WebSocketSubject<CmdWrapper | CmdUpdateMsg>;

  errorName = 'WebSocket Error';

  protected constructor(protected store: Store<AppState>,
                        protected authService: AuthService,
                        protected ngZone: NgZone,
                        protected apiEndpoint: string,
                        protected cmdWrapper: CmdWrapper,
                        protected window: Window,
                        protected reportService?: ReportService) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      () => {
        this.reset(true);
      }
    );

    let port = this.window.location.port;
    if (this.window.location.protocol === 'https:') {
      if (!port) {
        port = '443';
      }
      this.notificationUri = 'wss:';
    } else {
      if (!port) {
        port = '80';
      }
      this.notificationUri = 'ws:';
    }
    this.notificationUri += `//${this.window.location.hostname}:${port}/${apiEndpoint}`;
  }

  abstract subscribe(subscriber: T, skipPublish?: boolean);

  abstract update(subscriber: T);

  abstract unsubscribe(subscriber: T, skipPublish?: boolean);

  abstract processOnMessage(message: any);

  public batchSubscribe(subscribers: T[]) {
    subscribers.forEach((subscriber) => {
      this.subscribe(subscriber, true);
    });
  }

  public batchUnsubscribe(subscribers: T[]) {
    subscribers.forEach((subscriber) => {
      this.unsubscribe(subscriber, true);
      subscriber.complete();
    });
  }

  protected nextCmdId(): number {
    this.lastCmdId++;
    return this.lastCmdId;
  }

  protected publishCommands() {
    while (this.isOpened && this.cmdWrapper.hasCommands()) {
      const cmds = this.cmdWrapper.preparePublishCommands(MAX_PUBLISH_COMMANDS);
      if (this.reportService?.reportView) {
        this.reportService.onSendWsCommands(cmds);
      }
      this.dataStream.next(cmds);
      this.checkToClose();
    }
    this.tryOpenSocket();
  }

  private checkToClose() {
    if (this.subscribersCount === 0 && this.isOpened) {
      if (!this.socketCloseTimer) {
        this.socketCloseTimer = setTimeout(
          () => this.closeSocket(), WS_IDLE_TIMEOUT);
      }
    }
  }

  private reset(close: boolean) {
    if (this.socketCloseTimer) {
      clearTimeout(this.socketCloseTimer);
      this.socketCloseTimer = null;
    }
    this.lastCmdId = 0;
    this.subscribersMap.clear();
    this.subscribersCount = 0;
    this.cmdWrapper.clear();
    if (close) {
      this.closeSocket();
    }
  }

  private closeSocket() {
    this.isActive = false;
    if (this.isOpened) {
      this.dataStream.unsubscribe();
    }
  }

  private tryOpenSocket() {
    if (this.isActive) {
      if (!this.isOpened && !this.isOpening) {
        this.isOpening = true;
        if (AuthService.isJwtTokenValid()) {
          this.openSocket(AuthService.getJwtToken());
        } else {
          this.authService.refreshJwtToken().subscribe({
            next: () => {
              this.openSocket(AuthService.getJwtToken());
            },
            error: () => {
              this.isOpening = false;
              this.authService.logout(true, true);
            }
          });
        }
      }
      if (this.socketCloseTimer) {
        clearTimeout(this.socketCloseTimer);
        this.socketCloseTimer = null;
      }
    }
  }

  private openSocket(token: string) {
    const uri = `${this.notificationUri}?token=${token}`;
    this.dataStream = webSocket(
      {
        url: uri,
        openObserver: {
          next: () => {
            this.onOpen();
          }
        },
        closeObserver: {
          next: (e: CloseEvent) => {
            this.onClose(e);
          }
        }
      }
    );

    this.dataStream.subscribe({
      next: (message) => {
        this.ngZone.runOutsideAngular(() => {
          this.onMessage(message as WebsocketNotificationMsg);
        });
      },
      error: (error) => {
        this.onError(error);
      }
    });
  }

  private onOpen() {
    this.isOpening = false;
    this.isOpened = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.isReconnect) {
      this.isReconnect = false;
      this.reconnectSubscribers.forEach(
        (reconnectSubscriber) => {
          reconnectSubscriber.onReconnected();
          this.subscribe(reconnectSubscriber);
        }
      );
      this.reconnectSubscribers.clear();
    } else {
      this.publishCommands();
    }
  }

  private onMessage(message: WebsocketNotificationMsg) {
    if (this.reportService?.reportView) {
      this.reportService.onWsCmdUpdateMessage(message);
    }
    if (message.errorCode) {
      this.showWsError(message.errorCode, message.errorMsg);
    } else {
      this.processOnMessage(message);
    }
    this.checkToClose();
  }

  private onError(errorEvent) {
    if (errorEvent) {
      console.warn('WebSocket error event', errorEvent);
    }
    this.isOpening = false;
  }

  private onClose(closeEvent: CloseEvent) {
    if (closeEvent && closeEvent.code > 1001 && closeEvent.code !== 1006
      && closeEvent.code !== 1011 && closeEvent.code !== 1012 && closeEvent.code !== 4500) {
      this.showWsError(closeEvent.code, closeEvent.reason);
    }
    this.isOpening = false;
    this.isOpened = false;
    if (this.isActive) {
      if (!this.isReconnect) {
        this.reconnectSubscribers.clear();
        this.subscribersMap.forEach(
          (subscriber) => {
            this.reconnectSubscribers.add(subscriber);
          }
        );
        this.reset(false);
        this.isReconnect = true;
      }
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
      }
      this.reconnectTimer = setTimeout(() => this.tryOpenSocket(), RECONNECT_INTERVAL);
    }
  }

  private showWsError(errorCode: number, errorMsg: string) {
    let message = errorMsg;
    if (!message) {
      message += `${this.errorName}: error code - ${errorCode}.`;
    }
    this.store.dispatch(new ActionNotificationShow(
      {
        message, type: 'error'
      }));
  }

}
