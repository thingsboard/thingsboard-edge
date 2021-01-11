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
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { EntityGroupRoutingModule } from '@home/pages/group/entity-group-routing.module';
import { DeviceModule } from '@home/pages/device/device.module';
import { AssetModule } from '@home/pages/asset/asset.module';
import { EntityViewModule } from '@home/pages/entity-view/entity-view.module';
import { DashboardModule } from '@home/pages/dashboard/dashboard.module';
import { UserModule } from '@home/pages/user/user.module';
import { CustomerModule } from '@home/pages/customer/customer.module';
import { CustomersHierarchyComponent } from './customers-hierarchy.component';

@NgModule({
  declarations: [
    CustomersHierarchyComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DeviceModule,
    AssetModule,
    EntityViewModule,
    DashboardModule,
    UserModule,
    CustomerModule,
    EntityGroupRoutingModule
  ],
  providers: [
  ]
})
export class EntityGroupModule { }
