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
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { NotificationRule, TriggerTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleTableHeaderComponent } from '@home/pages/notification-center/rule-table/rule-table-header.component';
import {
  RuleNotificationDialogComponent,
  RuleNotificationDialogData
} from '@home/pages/notification-center/rule-table/rule-notification-dialog.component';

export class RuleTableConfig extends EntityTableConfig<NotificationRule> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
    this.loadDataOnInit = false;
    this.rowPointer = true;

    this.entityTranslations = {
      noEntities: 'notification.no-rules-notification',
      search: 'notification.search-rules'
    };
    this.entityResources = {} as EntityTypeResource<NotificationRule>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRules(pageLink);

    this.deleteEntityTitle = rule => this.translate.instant('notification.delete-rule-title', {ruleName: rule.name});
    this.deleteEntityContent = () => this.translate.instant('notification.delete-rule-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationRule(id.id);

    this.cellActionDescriptors = this.configureCellActions();
    this.headerComponent = RuleTableHeaderComponent;
    this.onEntityAction = action => this.onTargetAction(action);

    this.defaultSortOrder = {property: 'name', direction: Direction.ASC};

    this.handleRowClick = ($event, rule) => {
      this.editRule($event, rule);
      return true;
    };

    this.columns.push(
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

  private configureCellActions(): Array<CellActionDescriptor<NotificationRule>> {
    return [{
      name: this.translate.instant('notification.copy-rule'),
      icon: 'content_copy',
      isEnabled: () => true,
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
        rule
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.updateData();
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
