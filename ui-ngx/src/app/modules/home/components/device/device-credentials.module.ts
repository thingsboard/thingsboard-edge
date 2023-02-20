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
import { CopyDeviceCredentialsComponent } from '@home/components/device/copy-device-credentials.component';
import { DeviceCredentialsComponent } from '@home/components/device/device-credentials.component';
import { DeviceCredentialsLwm2mComponent } from '@home/components/device/device-credentials-lwm2m.component';
import { DeviceCredentialsLwm2mServerComponent } from '@home/components/device/device-credentials-lwm2m-server.component';
import { DeviceCredentialsMqttBasicComponent } from '@home/components/device/device-credentials-mqtt-basic.component';

@NgModule({
  declarations: [
    CopyDeviceCredentialsComponent,
    DeviceCredentialsComponent,
    DeviceCredentialsLwm2mComponent,
    DeviceCredentialsLwm2mServerComponent,
    DeviceCredentialsMqttBasicComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    CopyDeviceCredentialsComponent,
    DeviceCredentialsComponent,
    DeviceCredentialsLwm2mComponent,
    DeviceCredentialsLwm2mServerComponent,
    DeviceCredentialsMqttBasicComponent
  ]
})
export class DeviceCredentialsModule { }
