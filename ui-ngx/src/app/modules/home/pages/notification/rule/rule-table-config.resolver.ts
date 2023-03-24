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
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { NotificationRule, TriggerTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleTableHeaderComponent } from '@home/pages/notification/rule/rule-table-header.component';
import {
  RuleNotificationDialogComponent,
  RuleNotificationDialogData
} from '@home/pages/notification/rule/rule-notification-dialog.component';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Injectable } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class RuleTableConfigResolver implements Resolve<EntityTableConfig<NotificationRule>> {

  private readonly config: EntityTableConfig<NotificationRule> = new EntityTableConfig<NotificationRule>();

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.NOTIFICATION_RULE;
    this.config.detailsPanelEnabled = false;
    this.config.addEnabled = false;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_RULE);
    this.config.entityResources = {} as EntityTypeResource<NotificationRule>;

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRules(pageLink);

    this.config.deleteEntityTitle = rule => this.translate.instant('notification.delete-rule-title', {ruleName: rule.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-rule-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-rules-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-rules-text');
    this.config.deleteEntity = id => this.notificationService.deleteNotificationRule(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();
    this.config.headerComponent = RuleTableHeaderComponent;
    this.config.onEntityAction = action => this.onTargetAction(action);

    this.config.deleteEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);
    this.config.entitySelectionEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.handleRowClick = ($event, rule) => {
      this.editRule($event, rule);
      return true;
    };

    this.config.columns.push(
      new DateEntityTableColumn<NotificationRule>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationRule>('name', 'notification.rule-name', '30%'),
      new EntityTableColumn<NotificationRule>('templateName', 'notification.template', '20%'),
      new EntityTableColumn<NotificationRule>('triggerType', 'notification.trigger.trigger', '20%',
        (rule) => this.translate.instant(TriggerTypeTranslationMap.get(rule.triggerType)) || '',
        () => ({}), true),
      new EntityTableColumn<NotificationRule>('additionalConfig.description', 'notification.description', '30%',
        (target) => target.additionalConfig.description || '',
        () => ({}), false)
    );
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<NotificationRule> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRule>> {
    return [{
      name: this.translate.instant('notification.copy-rule'),
      icon: 'content_copy',
      isEnabled: () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE),
      onAction: ($event, entity) => this.editRule($event, entity, false, true)
    }];
  }

  private editRule($event: Event, rule: NotificationRule, isAdd = false, isCopy = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RuleNotificationDialogComponent, RuleNotificationDialogData,
      NotificationRule>(RuleNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isCopy,
        rule,
        readonly: !this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE)
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  private onTargetAction(action: EntityAction<NotificationRule>): boolean {
    switch (action.action) {
      case 'add':
        this.editRule(action.event, action.entity, true);
        return true;
    }
    return false;
  }
}
