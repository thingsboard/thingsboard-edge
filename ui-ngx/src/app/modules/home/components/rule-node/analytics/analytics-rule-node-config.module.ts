///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import { SharedModule } from '@shared/public-api';
import { CommonRuleNodeConfigModule } from '@home/components/rule-node/common/common-rule-node-config.module';

import { AggregateIncomingConfigComponent } from './aggregate-incoming-config.component';
import { AggregateLatestConfigComponent } from './aggregate-latest-config.component';
import { AggregateLatestMappingTableComponent } from './aggregate-latest-mapping-table.component';
import { AggregateLatestMappingDialogComponent } from './aggregate-latest-mapping-dialog.component';
import { AlarmsCountConfigComponent } from './alarms-count-config.component';
import { AlarmsCountMappingTableComponent } from './alarms-count-mapping-table.component';
import { AlarmsCountMappingDialogComponent } from './alarms-count-mapping-dialog.component';
import { AlarmsCountV2ConfigComponent } from './alarms-count-v2-config.component';
import { AggregateLatestV2ConfigComponent } from './aggregate-latest-v2-config.component';

@NgModule({
  declarations: [
    AggregateIncomingConfigComponent,
    AggregateLatestMappingDialogComponent,
    AggregateLatestMappingTableComponent,
    AggregateLatestConfigComponent,
    AlarmsCountMappingDialogComponent,
    AlarmsCountMappingTableComponent,
    AlarmsCountConfigComponent,
    AlarmsCountV2ConfigComponent,
    AggregateLatestV2ConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    AggregateIncomingConfigComponent,
    AggregateLatestConfigComponent,
    AlarmsCountConfigComponent,
    AlarmsCountV2ConfigComponent,
    AggregateLatestV2ConfigComponent
  ]
})

export class AnalyticsRuleNodeConfigModule {
}
