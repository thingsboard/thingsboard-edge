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

import { Type } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { SendRpcRequestComponent } from '@home/components/scheduler/config/send-rpc-request.component';
import { UpdateAttributesComponent } from '@home/components/scheduler/config/update-attributes.component';
import { GenerateReportComponent } from '@home/components/scheduler/config/generate-report.component';
import { OtaUpdateEventConfigComponent } from '@home/components/scheduler/config/ota-update-event-config.component';

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
  },
  updateFirmware: {
    name: 'Update Firmware',
    componentType: OtaUpdateEventConfigComponent,
    originator: false,
    msgType: false,
    metadata: false
  },
  updateSoftware: {
    name: 'Update Software',
    componentType: OtaUpdateEventConfigComponent,
    originator: false,
    msgType: false,
    metadata: false
  }
};

