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
import { KvMapConfigComponent } from './kv-map-config.component';
import { DeviceRelationsQueryConfigComponent } from './device-relations-query-config.component';
import { RelationsQueryConfigComponent } from './relations-query-config.component';
import { MessageTypesConfigComponent } from './message-types-config.component';
import { CredentialsConfigComponent } from './credentials-config.component';
import { ArgumentsMapConfigComponent } from './arguments-map-config.component';
import { MathFunctionAutocompleteComponent } from './math-function-autocomplete.component';
import { OutputMessageTypeAutocompleteComponent } from './output-message-type-autocomplete.component';
import { KvMapConfigOldComponent } from './kv-map-config-old.component';
import { MsgMetadataChipComponent } from './msg-metadata-chip.component';
import { SvMapConfigComponent } from './sv-map-config.component';
import { RelationsQueryConfigOldComponent } from './relations-query-config-old.component';
import { SelectAttributesComponent } from './select-attributes.component';
import { AlarmStatusSelectComponent } from './alarm-status-select.component';
import { ExampleHintComponent } from './example-hint.component';
import { TimeUnitInputComponent } from './time-unit-input.component';
import { TargetEntityComponent } from '@home/components/rule-node/common/target-entity.component';

@NgModule({
  declarations: [
    KvMapConfigComponent,
    DeviceRelationsQueryConfigComponent,
    RelationsQueryConfigComponent,
    MessageTypesConfigComponent,
    CredentialsConfigComponent,
    ArgumentsMapConfigComponent,
    MathFunctionAutocompleteComponent,
    OutputMessageTypeAutocompleteComponent,
    KvMapConfigOldComponent,
    MsgMetadataChipComponent,
    SvMapConfigComponent,
    RelationsQueryConfigOldComponent,
    SelectAttributesComponent,
    AlarmStatusSelectComponent,
    ExampleHintComponent,
    TimeUnitInputComponent,
    TargetEntityComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    KvMapConfigComponent,
    DeviceRelationsQueryConfigComponent,
    RelationsQueryConfigComponent,
    MessageTypesConfigComponent,
    CredentialsConfigComponent,
    ArgumentsMapConfigComponent,
    MathFunctionAutocompleteComponent,
    OutputMessageTypeAutocompleteComponent,
    KvMapConfigOldComponent,
    MsgMetadataChipComponent,
    SvMapConfigComponent,
    RelationsQueryConfigOldComponent,
    SelectAttributesComponent,
    AlarmStatusSelectComponent,
    ExampleHintComponent,
    TimeUnitInputComponent,
    TargetEntityComponent,
  ]
})

export class CommonRuleNodeConfigModule {
}
