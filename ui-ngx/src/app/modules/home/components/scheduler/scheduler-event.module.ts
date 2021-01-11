///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
