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

import { Inject, Injectable, NgZone } from '@angular/core';
import {
  AlarmDataCmd,
  AlarmDataUnsubscribeCmd,
  AlarmDataUpdate,
<<<<<<< HEAD
  AttributesSubscriptionCmd, CmdUpdateMsg,
  EntityCountCmd, EntityCountUnsubscribeCmd,
=======
  AttributesSubscriptionCmd,
  EntityCountCmd,
  EntityCountUnsubscribeCmd,
>>>>>>> ce-fork/feature/notification-system
  EntityCountUpdate,
  EntityDataCmd,
  EntityDataUnsubscribeCmd,
  EntityDataUpdate,
  GetHistoryCmd,
  isAlarmDataUpdateMsg,
  isEntityCountUpdateMsg,
  isEntityDataUpdateMsg,
  SubscriptionCmd,
  SubscriptionUpdate,
  TelemetryFeature,
  TelemetryPluginCmdsWrapper,
  TelemetrySubscriber,
  TimeseriesSubscriptionCmd,
  WebsocketDataMsg
} from '@app/shared/models/telemetry/telemetry.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
<<<<<<< HEAD
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import Timeout = NodeJS.Timeout;
import { ReportService } from '@core/http/report.service';

const RECONNECT_INTERVAL = 2000;
const WS_IDLE_TIMEOUT = 90000;
const MAX_PUBLISH_COMMANDS = 10;
=======
import { WebsocketService } from '@core/ws/websocket.service';
>>>>>>> ce-fork/feature/notification-system

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class TelemetryWebsocketService extends WebsocketService<TelemetrySubscriber> {

<<<<<<< HEAD
  cmdsWrapper = new TelemetryPluginCmdsWrapper();
  telemetryUri: string;

  dataStream: WebSocketSubject<TelemetryPluginCmdsWrapper | WebsocketDataMsg>;

  constructor(private store: Store<AppState>,
              private authService: AuthService,
              private ngZone: NgZone,
              private reportService: ReportService,
              @Inject(WINDOW) private window: Window) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      () => {
        this.reset(true);
      }
    );
=======
  cmdWrapper: TelemetryPluginCmdsWrapper;
>>>>>>> ce-fork/feature/notification-system

  constructor(protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws/plugins/telemetry', new TelemetryPluginCmdsWrapper(), window);
  }

  public subscribe(subscriber: TelemetrySubscriber, skipPublish?: boolean) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        this.subscribersMap.set(cmdId, subscriber);
        subscriptionCommand.cmdId = cmdId;
        if (subscriptionCommand instanceof SubscriptionCmd) {
          if (subscriptionCommand.getType() === TelemetryFeature.TIMESERIES) {
            this.cmdWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
          } else {
            this.cmdWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
          }
        } else if (subscriptionCommand instanceof GetHistoryCmd) {
          this.cmdWrapper.historyCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityDataCmd) {
          this.cmdWrapper.entityDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof AlarmDataCmd) {
          this.cmdWrapper.alarmDataCmds.push(subscriptionCommand);
        } else if (subscriptionCommand instanceof EntityCountCmd) {
          this.cmdWrapper.entityCountCmds.push(subscriptionCommand);
        }
      }
    );
    this.subscribersCount++;
    if (!skipPublish) {
      this.publishCommands();
    }
  }

  public batchSubscribe(subscribers: TelemetrySubscriber[]) {
    subscribers.forEach((subscriber) => {
      this.subscribe(subscriber, true);
    });
  }

  public update(subscriber: TelemetrySubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && subscriptionCommand instanceof EntityDataCmd) {
            this.cmdWrapper.entityDataCmds.push(subscriptionCommand);
          }
        }
      );
      this.publishCommands();
    }
  }

  public unsubscribe(subscriber: TelemetrySubscriber, skipPublish?: boolean) {
    if (this.isActive) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand instanceof SubscriptionCmd) {
            subscriptionCommand.unsubscribe = true;
            if (subscriptionCommand.getType() === TelemetryFeature.TIMESERIES) {
              this.cmdWrapper.tsSubCmds.push(subscriptionCommand as TimeseriesSubscriptionCmd);
            } else {
              this.cmdWrapper.attrSubCmds.push(subscriptionCommand as AttributesSubscriptionCmd);
            }
          } else if (subscriptionCommand instanceof EntityDataCmd) {
            const entityDataUnsubscribeCmd = new EntityDataUnsubscribeCmd();
            entityDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.entityDataUnsubscribeCmds.push(entityDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof AlarmDataCmd) {
            const alarmDataUnsubscribeCmd = new AlarmDataUnsubscribeCmd();
            alarmDataUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.alarmDataUnsubscribeCmds.push(alarmDataUnsubscribeCmd);
          } else if (subscriptionCommand instanceof EntityCountCmd) {
            const entityCountUnsubscribeCmd = new EntityCountUnsubscribeCmd();
            entityCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.entityCountUnsubscribeCmds.push(entityCountUnsubscribeCmd);
          }
          const cmdId = subscriptionCommand.cmdId;
          if (cmdId) {
            this.subscribersMap.delete(cmdId);
          }
        }
      );
      this.reconnectSubscribers.delete(subscriber);
      this.subscribersCount--;
      if (!skipPublish) {
        this.publishCommands();
      }
    }
  }

