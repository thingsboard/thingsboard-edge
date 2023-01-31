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
import { DeviceComponent } from '@modules/home/pages/device/device.component';
import { DeviceCredentialsDialogComponent } from '@modules/home/pages/device/device-credentials-dialog.component';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DeviceGroupConfigFactory } from '@home/pages/device/device-group-config.factory';
import { DEVICE_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { DefaultDeviceConfigurationComponent } from './data/default-device-configuration.component';
import { DeviceConfigurationComponent } from './data/device-configuration.component';
import { DeviceDataComponent } from './data/device-data.component';
import { DefaultDeviceTransportConfigurationComponent } from './data/default-device-transport-configuration.component';
import { DeviceTransportConfigurationComponent } from './data/device-transport-configuration.component';
import { MqttDeviceTransportConfigurationComponent } from './data/mqtt-device-transport-configuration.component';
import { CoapDeviceTransportConfigurationComponent } from './data/coap-device-transport-configuration.component';
import { Lwm2mDeviceTransportConfigurationComponent } from './data/lwm2m-device-transport-configuration.component';
import { SnmpDeviceTransportConfigurationComponent } from './data/snmp-device-transport-configuration.component';
import { DeviceCredentialsModule } from '@home/components/device/device-credentials.module';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';

@NgModule({
  declarations: [
    DefaultDeviceConfigurationComponent,
    DeviceConfigurationComponent,
    DefaultDeviceTransportConfigurationComponent,
    MqttDeviceTransportConfigurationComponent,
    CoapDeviceTransportConfigurationComponent,
    Lwm2mDeviceTransportConfigurationComponent,
    SnmpDeviceTransportConfigurationComponent,
    DeviceTransportConfigurationComponent,
    DeviceDataComponent,
    DeviceComponent,
    DeviceCredentialsDialogComponent,
    DeviceCredentialsDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DeviceCredentialsModule,
    DeviceProfileCommonModule,
  ],
  providers: [
    {
      provide: DEVICE_GROUP_CONFIG_FACTORY,
      useClass: DeviceGroupConfigFactory
    }
  ]
})
export class DeviceModule { }
