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
import { NotificationRoutingModule } from '@home/pages/notification/notification-routing.module';
import { InboxTableHeaderComponent } from '@home/pages/notification/inbox/inbox-table-header.component';
import { InboxNotificationDialogComponent } from '@home/pages/notification/inbox/inbox-notification-dialog.component';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { SentErrorDialogComponent } from '@home/pages/notification/sent/sent-error-dialog.component';
import { SentNotificationDialogComponent } from '@home/pages/notification/sent/sent-notification-dialog.componet';
import {
  RecipientNotificationDialogComponent
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { RecipientTableHeaderComponent } from '@home/pages/notification/recipient/recipient-table-header.component';
import { TemplateAutocompleteComponent } from '@home/pages/notification/template/template-autocomplete.component';
import {
  TemplateNotificationDialogComponent
} from '@home/pages/notification/template/template-notification-dialog.component';
import { TemplateTableHeaderComponent } from '@home/pages/notification/template/template-table-header.component';
import { EscalationFormComponent } from '@home/pages/notification/rule/escalation-form.component';
import { EscalationsComponent } from '@home/pages/notification/rule/escalations.component';
import { RuleNotificationDialogComponent } from '@home/pages/notification/rule/rule-notification-dialog.component';
import { RuleTableHeaderComponent } from '@home/pages/notification/rule/rule-table-header.component';

@NgModule({
  declarations: [
    InboxTableHeaderComponent,
    InboxNotificationDialogComponent,
    SentErrorDialogComponent,
    SentNotificationDialogComponent,
    RecipientNotificationDialogComponent,
    RecipientTableHeaderComponent,
    TemplateAutocompleteComponent,
    TemplateNotificationDialogComponent,
    TemplateTableHeaderComponent,
    EscalationFormComponent,
    EscalationsComponent,
    RuleNotificationDialogComponent,
    RuleTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationRoutingModule,
    HomeComponentsModule
  ]
})
export class NotificationModule { }
