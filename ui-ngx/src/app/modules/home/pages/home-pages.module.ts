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

import { AdminModule } from './admin/admin.module';
import { HomeLinksModule } from './home-links/home-links.module';
import { ProfileModule } from './profile/profile.module';
import { TenantModule } from '@modules/home/pages/tenant/tenant.module';
import { AuditLogModule } from '@modules/home/pages/audit-log/audit-log.module';
import { UserModule } from '@modules/home/pages/user/user.module';
import { RuleChainModule } from '@modules/home/pages/rulechain/rulechain.module';
import { WidgetLibraryModule } from '@modules/home/pages/widget/widget-library.module';
import { IFrameViewModule } from '@home/pages/iframe/iframe-view.module';
import { ConverterModule } from '@home/pages/converter/converter.module';
import { IntegrationModule } from '@home/pages/integration/integration.module';
import { RoleModule } from '@home/pages/role/role.module';
import { SchedulerModule } from '@home/pages/scheduler/scheduler.module';
import { EntityGroupModule } from '@home/pages/group/entity-group.module';
import { TenantProfileModule } from './tenant-profile/tenant-profile.module';
import { MODULES_MAP } from '@shared/public-api';
import { modulesMap } from '../../common/modules-map';
import { DeviceProfileModule } from './device-profile/device-profile.module';
import { ApiUsageModule } from '@home/pages/api-usage/api-usage.module';

@NgModule({
  exports: [
    AdminModule,
    HomeLinksModule,
    ProfileModule,
    TenantProfileModule,
    TenantModule,
    DeviceProfileModule,
    RuleChainModule,
    WidgetLibraryModule,
    AuditLogModule,
    ApiUsageModule,
    UserModule,
    RoleModule,
    ConverterModule,
    IntegrationModule,
    EntityGroupModule,
    IFrameViewModule,
    SchedulerModule
  ],
  providers: [
    {
      provide: MODULES_MAP,
      useValue: modulesMap
    }
  ]
})
export class HomePagesModule { }
