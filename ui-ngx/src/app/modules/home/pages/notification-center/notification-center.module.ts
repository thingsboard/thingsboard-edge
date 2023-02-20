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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { NotificationCenterRoutingModule } from './notification-center-routing.module';
import { NotificationCenterComponent } from './notification-center.component';
import { HomeComponentsModule } from '@home/components/home-components.module';
import {
  TargetNotificationDialogComponent
} from '@home/pages/notification-center/targets-table/target-notification-dialog.componet';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import {
  TemplateNotificationDialogComponent
} from '@home/pages/notification-center/template-table/template-notification-dialog.component';
import {
  TargetTableHeaderComponent
} from '@home/pages/notification-center/targets-table/target-table-header.component';
import {
  TemplateTableHeaderComponent
} from '@home/pages/notification-center/template-table/template-table-header.component';
import {
  RequestNotificationDialogComponent
} from '@home/pages/notification-center/request-table/request-notification-dialog.componet';
import {
  TemplateAutocompleteComponent
} from '@home/pages/notification-center/template-table/template-autocomplete.component';
import { InboxTableHeaderComponent } from '@home/pages/notification-center/inbox-table/inbox-table-header.component';
import { RuleTableHeaderComponent } from '@home/pages/notification-center/rule-table/rule-table-header.component';
import {
  RuleNotificationDialogComponent
} from '@home/pages/notification-center/rule-table/rule-notification-dialog.component';
import { EscalationsComponent } from '@home/pages/notification-center/rule-table/escalations.component';
import { EscalationFormComponent } from '@home/pages/notification-center/rule-table/escalation-form.component';
import {
  AlarmSeveritiesListComponent
} from '@home/pages/notification-center/rule-table/alarm-severities-list.component';
import { TemplateConfiguration } from '@home/pages/notification-center/template-table/template-configuration';

@NgModule({
  declarations: [
    NotificationCenterComponent,
    TargetNotificationDialogComponent,
    TemplateNotificationDialogComponent,
    RequestNotificationDialogComponent,
    TargetTableHeaderComponent,
    TemplateTableHeaderComponent,
    NotificationTableComponent,
    TemplateAutocompleteComponent,
    InboxTableHeaderComponent,
    RuleTableHeaderComponent,
    RuleNotificationDialogComponent,
    EscalationsComponent,
    EscalationFormComponent,
    AlarmSeveritiesListComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationCenterRoutingModule,
    HomeComponentsModule
  ]
})
export class NotificationCenterModule { }
