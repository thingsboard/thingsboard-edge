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
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { AssetComponent } from './asset.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ASSET_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { AssetGroupConfigFactory } from '@home/pages/asset/asset-group-config.factory';

@NgModule({
  declarations: [
    AssetComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule
  ],
  providers: [
    {
      provide: ASSET_GROUP_CONFIG_FACTORY,
      useClass: AssetGroupConfigFactory
    }
  ]
})
export class AssetModule { }
