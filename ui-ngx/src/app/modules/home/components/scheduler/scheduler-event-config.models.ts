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

import { Type } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { SendRpcRequestComponent } from '@home/components/scheduler/config/send-rpc-request.component';
import { UpdateAttributesComponent } from '@home/components/scheduler/config/update-attributes.component';
import { GenerateReportComponent } from '@home/components/scheduler/config/generate-report.component';

export interface SchedulerEventConfigType {
  name: string;
  componentType?: Type<ControlValueAccessor>;
  template?: string;
  originator?: boolean;
  msgType?: boolean;
  metadata?: boolean;
}

// Example of custom scheduler event config type

/*
test = {
  originator: true,
  msgType: true,
  template: '<form #myCustomConfigForm="ngForm">' +
    '<mat-form-field class="mat-block">' +
    '<mat-label>My custom field</mat-label>' +
    '<input name="myField" #myField="ngModel" matInput [(ngModel)]="configuration.msgBody.myField" required>' +
    '<mat-error *ngIf="myField.hasError(\'required\')">' +
    'My field is required.' +
    '</mat-error>' +
    '</mat-form-field>' +
    '<div>Form valid: {{myCustomConfigForm.valid}}</div>' +
    '</form>',
  name: 'Test!'
}*/

export const defaultSchedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType} = {
  generateReport: {
    name: 'Generate Report',
    componentType: GenerateReportComponent,
    originator: false,
    msgType: false,
    metadata: false
  },
  updateAttributes: {
    name: 'Update Attributes',
    componentType: UpdateAttributesComponent,
    originator: false,
    msgType: false,
    metadata: false
  },
  sendRpcRequest: {
    name: 'Send RPC Request to Device',
    componentType: SendRpcRequestComponent,
    originator: false,
    msgType: false,
    metadata: false
  }
};

