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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { NotificationService } from '@core/http/notification.service';
import { TargetsTableConfig } from '@home/pages/notification-center/notification-table/targets-table-config';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { InboxTableConfig } from '@home/pages/notification-center/notification-table/inbox-table-config';
import { DatePipe } from '@angular/common';
import { TemplateTableConfig } from '@home/pages/notification-center/notification-table/template-table-config';
import { RequestTableConfig } from '@home/pages/notification-center/notification-table/request-table-config';
import { RuleTableConfig } from '@home/pages/notification-center/notification-table/rule-table-config';

@Component({
  selector: 'tb-notification-table',
  templateUrl: './notification-table.component.html'
})
export class NotificationTableComponent implements OnInit {

  @Input()
  notificationType = EntityType.NOTIFICATION;

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  entityTableConfig: EntityTableConfig<any>;

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.entityTableConfig = this.getTableConfig();
    this.entityTableConfig.pageMode = false;
    this.entityTableConfig.detailsPanelEnabled = false;
    this.entityTableConfig.selectionEnabled = false;
    this.entityTableConfig.addEnabled = false;
  }

  updateData() {
    this.entitiesTable.updateData();
  }

  private getTableConfig(): EntityTableConfig<any> {
    switch (this.notificationType) {
      case EntityType.NOTIFICATION_TARGET:
        return new TargetsTableConfig(
          this.notificationService,
          this.translate,
          this.dialog
        );
      case EntityType.NOTIFICATION:
        return new InboxTableConfig(
          this.notificationService,
          this.translate,
          this.dialog,
          this.datePipe
        );
      case EntityType.NOTIFICATION_TEMPLATE:
        return new TemplateTableConfig(
          this.notificationService,
          this.translate,
          this.dialog
        );
      case EntityType.NOTIFICATION_REQUEST:
        return new RequestTableConfig(
          this.notificationService,
          this.translate,
          this.dialog,
          this.datePipe
        );
      case EntityType.NOTIFICATION_RULE:
        return new RuleTableConfig(
          this.notificationService,
          this.translate,
          this.dialog
        );
    }
  }
}
