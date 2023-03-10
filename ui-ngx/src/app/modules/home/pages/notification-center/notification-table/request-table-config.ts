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
import {
  NotificationDeliveryMethodTranslateMap,
  NotificationRequest,
  NotificationRequestInfo,
  NotificationRequestStats,
  NotificationRequestStatus,
  NotificationRequestStatusTranslateMap,
  NotificationTemplate
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  RequestNotificationDialogComponent,
  RequestNotificationDialogData
} from '@home/pages/notification-center/request-table/request-notification-dialog.componet';
import { PageLink } from '@shared/models/page/page-link';
import {
  NotificationRequestErrorDialogComponent,
  NotificationRequestErrorDialogData
} from '@home/pages/notification-center/request-table/notification-request-error-dialog.component';

export class RequestTableConfig extends EntityTableConfig<NotificationRequest, PageLink, NotificationRequestInfo> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {
    super();
    this.loadDataOnInit = false;
    this.searchEnabled = false;
    this.entityTranslations = {
      noEntities: 'notification.no-notification-request',
    };
    this.entityResources = {} as EntityTypeResource<NotificationRequest>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRequests(pageLink);

    this.deleteEnabled = (request) => request.status === NotificationRequestStatus.SCHEDULED;
    this.deleteEntityTitle = () => this.translate.instant('notification.delete-request-title');
    this.deleteEntityContent = () => this.translate.instant('notification.delete-request-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationRequest(id.id);

    this.cellActionDescriptors = this.configureCellActions();

    this.onEntityAction = action => this.onRequestAction(action);

    this.handleRowClick = (event, entity) => {
      const path = (event as any).path || (event.composedPath && event.composedPath());
      if ((event.target as HTMLElement).getElementsByClassName('stats').length || (event.target as HTMLElement).className === 'stats') {
        this.openStatsErrorDialog(event, entity);
      }
      return true;
    };

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<NotificationRequestInfo>('createdTime', 'notification.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationRequestInfo>('status', 'notification.status', '15%',
        request => `<span style="display: flex;">${this.requestStatus(request.status)}${this.requestStats(request.stats)}</span>`,
          request => this.requestStatusStyle(request.status)),
      new EntityTableColumn<NotificationRequest>('deliveryMethods', 'notification.delivery-method', '15%',
        (request) => request.deliveryMethods
          .map((deliveryMethod) => this.translate.instant(NotificationDeliveryMethodTranslateMap.get(deliveryMethod))).join(', '),
        () => ({}), false),
      new EntityTableColumn<NotificationRequest>('templateName', 'notification.template', '70%')
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRequestInfo>> {
    return [{
      name: this.translate.instant('notification.notify-again'),
      mdiIcon: 'mdi:repeat-variant',
      isEnabled: (request) => request.status !== NotificationRequestStatus.SCHEDULED,
      onAction: ($event, entity) => this.createRequest($event, entity)
    }];
  }

  private createRequest($event: Event, request: NotificationRequest, isAdd = false, updateData = true) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RequestNotificationDialogComponent, RequestNotificationDialogData,
      NotificationTemplate>(RequestNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        request
      }
    }).afterClosed().subscribe((res) => {
      if (res && updateData) {
        this.updateData();
      }
    });
  }

  private onRequestAction(action: EntityAction<NotificationRequest>): boolean {
    switch (action.action) {
      case 'add':
        this.createRequest(action.event, action.entity, true);
        return true;
      case 'add-without-update':
        this.createRequest(action.event, action.entity, true, false);
        return true;
    }
    return false;
  }

  private requestStatus(status: NotificationRequestStatus): string {
    const translateKey = NotificationRequestStatusTranslateMap.get(status);
    let backgroundColor = 'rgba(25, 128, 56, 0.08)';
    switch (status) {
      case NotificationRequestStatus.SCHEDULED:
        backgroundColor = 'rgba(48, 86, 128, 0.08)';
        break;
      case NotificationRequestStatus.PROCESSING:
        backgroundColor = 'rgba(212, 125, 24, 0.08)';
        break;
    }
    return `<div style="border-radius: 12px; height: 24px; line-height: 24px; padding: 0 10px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private requestStats(stats: NotificationRequestStats): string {
    let countError = 0;
    Object.keys(stats.errors).forEach(method => countError += Object.keys(stats.errors[method]).length);
    if (countError === 0) {
      return ''
    }
    return `<div style="border-radius: 12px; height: 24px; line-height: 24px; padding: 0 10px; width: max-content; background-color: #D12730;
                        color: #fff; font-weight: 500; margin-left: 8px" class="stats">
                ${countError} ${this.translate.instant('notification.fails')} >
            </div>`;
  }

  private requestStatusStyle(status: NotificationRequestStatus): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038',
      cursor: 'pointer'
    };
    switch (status) {
      case NotificationRequestStatus.SCHEDULED:
        styleObj.color = '#305680';
        break;
      case NotificationRequestStatus.PROCESSING:
        styleObj.color = '#D47D18';
        break;
    }
    return styleObj;
  }

  private openStatsErrorDialog($event: Event, notificationRequest: NotificationRequest) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<NotificationRequestErrorDialogComponent, NotificationRequestErrorDialogData,
      void>(NotificationRequestErrorDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        notificationRequest
      }
    }).afterClosed().subscribe(() => {});
  }
}
