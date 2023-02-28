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
import { SharedModule } from '@app/shared/shared.module';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { SchedulerEventDialogComponent } from '@home/components/scheduler/scheduler-event-dialog.component';
import { SchedulerEventTypeAutocompleteComponent } from '@home/components/scheduler/scheduler-event-type-autocomplete.component';
import { SchedulerEventConfigComponent } from '@home/components/scheduler/scheduler-event-config.component';
import { SchedulerEventTemplateConfigComponent } from '@home/components/scheduler/scheduler-event-template-config.component';
import { SendRpcRequestComponent } from '@home/components/scheduler/config/send-rpc-request.component';
import { UpdateAttributesComponent } from '@home/components/scheduler/config/update-attributes.component';
import { AttributeKeyValueTableComponent } from '@home/components/scheduler/config/attribute-key-value-table.component';
import { GenerateReportComponent } from '@home/components/scheduler/config/generate-report.component';
import { ReportConfigComponent } from '@home/components/scheduler/config/report-config.component';
import { SelectDashboardStateDialogComponent } from '@home/components/scheduler/config/select-dashboard-state-dialog.component';
import { EmailConfigComponent } from '@home/components/scheduler/config/email-config.component';
import { SchedulerEventScheduleComponent } from '@home/components/scheduler/scheduler-event-schedule.component';

@NgModule({
  declarations:
    [
      SchedulerEventsComponent,
      SchedulerEventTypeAutocompleteComponent,
      SchedulerEventConfigComponent,
      SchedulerEventTemplateConfigComponent,
      SendRpcRequestComponent,
      UpdateAttributesComponent,
      AttributeKeyValueTableComponent,
      GenerateReportComponent,
      ReportConfigComponent,
      EmailConfigComponent,
      SelectDashboardStateDialogComponent,
      SchedulerEventScheduleComponent,
      SchedulerEventDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    SchedulerEventsComponent,
    SchedulerEventTypeAutocompleteComponent,
    SchedulerEventConfigComponent,
    SchedulerEventTemplateConfigComponent,
    SendRpcRequestComponent,
    UpdateAttributesComponent,
    AttributeKeyValueTableComponent,
    GenerateReportComponent,
    ReportConfigComponent,
    EmailConfigComponent,
    SelectDashboardStateDialogComponent,
    SchedulerEventScheduleComponent,
    SchedulerEventDialogComponent
  ]
})
export class SchedulerEventModule { }
