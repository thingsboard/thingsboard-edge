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
import { SharedModule } from '@app/shared/shared.module';
import { EntitiesTableWidgetComponent } from '@home/components/widget/lib/entity/entities-table-widget.component';
import { DisplayColumnsPanelComponent } from '@home/components/widget/lib/display-columns-panel.component';
import { AlarmsTableWidgetComponent } from '@home/components/widget/lib/alarm/alarms-table-widget.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { TimeseriesTableWidgetComponent } from '@home/components/widget/lib/timeseries-table-widget.component';
import {
  EntitiesHierarchyWidgetComponent
} from '@home/components/widget/lib/entity/entities-hierarchy-widget.component';
import { RpcWidgetsModule } from '@home/components/widget/lib/rpc/rpc-widgets.module';
import {
  DateRangeNavigatorPanelComponent,
  DateRangeNavigatorWidgetComponent
} from '@home/components/widget/lib/date-range-navigator/date-range-navigator.component';
import { MultipleInputWidgetComponent } from '@home/components/widget/lib/multiple-input-widget.component';
import { TripAnimationComponent } from '@home/components/widget/lib/trip-animation/trip-animation.component';
import { PhotoCameraInputWidgetComponent } from '@home/components/widget/lib/photo-camera-input.component';
import { NavigationCardsWidgetComponent } from '@home/components/widget/lib/navigation-cards-widget.component';
import { NavigationCardWidgetComponent } from '@home/components/widget/lib/navigation-card-widget.component';
import { EdgesOverviewWidgetComponent } from '@home/components/widget/lib/edges-overview-widget.component';
import { JsonInputWidgetComponent } from '@home/components/widget/lib/json-input-widget.component';
import { QrCodeWidgetComponent } from '@home/components/widget/lib/qrcode-widget.component';
import { MarkdownWidgetComponent } from '@home/components/widget/lib/markdown-widget.component';
import { SelectEntityDialogComponent } from '@home/components/widget/lib/maps/dialogs/select-entity-dialog.component';
import { HomePageWidgetsModule } from '@home/components/widget/lib/home-page/home-page-widgets.module';
import { WIDGET_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { FlotWidgetComponent } from '@home/components/widget/lib/flot-widget.component';
import { LegendComponent } from '@home/components/widget/lib/legend.component';
import { ValueCardWidgetComponent } from '@home/components/widget/lib/cards/value-card-widget.component';
import {
  AggregatedValueCardWidgetComponent
} from '@home/components/widget/lib/cards/aggregated-value-card-widget.component';
import { CountWidgetComponent } from '@home/components/widget/lib/count/count-widget.component';
import { BatteryLevelWidgetComponent } from '@home/components/widget/lib/indicator/battery-level-widget.component';
import {
  WindSpeedDirectionWidgetComponent
} from '@home/components/widget/lib/weather/wind-speed-direction-widget.component';
import { SignalStrengthWidgetComponent } from '@home/components/widget/lib/indicator/signal-strength-widget.component';
import { ValueChartCardWidgetComponent } from '@home/components/widget/lib/cards/value-chart-card-widget.component';
import { ProgressBarWidgetComponent } from '@home/components/widget/lib/cards/progress-bar-widget.component';
import { LiquidLevelWidgetComponent } from '@home/components/widget/lib/indicator/liquid-level-widget.component';
import { DoughnutWidgetComponent } from '@home/components/widget/lib/chart/doughnut-widget.component';
import { RangeChartWidgetComponent } from '@home/components/widget/lib/chart/range-chart-widget.component';
import {
  BarChartWithLabelsWidgetComponent
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.component';
import { SingleSwitchWidgetComponent } from '@home/components/widget/lib/rpc/single-switch-widget.component';
import { ActionButtonWidgetComponent } from '@home/components/widget/lib/button/action-button-widget.component';
import { CommandButtonWidgetComponent } from '@home/components/widget/lib/button/command-button-widget.component';
import { PowerButtonWidgetComponent } from '@home/components/widget/lib/rpc/power-button-widget.component';
import { SliderWidgetComponent } from '@home/components/widget/lib/rpc/slider-widget.component';
import { ToggleButtonWidgetComponent } from '@home/components/widget/lib/button/toggle-button-widget.component';
import { TimeSeriesChartWidgetComponent } from '@home/components/widget/lib/chart/time-series-chart-widget.component';
import { StatusWidgetComponent } from '@home/components/widget/lib/indicator/status-widget.component';
import { LatestChartComponent } from '@home/components/widget/lib/chart/latest-chart.component';
import { PieChartWidgetComponent } from '@home/components/widget/lib/chart/pie-chart-widget.component';
import { BarChartWidgetComponent } from '@home/components/widget/lib/chart/bar-chart-widget.component';
import { PolarAreaWidgetComponent } from '@home/components/widget/lib/chart/polar-area-widget.component';
import { RadarChartWidgetComponent } from '@home/components/widget/lib/chart/radar-chart-widget.component';
import { MobileAppQrcodeWidgetComponent } from '@home/components/widget/lib/mobile-app-qrcode-widget.component';
import { KeyValueIsNotEmptyPipe } from '@shared/pipe/key-value-not-empty.pipe';
import { LabelCardWidgetComponent } from '@home/components/widget/lib/cards/label-card-widget.component';
import { LabelValueCardWidgetComponent } from '@home/components/widget/lib/cards/label-value-card-widget.component';
import {
  UnreadNotificationWidgetComponent
} from '@home/components/widget/lib/cards/unread-notification-widget.component';
import {
  NotificationTypeFilterPanelComponent
} from '@home/components/widget/lib/cards/notification-type-filter-panel.component';
import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import { ScadaSymbolWidgetComponent } from '@home/components/widget/lib/scada/scada-symbol-widget.component';
import { TwoSegmentButtonWidgetComponent } from '@home/components/widget/lib/button/two-segment-button-widget.component';
import { ValueStepperWidgetComponent } from '@home/components/widget/lib/rpc/value-stepper-widget.component';

@NgModule({
  declarations: [
    DisplayColumnsPanelComponent,
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
    NavigationCardsWidgetComponent,
    NavigationCardWidgetComponent,
    QrCodeWidgetComponent,
    MobileAppQrcodeWidgetComponent,
    MarkdownWidgetComponent,
    SelectEntityDialogComponent,
    LegendComponent,
    FlotWidgetComponent,
    ValueCardWidgetComponent,
    AggregatedValueCardWidgetComponent,
    CountWidgetComponent,
    BatteryLevelWidgetComponent,
    WindSpeedDirectionWidgetComponent,
    SignalStrengthWidgetComponent,
    ValueChartCardWidgetComponent,
    ProgressBarWidgetComponent,
    LiquidLevelWidgetComponent,
    DoughnutWidgetComponent,
    RangeChartWidgetComponent,
    BarChartWithLabelsWidgetComponent,
    SingleSwitchWidgetComponent,
    ActionButtonWidgetComponent,
    TwoSegmentButtonWidgetComponent,
    CommandButtonWidgetComponent,
    PowerButtonWidgetComponent,
    ValueStepperWidgetComponent,
    SliderWidgetComponent,
    ToggleButtonWidgetComponent,
    TimeSeriesChartWidgetComponent,
    StatusWidgetComponent,
    LatestChartComponent,
    PieChartWidgetComponent,
    BarChartWidgetComponent,
    PolarAreaWidgetComponent,
    RadarChartWidgetComponent,
    LabelCardWidgetComponent,
    LabelValueCardWidgetComponent,
    UnreadNotificationWidgetComponent,
    NotificationTypeFilterPanelComponent,
    ScadaSymbolWidgetComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    RpcWidgetsModule,
    HomePageWidgetsModule,
    SharedHomeComponentsModule,
    KeyValueIsNotEmptyPipe,
    EllipsisChipListDirective,
  ],
  exports: [
    EntitiesTableWidgetComponent,
    AlarmsTableWidgetComponent,
    TimeseriesTableWidgetComponent,
    EntitiesHierarchyWidgetComponent,
    EdgesOverviewWidgetComponent,
    RpcWidgetsModule,
    HomePageWidgetsModule,
    DateRangeNavigatorWidgetComponent,
    JsonInputWidgetComponent,
    MultipleInputWidgetComponent,
    TripAnimationComponent,
    PhotoCameraInputWidgetComponent,
    NavigationCardsWidgetComponent,
    NavigationCardWidgetComponent,
    QrCodeWidgetComponent,
    MobileAppQrcodeWidgetComponent,
    MarkdownWidgetComponent,
    LegendComponent,
    FlotWidgetComponent,
    EllipsisChipListDirective,
    ValueCardWidgetComponent,
    AggregatedValueCardWidgetComponent,
    CountWidgetComponent,
    BatteryLevelWidgetComponent,
    WindSpeedDirectionWidgetComponent,
    SignalStrengthWidgetComponent,
    ValueChartCardWidgetComponent,
    ProgressBarWidgetComponent,
    LiquidLevelWidgetComponent,
    DoughnutWidgetComponent,
    RangeChartWidgetComponent,
    BarChartWithLabelsWidgetComponent,
    SingleSwitchWidgetComponent,
    ActionButtonWidgetComponent,
    TwoSegmentButtonWidgetComponent,
    CommandButtonWidgetComponent,
    PowerButtonWidgetComponent,
    ValueStepperWidgetComponent,
    SliderWidgetComponent,
    ToggleButtonWidgetComponent,
    TimeSeriesChartWidgetComponent,
    StatusWidgetComponent,
    PieChartWidgetComponent,
    BarChartWidgetComponent,
    PolarAreaWidgetComponent,
    RadarChartWidgetComponent,
    LabelCardWidgetComponent,
    LabelValueCardWidgetComponent,
    UnreadNotificationWidgetComponent,
    NotificationTypeFilterPanelComponent,
    ScadaSymbolWidgetComponent
  ],
  providers: [
    {provide: WIDGET_COMPONENTS_MODULE_TOKEN, useValue: WidgetComponentsModule},
  ]
})
export class WidgetComponentsModule {
}
