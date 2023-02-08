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
import { SharedModule } from '@app/shared/shared.module';
import { LedIndicatorComponent } from '@home/components/widget/lib/rpc/led-indicator.component';
import { RoundSwitchComponent } from '@home/components/widget/lib/rpc/round-switch.component';
import { SwitchComponent } from '@home/components/widget/lib/rpc/switch.component';
import { KnobComponent } from '@home/components/widget/lib/rpc/knob.component';
import { PersistentTableComponent } from '@home/components/widget/lib/rpc/persistent-table.component';
import { PersistentDetailsDialogComponent } from '@home/components/widget/lib/rpc/persistent-details-dialog.component';
import { PersistentFilterPanelComponent } from '@home/components/widget/lib/rpc/persistent-filter-panel.component';
import { PersistentAddDialogComponent } from '@home/components/widget/lib/rpc/persistent-add-dialog.component';

@NgModule({
  declarations:
    [
      LedIndicatorComponent,
      RoundSwitchComponent,
      SwitchComponent,
      KnobComponent,
      PersistentTableComponent,
      PersistentDetailsDialogComponent,
      PersistentAddDialogComponent,
      PersistentFilterPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    LedIndicatorComponent,
    RoundSwitchComponent,
    SwitchComponent,
    KnobComponent,
    PersistentTableComponent,
    PersistentDetailsDialogComponent,
    PersistentAddDialogComponent
  ]
})
export class RpcWidgetsModule { }
