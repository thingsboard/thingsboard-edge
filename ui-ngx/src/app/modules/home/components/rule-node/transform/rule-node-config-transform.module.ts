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
import { ChangeOriginatorConfigComponent } from './change-originator-config.component';
import { RuleNodeConfigCommonModule } from '../common/rule-node-config-common.module';
import { TransformScriptConfigComponent } from './script-config.component';
import { ToEmailConfigComponent } from './to-email-config.component';
import { CopyKeysConfigComponent } from './copy-keys-config.component';
import { RenameKeysConfigComponent } from './rename-keys-config.component';
import { NodeJsonPathConfigComponent } from './node-json-path-config.component';
import { DeleteKeysConfigComponent } from './delete-keys-config.component';
import { DeduplicationConfigComponent } from './deduplication-config.component';
import { ScriptConfigComponent } from '@home/components/rule-node/filter/script-config.component';

@NgModule({
  declarations: [
    ChangeOriginatorConfigComponent,
    TransformScriptConfigComponent,
    ToEmailConfigComponent,
    CopyKeysConfigComponent,
    RenameKeysConfigComponent,
    NodeJsonPathConfigComponent,
    DeleteKeysConfigComponent,
    DeduplicationConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    RuleNodeConfigCommonModule
  ],
  exports: [
    ChangeOriginatorConfigComponent,
    TransformScriptConfigComponent,
    ToEmailConfigComponent,
    CopyKeysConfigComponent,
    RenameKeysConfigComponent,
    NodeJsonPathConfigComponent,
    DeleteKeysConfigComponent,
    DeduplicationConfigComponent
  ]
})
export class RuleNodeConfigTransformModule {
}

export const ruleNodeTransformConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  'tbTransformationNodeChangeOriginatorConfig': ChangeOriginatorConfigComponent,
  'tbTransformationNodeCopyKeysConfig': CopyKeysConfigComponent,
  'tbActionNodeMsgDeduplicationConfig': DeduplicationConfigComponent,
  'tbTransformationNodeDeleteKeysConfig': DeleteKeysConfigComponent,
  'tbTransformationNodeJsonPathConfig': NodeJsonPathConfigComponent,
  'tbTransformationNodeRenameKeysConfig': RenameKeysConfigComponent,
  'tbTransformationNodeScriptConfig': ScriptConfigComponent,
  'tbTransformationNodeToEmailConfig': ToEmailConfigComponent
}