<<<<<<< HEAD
  public batchUnsubscribe(subscribers: TelemetrySubscriber[]) {
    subscribers.forEach((subscriber) => {
      this.unsubscribe(subscriber, true);
      subscriber.complete();
    });
  }

  private nextCmdId(): number {
    this.lastCmdId++;
    return this.lastCmdId;
  }

  public publishCommands() {
    while (this.isOpened && this.cmdsWrapper.hasCommands()) {
      const cmds = this.cmdsWrapper.preparePublishCommands(MAX_PUBLISH_COMMANDS);
      if (this.reportService.reportView) {
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
    this.cmdsWrapper.clear();
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
          this.authService.refreshJwtToken().subscribe(() => {
              this.openSocket(AuthService.getJwtToken());
            },
            () => {
              this.isOpening = false;
              this.authService.logout(true, true);
            }
          );
        }
      }
      if (this.socketCloseTimer) {
        clearTimeout(this.socketCloseTimer);
        this.socketCloseTimer = null;
      }
    }
  }

  private openSocket(token: string) {
    const uri = `${this.telemetryUri}?token=${token}`;
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

    this.dataStream.subscribe((message) => {
        this.ngZone.runOutsideAngular(() => {
          this.onMessage(message as WebsocketDataMsg);
        });
    },
    (error) => {
      this.onError(error);
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

  private onMessage(message: WebsocketDataMsg) {
    if (this.reportService.reportView) {
      this.reportService.onWsCmdUpdateMessage(message as CmdUpdateMsg);
    }
    if (message.errorCode) {
      this.showWsError(message.errorCode, message.errorMsg);
    } else {
      let subscriber: TelemetrySubscriber;
      if (isEntityDataUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onEntityData(new EntityDataUpdate(message));
        }
      } else if (isAlarmDataUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onAlarmData(new AlarmDataUpdate(message));
        }
      } else if (isEntityCountUpdateMsg(message)) {
        subscriber = this.subscribersMap.get(message.cmdId);
        if (subscriber) {
          subscriber.onEntityCount(new EntityCountUpdate(message));
        }
      } else if (message.subscriptionId) {
        subscriber = this.subscribersMap.get(message.subscriptionId);
        if (subscriber) {
          subscriber.onData(new SubscriptionUpdate(message));
        }
=======
  processOnMessage(message: WebsocketDataMsg) {
    let subscriber: TelemetrySubscriber;
    if (isEntityDataUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onEntityData(new EntityDataUpdate(message));
      }
    } else if (isAlarmDataUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onAlarmData(new AlarmDataUpdate(message));
>>>>>>> ce-fork/feature/notification-system
      }
    } else if (isEntityCountUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onEntityCount(new EntityCountUpdate(message));
      }
    } else if (message.subscriptionId) {
      subscriber = this.subscribersMap.get(message.subscriptionId);
      if (subscriber) {
        subscriber.onData(new SubscriptionUpdate(message));
      }
    }
  }

}
