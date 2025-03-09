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

import {
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { NotificationTarget, NotificationTargetTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { DatePipe } from '@angular/common';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { Observable } from 'rxjs';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class RecipientTableConfigResolver  {

  private readonly config: EntityTableConfig<NotificationTarget> = new EntityTableConfig<NotificationTarget>();

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private customTranslate: CustomTranslatePipe,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.NOTIFICATION_TARGET;
    this.config.detailsPanelEnabled = false;
    this.config.addAsTextButton = true;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_TARGET);
    this.config.entityResources = {} as EntityTypeResource<NotificationTarget>;

    this.config.addEntity = () => this.notificationTargetDialog(null, true);

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTargets(pageLink);

    this.config.deleteEntityTitle = target => this.translate.instant('notification.delete-recipient-title', {recipientName: target.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-recipient-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-recipients-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-recipients-text');

    this.config.deleteEntity = id => this.notificationService.deleteNotificationTarget(id.id);
    this.config.entitySelectionEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.handleRowClick = ($event, target) => {
      this.editTarget($event, target);
      return true;
    };

    this.config.columns.push(
      new DateEntityTableColumn<NotificationTarget>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationTarget>('name', 'notification.recipient-group', '20%'),
      new EntityTableColumn<NotificationTarget>('configuration.type', 'notification.type', '20%',
        (target) => this.translate.instant(NotificationTargetTypeTranslationMap.get(target.configuration.type)),
        () => ({}), false),
      new EntityTableColumn<NotificationTarget>('configuration.description', 'notification.description', '60%',
      (target) => this.customTranslate.transform(target.configuration.description || ''),
      () => ({}), false)
    );
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<NotificationTarget> {
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private editTarget($event: Event, target: NotificationTarget): void {
    $event?.stopPropagation();
    this.notificationTargetDialog(target).subscribe(res => res ? this.config.updateData() : null);
  }

  private notificationTargetDialog(target: NotificationTarget, isAdd = false): Observable<NotificationTarget> {
    return this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        target,
        readonly: !this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE)
      }
    }).afterClosed();
  }
}
