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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { RuleChainRoutingModule } from '@modules/home/pages/rulechain/rulechain-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import {
  AddRuleNodeDialogComponent,
  AddRuleNodeLinkDialogComponent, CreateNestedRuleChainDialogComponent,
  RuleChainPageComponent
} from './rulechain-page.component';
import { RuleNodeComponent } from '@home/pages/rulechain/rulenode.component';
import { FC_NODE_COMPONENT_CONFIG } from 'ngx-flowchart';
import { RuleNodeDetailsComponent } from './rule-node-details.component';
import { RuleNodeLinkComponent } from './rule-node-link.component';
import { LinkLabelsComponent } from '@home/pages/rulechain/link-labels.component';
import { RuleNodeConfigComponent } from './rule-node-config.component';

@NgModule({
  declarations: [
    RuleChainComponent,
    RuleChainTabsComponent,
    RuleChainPageComponent,
    RuleNodeComponent,
    RuleNodeDetailsComponent,
    RuleNodeConfigComponent,
    LinkLabelsComponent,
    RuleNodeLinkComponent,
    AddRuleNodeLinkDialogComponent,
    AddRuleNodeDialogComponent,
    CreateNestedRuleChainDialogComponent
  ],
  providers: [
    {
      provide: FC_NODE_COMPONENT_CONFIG,
      useValue: {
        nodeComponentType: RuleNodeComponent
      }
    }
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    RuleChainRoutingModule
  ]
})
export class RuleChainModule { }
