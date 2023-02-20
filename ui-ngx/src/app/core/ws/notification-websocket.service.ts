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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { WINDOW } from '@core/services/window.service';
import {
  isNotificationCountUpdateMsg,
  isNotificationsUpdateMsg,
  MarkAllAsReadCmd,
  MarkAsReadCmd,
  NotificationCountUpdate,
  NotificationPluginCmdWrapper,
  NotificationSubscriber,
  NotificationsUpdate,
  UnreadCountSubCmd,
  UnreadSubCmd,
  UnsubscribeCmd,
  WebsocketNotificationMsg
} from '@shared/models/websocket/notification-ws.models';
import { WebsocketService } from '@core/ws/websocket.service';


// @dynamic
@Injectable({
  providedIn: 'root'
})
export class NotificationWebsocketService extends WebsocketService<NotificationSubscriber> {

  cmdWrapper: NotificationPluginCmdWrapper;

  constructor(protected store: Store<AppState>,
              protected authService: AuthService,
              protected ngZone: NgZone,
              @Inject(WINDOW) protected window: Window) {
    super(store, authService, ngZone, 'api/ws/plugins/notifications', new NotificationPluginCmdWrapper(), window);
    this.errorName = 'WebSocket Notification Error';
  }

  public subscribe(subscriber: NotificationSubscriber) {
    this.isActive = true;
    subscriber.subscriptionCommands.forEach(
      (subscriptionCommand) => {
        const cmdId = this.nextCmdId();
        this.subscribersMap.set(cmdId, subscriber);
        subscriptionCommand.cmdId = cmdId;
        if (subscriptionCommand instanceof UnreadCountSubCmd) {
          this.cmdWrapper.unreadCountSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof UnreadSubCmd) {
          this.cmdWrapper.unreadSubCmd = subscriptionCommand;
        } else if (subscriptionCommand instanceof MarkAsReadCmd) {
          this.cmdWrapper.markAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        } else if (subscriptionCommand instanceof MarkAllAsReadCmd) {
          this.cmdWrapper.markAllAsReadCmd = subscriptionCommand;
          this.subscribersMap.delete(cmdId);
        }
      }
    );
    if (this.cmdWrapper.markAsReadCmd || this.cmdWrapper.markAllAsReadCmd) {
      this.subscribersCount++;
    }
    this.publishCommands();
  }

  public update(subscriber: NotificationSubscriber) {
    if (!this.isReconnect) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand.cmdId && subscriptionCommand instanceof UnreadSubCmd) {
            this.cmdWrapper.unreadSubCmd = subscriptionCommand;
          }
        }
      );
      this.publishCommands();
    }
  }

  public unsubscribe(subscriber: NotificationSubscriber) {
    if (this.isActive) {
      subscriber.subscriptionCommands.forEach(
        (subscriptionCommand) => {
          if (subscriptionCommand instanceof UnreadCountSubCmd
              || subscriptionCommand instanceof UnreadSubCmd) {
            const unreadCountUnsubscribeCmd = new UnsubscribeCmd();
            unreadCountUnsubscribeCmd.cmdId = subscriptionCommand.cmdId;
            this.cmdWrapper.unsubCmd = unreadCountUnsubscribeCmd;
          }
          const cmdId = subscriptionCommand.cmdId;
          if (cmdId) {
            this.subscribersMap.delete(cmdId);
          }
        }
      );
      this.reconnectSubscribers.delete(subscriber);
      this.subscribersCount--;
      this.publishCommands();
    }
  }

  processOnMessage(message: WebsocketNotificationMsg) {
    let subscriber: NotificationSubscriber;
    if (isNotificationCountUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onNotificationCountUpdate(new NotificationCountUpdate(message));
      }
    } else if (isNotificationsUpdateMsg(message)) {
      subscriber = this.subscribersMap.get(message.cmdId);
      if (subscriber) {
        subscriber.onNotificationsUpdate(new NotificationsUpdate(message));
      }
    }
  }

}
