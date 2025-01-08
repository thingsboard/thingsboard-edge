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
import { EmptyConfigComponent } from './empty-config.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  ruleNodeActionConfigComponentsMap,
  RuleNodeConfigActionModule
} from '@home/components/rule-node/action/rule-node-config-action.module';
import {
  RuleNodeConfigFilterModule,
  ruleNodeFilterConfigComponentsMap
} from '@home/components/rule-node/filter/rule-node-config-filter.module';
import {
  RuleNodeCoreEnrichmentModule,
  ruleNodeEnrichmentConfigComponentsMap
} from '@home/components/rule-node/enrichment/rule-node-core-enrichment.module';
import {
  RuleNodeConfigExternalModule,
  ruleNodeExternalConfigComponentsMap
} from '@home/components/rule-node/external/rule-node-config-external.module';
import {
  RuleNodeConfigTransformModule,
  ruleNodeTransformConfigComponentsMap
} from '@home/components/rule-node/transform/rule-node-config-transform.module';
import {
  RuleNodeConfigFlowModule,
  ruleNodeFlowConfigComponentsMap
} from '@home/components/rule-node/flow/rule-node-config-flow.module';
import { IRuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@NgModule({
  declarations: [
    EmptyConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    RuleNodeConfigActionModule,
    RuleNodeConfigFilterModule,
    RuleNodeCoreEnrichmentModule,
    RuleNodeConfigExternalModule,
    RuleNodeConfigTransformModule,
    RuleNodeConfigFlowModule,
    EmptyConfigComponent
  ]
})
export class RuleNodeConfigModule {}

export const ruleNodeConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  ...ruleNodeActionConfigComponentsMap,
  ...ruleNodeEnrichmentConfigComponentsMap,
  ...ruleNodeExternalConfigComponentsMap,
  ...ruleNodeFilterConfigComponentsMap,
  ...ruleNodeFlowConfigComponentsMap,
  ...ruleNodeTransformConfigComponentsMap,
  'tbNodeEmptyConfig': EmptyConfigComponent
};
