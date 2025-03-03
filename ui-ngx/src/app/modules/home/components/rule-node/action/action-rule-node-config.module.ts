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
import { HomeComponentsModule } from '@home/components/public-api';
import { AttributesConfigComponent } from './attributes-config.component';
import { TimeseriesConfigComponent } from './timeseries-config.component';
import { RpcRequestConfigComponent } from './rpc-request-config.component';
import { LogConfigComponent } from './log-config.component';
import { AssignCustomerConfigComponent } from './assign-customer-config.component';
import { ClearAlarmConfigComponent } from './clear-alarm-config.component';
import { CreateAlarmConfigComponent } from './create-alarm-config.component';
import { CreateRelationConfigComponent } from './create-relation-config.component';
import { MsgDelayConfigComponent } from './msg-delay-config.component';
import { DeleteRelationConfigComponent } from './delete-relation-config.component';
import { GeneratorConfigComponent } from './generator-config.component';
import { GpsGeoActionConfigComponent } from './gps-geo-action-config.component';
import { MsgCountConfigComponent } from './msg-count-config.component';
import { RpcReplyConfigComponent } from './rpc-reply-config.component';
import { SaveToCustomTableConfigComponent } from './save-to-custom-table-config.component';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';
import { UnassignCustomerConfigComponent } from './unassign-customer-config.component';
import { DeviceProfileConfigComponent } from './device-profile-config.component';
import { PushToEdgeConfigComponent } from './push-to-edge-config.component';
import { PushToCloudConfigComponent } from './push-to-cloud-config.component';
import { DeleteAttributesConfigComponent } from './delete-attributes-config.component';
import { MathFunctionConfigComponent } from './math-function-config.component';
import { DeviceStateConfigComponent } from './device-state-config.component';
import { SendRestApiCallReplyConfigComponent } from './send-rest-api-call-reply-config.component';
import { AddToGroupConfigComponent } from '@home/components/rule-node/action/add-to-group-config.component';
import { ChangeOwnerConfigComponent } from '@home/components/rule-node/action/change-owner-config.component';
import { GenerateReportConfigComponent } from '@home/components/rule-node/action/generate-report-config.component';
import {
  IntegrationDownlinkConfigComponent
} from '@home/components/rule-node/action/integration-downlink-config.component';
import { RemoveFromGroupConfigComponent } from '@home/components/rule-node/action/remove-from-group-config.component';
import {
  AdvancedProcessingSettingComponent
} from '@home/components/rule-node/action/advanced-processing-setting.component';
import {
  AdvancedProcessingSettingRowComponent
} from '@home/components/rule-node/action/advanced-processing-setting-row.component';

@NgModule({
  declarations: [
    DeleteAttributesConfigComponent,
    AttributesConfigComponent,
    TimeseriesConfigComponent,
    RpcRequestConfigComponent,
    LogConfigComponent,
    AssignCustomerConfigComponent,
    ClearAlarmConfigComponent,
    CreateAlarmConfigComponent,
    CreateRelationConfigComponent,
    MsgDelayConfigComponent,
    DeleteRelationConfigComponent,
    GeneratorConfigComponent,
    GpsGeoActionConfigComponent,
    MsgCountConfigComponent,
    RpcReplyConfigComponent,
    SaveToCustomTableConfigComponent,
    UnassignCustomerConfigComponent,
    SendRestApiCallReplyConfigComponent,
    DeviceProfileConfigComponent,
    PushToEdgeConfigComponent,
    PushToCloudConfigComponent,
    MathFunctionConfigComponent,
    DeviceStateConfigComponent,
    AdvancedProcessingSettingComponent,
    AdvancedProcessingSettingRowComponent,
    AddToGroupConfigComponent,
    ChangeOwnerConfigComponent,
    GenerateReportConfigComponent,
    IntegrationDownlinkConfigComponent,
    RemoveFromGroupConfigComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    DeleteAttributesConfigComponent,
    AttributesConfigComponent,
    TimeseriesConfigComponent,
    RpcRequestConfigComponent,
    LogConfigComponent,
    AssignCustomerConfigComponent,
    ClearAlarmConfigComponent,
    CreateAlarmConfigComponent,
    CreateRelationConfigComponent,
    MsgDelayConfigComponent,
    DeleteRelationConfigComponent,
    GeneratorConfigComponent,
    GpsGeoActionConfigComponent,
    MsgCountConfigComponent,
    RpcReplyConfigComponent,
    UnassignCustomerConfigComponent,
    SaveToCustomTableConfigComponent,
    SendRestApiCallReplyConfigComponent,
    DeviceProfileConfigComponent,
    PushToEdgeConfigComponent,
    PushToCloudConfigComponent,
    MathFunctionConfigComponent,
    DeviceStateConfigComponent,
    AddToGroupConfigComponent,
    ChangeOwnerConfigComponent,
    GenerateReportConfigComponent,
    IntegrationDownlinkConfigComponent,
    RemoveFromGroupConfigComponent,
  ]
})
export class ActionRuleNodeConfigModule {
}
