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
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import {
  Notification,
  NotificationStatus,
  NotificationTemplateTypeTranslateMap
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { InboxTableHeaderComponent } from '@home/pages/notification-center/inbox-table/inbox-table-header.component';
import { TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  InboxNotificationDialogComponent, InboxNotificationDialogData
} from '@home/pages/notification-center/inbox-table/inbox-notification-dialog.component';

export class InboxTableConfig extends EntityTableConfig<Notification> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {
    super();
    this.entitiesDeleteEnabled = false;
    this.rowPointer = true;
    this.entityTranslations = {
      noEntities: 'notification.no-inbox-notification',
      search: 'notification.search-notification'
    };
    this.entityResources = {} as EntityTypeResource<Notification>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotifications(pageLink, this.componentsData.unreadOnly);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.handleRowClick = ($event, notification) => {
      this.showNotification($event, notification);
      return true;
    };

    this.componentsData = {
      unreadOnly: true
    };

    this.cellActionDescriptors = this.configureCellActions();

    this.headerComponent = InboxTableHeaderComponent;

    this.headerActionDescriptors = [{
      name: this.translate.instant('notification.mark-all-as-read'),
      icon: 'done_all',
      isEnabled: () => true,
      onAction: $event => this.markAllRead($event)
    }];

    this.columns.push(
      new DateEntityTableColumn<Notification>('createdTime', 'notification.created-time', this.datePipe, '170px'),
      new EntityTableColumn<Notification>('type', 'notification.type', '10%', (notification) =>
        this.translate.instant(NotificationTemplateTypeTranslateMap.get(notification.type).name)),
      new EntityTableColumn<Notification>('subject', 'notification.subject', '30%'),
      new EntityTableColumn<Notification>('text', 'notification.message', '60%')
    );

  }

  private configureCellActions(): Array<CellActionDescriptor<Notification>> {
    return [{
      name: this.translate.instant('notification.mark-as-read'),
      icon: 'check_circle_outline',
      isEnabled: (notification) => notification.status !== NotificationStatus.READ,
      onAction: ($event, entity) => this.markAsRead($event, entity)
    }];
  }

  private markAllRead($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.markAllNotificationsAsRead().subscribe(() => {
      if (this.componentsData.unreadOnly) {
        this.getTable().resetSortAndFilter(true);
      } else {
        this.updateData();
      }
    });
  }

  private markAsRead($event, entity){
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.markNotificationAsRead(entity.id.id).subscribe(() => {
      if (this.componentsData.unreadOnly) {
        this.getTable().dataSource.pageData$.pipe(take(1)).subscribe(
          (value) => {
            if (value.data.length === 1 && this.getTable().pageLink.page) {
              this.getTable().pageLink.page--;
            }
            this.updateData();
          }
        );
      } else {
        entity.status = NotificationStatus.READ;
        this.getTable().detectChanges();
      }
    });
  }

  private showNotification($event: Event, notification: Notification) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<InboxNotificationDialogComponent, InboxNotificationDialogData,
      string>(InboxNotificationDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        notification
      }
    }).afterClosed().subscribe(res => {
      if (res) {
        this.markAsRead(null, notification);
      }
    });
  }
}
