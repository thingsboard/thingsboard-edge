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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { NotificationWebsocketService } from '@core/ws/notification-websocket.service';
import { BehaviorSubject, ReplaySubject, Subscription } from 'rxjs';
import { distinctUntilChanged, share, tap } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ShowNotificationPopoverComponent } from '@home/components/notification/show-notification-popover.component';
import { NotificationSubscriber } from '@shared/models/websocket/notification-ws.models';
import { select, Store } from '@ngrx/store';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-notification-bell',
  templateUrl: './notification-bell.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationBellComponent implements OnDestroy {

  private notificationSubscriber: NotificationSubscriber;
  private notificationCountSubscriber: Subscription;
  private countSubject = new BehaviorSubject(0);

  count$ = this.countSubject.asObservable().pipe(
    distinctUntilChanged(),
    tap(() => setTimeout(() => this.cd.markForCheck())),
    share({
      connector: () => new ReplaySubject(1)
    })
  );

  constructor(
    private notificationWsService: NotificationWebsocketService,
    private zone: NgZone,
    private cd: ChangeDetectorRef,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
    private store: Store<AppState>,) {
    this.initSubscription();
    this.store.pipe(select(selectIsAuthenticated)).subscribe((value) => {
      if (value) {
        this.initSubscription();
      }
    })
  }

  ngOnDestroy() {
    this.unsubscribeSubscription();
  }

  showNotification($event: Event, createVersionButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    this.unsubscribeSubscription();
    const trigger = createVersionButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const showNotificationPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ShowNotificationPopoverComponent, 'bottom', true, null,
        {
          onClose: () => {
            showNotificationPopover.hide();
            this.initSubscription();
          },
          counter: this.countSubject
        },
        {maxHeight: '90vh', height: '100%', padding: '10px'},
        {width: '400px', minWidth: '100%', maxWidth: '100%'},
        {height: '100%', flexDirection: 'column', boxSizing: 'border-box', display: 'flex'}, false);
      showNotificationPopover.tbComponentRef.instance.popoverComponent = showNotificationPopover;
    }
  }

  private initSubscription() {
    this.notificationSubscriber = NotificationSubscriber.createNotificationCountSubscription(this.notificationWsService, this.zone);
    this.notificationCountSubscriber = this.notificationSubscriber.notificationCount$.subscribe(value => this.countSubject.next(value));

    this.notificationSubscriber.subscribe();
  }

  private unsubscribeSubscription() {
    this.notificationCountSubscriber.unsubscribe();
    this.notificationSubscriber.unsubscribe();
  }
}
