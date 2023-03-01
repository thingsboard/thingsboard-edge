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
import { EntitiesTableWidgetComponent } from '@home/components/widget/lib/entities-table-widget.component';
import { DisplayColumnsPanelComponent } from '@home/components/widget/lib/display-columns-panel.component';
import { AlarmsTableWidgetComponent } from '@home/components/widget/lib/alarms-table-widget.component';
import { AlarmFilterPanelComponent } from '@home/components/widget/lib/alarm-filter-panel.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { TimeseriesTableWidgetComponent } from '@home/components/widget/lib/timeseries-table-widget.component';
import { EntitiesHierarchyWidgetComponent } from '@home/components/widget/lib/entities-hierarchy-widget.component';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { RpcWidgetsModule } from '@home/components/widget/lib/rpc/rpc-widgets.module';
import {
  DateRangeNavigatorPanelComponent,
  DateRangeNavigatorWidgetComponent
} from '@home/components/widget/lib/date-range-navigator/date-range-navigator.component';
import { MultipleInputWidgetComponent } from '@home/components/widget/lib/multiple-input-widget.component';
import { TripAnimationComponent } from '@home/components/widget/trip-animation/trip-animation.component';
import { PhotoCameraInputWidgetComponent } from '@home/components/widget/lib/photo-camera-input.component';
import { GatewayFormComponent } from '@home/components/widget/lib/gateway/gateway-form.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { NavigationCardsWidgetComponent } from '@home/components/widget/lib/navigation-cards-widget.component';
import { NavigationCardWidgetComponent } from '@home/components/widget/lib/navigation-card-widget.component';
import { EdgesOverviewWidgetComponent } from '@home/components/widget/lib/edges-overview-widget.component';
import { JsonInputWidgetComponent } from '@home/components/widget/lib/json-input-widget.component';
import { QrCodeWidgetComponent } from '@home/components/widget/lib/qrcode-widget.component';
import { MarkdownWidgetComponent } from '@home/components/widget/lib/markdown-widget.component';
import { SelectEntityDialogComponent } from '@home/components/widget/lib/maps/dialogs/select-entity-dialog.component';

@NgModule({
  declarations:
    [
      DisplayColumnsPanelComponent,
      AlarmFilterPanelComponent,
      EntitiesTableWidgetComponent,
      AlarmsTableWidgetComponent,
      TimeseriesTableWidgetComponent,
      EntitiesHierarchyWidgetComponent,
      EdgesOverviewWidgetComponent,
      DateRangeNavigatorWidgetComponent,
      DateRangeNavigatorPanelComponent,
      JsonInputWidgetComponent,
      MultipleInputWidgetComponent,
      TripAnimationComponent,
      PhotoCameraInputWidgetComponent,
      GatewayFormComponent,
      NavigationCardsWidgetComponent,
      NavigationCardWidgetComponent,
      QrCodeWidgetComponent,
      MarkdownWidgetComponent,
      SelectEntityDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    RpcWidgetsModule,
    SharedHomeComponentsModule
  ],
  exports: [
    EntitiesTableWidgetComponent,
    AlarmsTableWidgetComponent,
    TimeseriesTableWidgetComponent,
    EntitiesHierarchyWidgetComponent,
    EdgesOverviewWidgetComponent,
    RpcWidgetsModule,
    SharedHomeComponentsModule,
    DateRangeNavigatorWidgetComponent,
    JsonInputWidgetComponent,
    MultipleInputWidgetComponent,
    TripAnimationComponent,
    PhotoCameraInputWidgetComponent,
    GatewayFormComponent,
    NavigationCardsWidgetComponent,
    NavigationCardWidgetComponent,
    QrCodeWidgetComponent,
    MarkdownWidgetComponent
  ],
  providers: [
    CustomDialogService,
    ImportExportService
  ]
})
export class WidgetComponentsModule {
}
