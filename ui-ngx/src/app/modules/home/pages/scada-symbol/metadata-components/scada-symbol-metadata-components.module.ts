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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  ScadaSymbolMetadataTagComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag.component';
import {
  ScadaSymbolMetadataComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata.component';
import {
  ScadaSymbolMetadataTagsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tags.component';
import {
  ScadaSymbolBehaviorsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behaviors.component';
import {
  ScadaSymbolBehaviorRowComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-row.component';
import {
  ScadaSymbolBehaviorPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-panel.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import {
  ScadaSymbolMetadataTagFunctionPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag-function-panel.component';

@NgModule({
  declarations:
    [
      ScadaSymbolMetadataComponent,
      ScadaSymbolMetadataTagComponent,
      ScadaSymbolMetadataTagsComponent,
      ScadaSymbolMetadataTagFunctionPanelComponent,
      ScadaSymbolBehaviorsComponent,
      ScadaSymbolBehaviorRowComponent,
      ScadaSymbolBehaviorPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetSettingsCommonModule
  ],
  exports: [
    ScadaSymbolMetadataComponent
  ]
})
export class ScadaSymbolMetadataComponentsModule { }
