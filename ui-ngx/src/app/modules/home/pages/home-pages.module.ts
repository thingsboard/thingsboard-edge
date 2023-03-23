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

import { AdminModule } from './admin/admin.module';
import { HomeLinksModule } from './home-links/home-links.module';
import { ProfileModule } from './profile/profile.module';
import { SecurityModule } from '@home/pages/security/security.module';
import { TenantModule } from '@modules/home/pages/tenant/tenant.module';
import { CustomerModule } from '@modules/home/pages/customer/customer.module';
import { AuditLogModule } from '@modules/home/pages/audit-log/audit-log.module';
import { UserModule } from '@modules/home/pages/user/user.module';
import { DeviceModule } from '@modules/home/pages/device/device.module';
import { AssetModule } from '@modules/home/pages/asset/asset.module';
import { EntityViewModule } from '@modules/home/pages/entity-view/entity-view.module';
import { RuleChainModule } from '@modules/home/pages/rulechain/rulechain.module';
import { WidgetLibraryModule } from '@modules/home/pages/widget/widget-library.module';
import { DashboardModule } from '@modules/home/pages/dashboard/dashboard.module';
import { IFrameViewModule } from '@home/pages/iframe/iframe-view.module';
import { ConverterModule } from '@home/pages/converter/converter.module';
import { IntegrationModule } from '@home/pages/integration/integration.module';
import { RoleModule } from '@home/pages/role/role.module';
import { SchedulerModule } from '@home/pages/scheduler/scheduler.module';
import { EntityGroupModule } from '@home/pages/group/entity-group.module';
import { TenantProfileModule } from './tenant-profile/tenant-profile.module';
import { DeviceProfileModule } from './device-profile/device-profile.module';
import { ApiUsageModule } from '@home/pages/api-usage/api-usage.module';
import { EdgeModule } from '@home/pages/edge/edge.module';
import { OtaUpdateModule } from '@home/pages/ota-update/ota-update.module';
import { SolutionTemplatesModule } from '@home/pages/solution-template/solution-templates.module';
import { VcModule } from '@home/pages/vc/vc.module';
import { AssetProfileModule } from '@home/pages/asset-profile/asset-profile.module';
import { ProfilesModule } from '@home/pages/profiles/profiles.module';
import { CloudEventModule } from '@home/pages/cloud-event/cloud-event.module';
import { EdgeStatusModule } from '@home/pages/edge-status/edge-status.module';
import { AlarmModule } from '@home/pages/alarm/alarm.module';
import { EntitiesModule } from '@home/pages/entities/entities.module';
import { FeaturesModule } from '@home/pages/features/features.module';
import { NotificationModule } from '@home/pages/notification/notification.module';
import { IntegrationsCenterModule } from '@home/pages/integration/integrations-center.module';

@NgModule({
  exports: [
    AdminModule,
    HomeLinksModule,
    ProfileModule,
    SecurityModule,
    TenantProfileModule,
    TenantModule,
    DeviceProfileModule,
    AssetProfileModule,
    ProfilesModule,
    EntitiesModule,
    FeaturesModule,
    NotificationModule,
    DeviceModule,
    AssetModule,
    AlarmModule,
    EdgeModule,
    EntityViewModule,
    CustomerModule,
    RuleChainModule,
    WidgetLibraryModule,
    DashboardModule,
    AuditLogModule,
    ApiUsageModule,
    UserModule,
    RoleModule,
    IntegrationsCenterModule,
    ConverterModule,
    IntegrationModule,
    EntityGroupModule,
    IFrameViewModule,
    SchedulerModule,
    OtaUpdateModule,
    SolutionTemplatesModule,
    VcModule,
    CloudEventModule,
    EdgeStatusModule
  ]
})
export class HomePagesModule { }
