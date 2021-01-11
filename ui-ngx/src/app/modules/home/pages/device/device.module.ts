///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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
import { Lwm2mDeviceTransportConfigurationComponent } from './data/lwm2m-device-transport-configuration.component';

@NgModule({
  declarations: [
    DefaultDeviceConfigurationComponent,
    DeviceConfigurationComponent,
    DefaultDeviceTransportConfigurationComponent,
    MqttDeviceTransportConfigurationComponent,
    Lwm2mDeviceTransportConfigurationComponent,
    DeviceTransportConfigurationComponent,
    DeviceDataComponent,
    DeviceComponent,
    DeviceCredentialsDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule
  ],
  providers: [
    {
      provide: DEVICE_GROUP_CONFIG_FACTORY,
      useClass: DeviceGroupConfigFactory
    }
  ]
})
export class DeviceModule { }
