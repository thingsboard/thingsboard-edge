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
import { AlarmFilterConfigComponent } from '@home/components/alarm/alarm-filter-config.component';
import { AlarmAssigneeSelectComponent } from '@home/components/alarm/alarm-assignee-select.component';
import { DataKeysComponent } from '@home/components/widget/config/data-keys.component';
import { DataKeyConfigDialogComponent } from '@home/components/widget/config/data-key-config-dialog.component';
import { DataKeyConfigComponent } from '@home/components/widget/config/data-key-config.component';
import { DatasourceComponent } from '@home/components/widget/config/datasource.component';
import { DatasourcesComponent } from '@home/components/widget/config/datasources.component';
import { EntityAliasSelectComponent } from '@home/components/alias/entity-alias-select.component';
import { FilterSelectComponent } from '@home/components/filter/filter-select.component';
import { WidgetSettingsModule } from '@home/components/widget/lib/settings/widget-settings.module';
import { WidgetSettingsComponent } from '@home/components/widget/config/widget-settings.component';
import { TimewindowConfigPanelComponent } from '@home/components/widget/config/timewindow-config-panel.component';
import { WidgetUnitsComponent } from '@home/components/widget/config/widget-units.component';

@NgModule({
  declarations:
    [
      AlarmAssigneeSelectComponent,
      AlarmFilterConfigComponent,
      DataKeysComponent,
      DataKeyConfigDialogComponent,
      DataKeyConfigComponent,
      DatasourceComponent,
      DatasourcesComponent,
      EntityAliasSelectComponent,
      FilterSelectComponent,
      TimewindowConfigPanelComponent,
      WidgetUnitsComponent,
      WidgetSettingsComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetSettingsModule
  ],
  exports: [
    AlarmAssigneeSelectComponent,
    AlarmFilterConfigComponent,
    DataKeysComponent,
    DataKeyConfigDialogComponent,
    DataKeyConfigComponent,
    DatasourceComponent,
    DatasourcesComponent,
    EntityAliasSelectComponent,
    FilterSelectComponent,
    TimewindowConfigPanelComponent,
    WidgetUnitsComponent,
    WidgetSettingsComponent
  ]
})
export class WidgetConfigComponentsModule { }
