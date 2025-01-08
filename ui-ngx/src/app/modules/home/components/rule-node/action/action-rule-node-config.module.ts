///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { NgModule, Type } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IRuleNodeConfigurationComponent, SharedModule } from '@shared/public-api';
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
    DeviceStateConfigComponent
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
    DeviceStateConfigComponent
  ]
})
export class ActionRuleNodeConfigModule {
}

export const actionRuleNodeConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  'tbActionNodeAssignToCustomerConfig': AssignCustomerConfigComponent,
  'tbActionNodeAttributesConfig': AttributesConfigComponent,
  'tbActionNodeClearAlarmConfig': ClearAlarmConfigComponent,
  'tbActionNodeCreateAlarmConfig': CreateAlarmConfigComponent,
  'tbActionNodeCreateRelationConfig': CreateRelationConfigComponent,
  'tbActionNodeDeleteAttributesConfig': DeleteAttributesConfigComponent,
  'tbActionNodeDeleteRelationConfig': DeleteRelationConfigComponent,
  'tbActionNodeDeviceProfileConfig': DeviceProfileConfigComponent,
  'tbActionNodeDeviceStateConfig': DeviceStateConfigComponent,
  'tbActionNodeGeneratorConfig': GeneratorConfigComponent,
  'tbActionNodeGpsGeofencingConfig': GpsGeoActionConfigComponent,
  'tbActionNodeLogConfig': LogConfigComponent,
  'tbActionNodeMathFunctionConfig': MathFunctionConfigComponent,
  'tbActionNodeMsgCountConfig': MsgCountConfigComponent,
  'tbActionNodeMsgDelayConfig': MsgDelayConfigComponent,
  'tbActionNodePushToCloudConfig': PushToCloudConfigComponent,
  'tbActionNodePushToEdgeConfig': PushToEdgeConfigComponent,
  'tbActionNodeRpcReplyConfig': RpcReplyConfigComponent,
  'tbActionNodeRpcRequestConfig': RpcRequestConfigComponent,
  'tbActionNodeCustomTableConfig': SaveToCustomTableConfigComponent,
  'tbActionNodeSendRestApiCallReplyConfig': SendRestApiCallReplyConfigComponent,
  'tbActionNodeTimeseriesConfig': TimeseriesConfigComponent,
};
