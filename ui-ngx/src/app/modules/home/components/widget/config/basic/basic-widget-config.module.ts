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
import { SharedModule } from '@shared/shared.module';
import { IBasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import {
  SimpleCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/simple-card-basic-config.component';
import {
  WidgetActionsPanelComponent
} from '@home/components/widget/config/basic/common/widget-actions-panel.component';
import {
  EntitiesTableBasicConfigComponent
} from '@home/components/widget/config/basic/entity/entities-table-basic-config.component';
import { DataKeysPanelComponent } from '@home/components/widget/config/basic/common/data-keys-panel.component';
import { DataKeyRowComponent } from '@home/components/widget/config/basic/common/data-key-row.component';
import {
  TimeseriesTableBasicConfigComponent
} from '@home/components/widget/config/basic/cards/timeseries-table-basic-config.component';
import { FlotBasicConfigComponent } from '@home/components/widget/config/basic/chart/flot-basic-config.component';
import {
  AlarmsTableBasicConfigComponent
} from '@home/components/widget/config/basic/alarm/alarms-table-basic-config.component';
import {
  ValueCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/value-card-basic-config.component';
import {
  AggregatedValueCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/aggregated-value-card-basic-config.component';
import {
  AggregatedDataKeyRowComponent
} from '@home/components/widget/config/basic/cards/aggregated-data-key-row.component';
import {
  AggregatedDataKeysPanelComponent
} from '@home/components/widget/config/basic/cards/aggregated-data-keys-panel.component';
import {
  AlarmCountBasicConfigComponent
} from '@home/components/widget/config/basic/alarm/alarm-count-basic-config.component';
import {
  EntityCountBasicConfigComponent
} from '@home/components/widget/config/basic/entity/entity-count-basic-config.component';
import {
  BatteryLevelBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/battery-level-basic-config.component';
import {
  WindSpeedDirectionBasicConfigComponent
} from '@home/components/widget/config/basic/weather/wind-speed-direction-basic-config.component';
import {
  SignalStrengthBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/signal-strength-basic-config.component';
import {
  ValueChartCardBasicConfigComponent
} from '@home/components/widget/config/basic/cards/value-chart-card-basic-config.component';
import {
  ProgressBarBasicConfigComponent
} from '@home/components/widget/config/basic/cards/progress-bar-basic-config.component';
import {
  RadialGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/radial-gauge-basic-config.component';
import {
  ThermometerScaleGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/thermometer-scale-gauge-basic-config.component';
import {
  CompassGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/compass-gauge-basic-config.component';
import {
  LiquidLevelCardBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/liquid-level-card-basic-config.component';
import {
  DoughnutBasicConfigComponent
} from '@home/components/widget/config/basic/chart/doughnut-basic-config.component';
import {
  RangeChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/range-chart-basic-config.component';
import {
  BarChartWithLabelsBasicConfigComponent
} from '@home/components/widget/config/basic/chart/bar-chart-with-labels-basic-config.component';
import {
  SingleSwitchBasicConfigComponent
} from '@home/components/widget/config/basic/rpc/single-switch-basic-config.component';
import {
  ActionButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/action-button-basic-config.component';
import {
  CommandButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/command-button-basic-config.component';
import {
  PowerButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/power-button-basic-config.component';
import { SliderBasicConfigComponent } from '@home/components/widget/config/basic/rpc/slider-basic-config.component';
import {
  ToggleButtonBasicConfigComponent
} from '@home/components/widget/config/basic/button/toggle-button-basic-config.component';
import {
  TimeSeriesChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/time-series-chart-basic-config.component';
import { ComparisonKeyRowComponent } from '@home/components/widget/config/basic/chart/comparison-key-row.component';
import {
  ComparisonKeysTableComponent
} from '@home/components/widget/config/basic/chart/comparison-keys-table.component';
import {
  StatusWidgetBasicConfigComponent
} from '@home/components/widget/config/basic/indicator/status-widget-basic-config.component';
import {
  PieChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/pie-chart-basic-config.component';
import {
  BarChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/bar-chart-basic-config.component';
import {
  PolarAreaChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/polar-area-chart-basic-config.component';
import {
  RadarChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/radar-chart-basic-config.component';
import {
  DigitalSimpleGaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/digital-simple-gauge-basic-config.component';
import { MobileAppQrCodeBasicConfigComponent } from '@home/components/widget/config/basic/cards/mobile-app-qr-code-basic-config.component';

@NgModule({
  declarations: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent,
    EntitiesTableBasicConfigComponent,
    TimeseriesTableBasicConfigComponent,
    FlotBasicConfigComponent,
    AlarmsTableBasicConfigComponent,
    ValueCardBasicConfigComponent,
    AggregatedValueCardBasicConfigComponent,
    AggregatedDataKeyRowComponent,
    AggregatedDataKeysPanelComponent,
    DataKeyRowComponent,
    DataKeysPanelComponent,
    AlarmCountBasicConfigComponent,
    EntityCountBasicConfigComponent,
    BatteryLevelBasicConfigComponent,
    WindSpeedDirectionBasicConfigComponent,
    SignalStrengthBasicConfigComponent,
    ValueChartCardBasicConfigComponent,
    ProgressBarBasicConfigComponent,
    RadialGaugeBasicConfigComponent,
    ThermometerScaleGaugeBasicConfigComponent,
    CompassGaugeBasicConfigComponent,
    LiquidLevelCardBasicConfigComponent,
    DoughnutBasicConfigComponent,
    RangeChartBasicConfigComponent,
    BarChartWithLabelsBasicConfigComponent,
    SingleSwitchBasicConfigComponent,
    ActionButtonBasicConfigComponent,
    CommandButtonBasicConfigComponent,
    PowerButtonBasicConfigComponent,
    SliderBasicConfigComponent,
    ToggleButtonBasicConfigComponent,
    TimeSeriesChartBasicConfigComponent,
    ComparisonKeyRowComponent,
    ComparisonKeysTableComponent,
    StatusWidgetBasicConfigComponent,
    PieChartBasicConfigComponent,
    BarChartBasicConfigComponent,
    PolarAreaChartBasicConfigComponent,
    RadarChartBasicConfigComponent,
    DigitalSimpleGaugeBasicConfigComponent,
    MobileAppQrCodeBasicConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    WidgetConfigComponentsModule
  ],
  exports: [
    WidgetActionsPanelComponent,
    SimpleCardBasicConfigComponent,
    EntitiesTableBasicConfigComponent,
    TimeseriesTableBasicConfigComponent,
    FlotBasicConfigComponent,
    AlarmsTableBasicConfigComponent,
    ValueCardBasicConfigComponent,
    AggregatedValueCardBasicConfigComponent,
    AggregatedDataKeyRowComponent,
    AggregatedDataKeysPanelComponent,
    DataKeyRowComponent,
    DataKeysPanelComponent,
    AlarmCountBasicConfigComponent,
    EntityCountBasicConfigComponent,
    BatteryLevelBasicConfigComponent,
    WindSpeedDirectionBasicConfigComponent,
    SignalStrengthBasicConfigComponent,
    ValueChartCardBasicConfigComponent,
    ProgressBarBasicConfigComponent,
    RadialGaugeBasicConfigComponent,
    ThermometerScaleGaugeBasicConfigComponent,
    CompassGaugeBasicConfigComponent,
    LiquidLevelCardBasicConfigComponent,
    DoughnutBasicConfigComponent,
    RangeChartBasicConfigComponent,
    BarChartWithLabelsBasicConfigComponent,
    SingleSwitchBasicConfigComponent,
    ActionButtonBasicConfigComponent,
    CommandButtonBasicConfigComponent,
    PowerButtonBasicConfigComponent,
    SliderBasicConfigComponent,
    ToggleButtonBasicConfigComponent,
    TimeSeriesChartBasicConfigComponent,
    StatusWidgetBasicConfigComponent,
    PieChartBasicConfigComponent,
    BarChartBasicConfigComponent,
    PolarAreaChartBasicConfigComponent,
    RadarChartBasicConfigComponent,
    DigitalSimpleGaugeBasicConfigComponent,
    MobileAppQrCodeBasicConfigComponent
  ]
})
export class BasicWidgetConfigModule {
}

export const basicWidgetConfigComponentsMap: {[key: string]: Type<IBasicWidgetConfigComponent>} = {
  'tb-simple-card-basic-config': SimpleCardBasicConfigComponent,
  'tb-entities-table-basic-config': EntitiesTableBasicConfigComponent,
  'tb-timeseries-table-basic-config': TimeseriesTableBasicConfigComponent,
  'tb-flot-basic-config': FlotBasicConfigComponent,
  'tb-alarms-table-basic-config': AlarmsTableBasicConfigComponent,
  'tb-value-card-basic-config': ValueCardBasicConfigComponent,
  'tb-aggregated-value-card-basic-config': AggregatedValueCardBasicConfigComponent,
  'tb-alarm-count-basic-config': AlarmCountBasicConfigComponent,
  'tb-entity-count-basic-config': EntityCountBasicConfigComponent,
  'tb-battery-level-basic-config': BatteryLevelBasicConfigComponent,
  'tb-wind-speed-direction-basic-config': WindSpeedDirectionBasicConfigComponent,
  'tb-signal-strength-basic-config': SignalStrengthBasicConfigComponent,
  'tb-value-chart-card-basic-config': ValueChartCardBasicConfigComponent,
  'tb-progress-bar-basic-config': ProgressBarBasicConfigComponent,
  'tb-radial-gauge-basic-config': RadialGaugeBasicConfigComponent,
  'tb-thermometer-scale-gauge-basic-config': ThermometerScaleGaugeBasicConfigComponent,
  'tb-compass-gauge-basic-config': CompassGaugeBasicConfigComponent,
  'tb-liquid-level-card-basic-config': LiquidLevelCardBasicConfigComponent,
  'tb-doughnut-basic-config': DoughnutBasicConfigComponent,
  'tb-range-chart-basic-config': RangeChartBasicConfigComponent,
  'tb-bar-chart-with-labels-basic-config': BarChartWithLabelsBasicConfigComponent,
  'tb-single-switch-basic-config': SingleSwitchBasicConfigComponent,
  'tb-action-button-basic-config': ActionButtonBasicConfigComponent,
  'tb-command-button-basic-config': CommandButtonBasicConfigComponent,
  'tb-power-button-basic-config': PowerButtonBasicConfigComponent,
  'tb-slider-basic-config': SliderBasicConfigComponent,
  'tb-toggle-button-basic-config': ToggleButtonBasicConfigComponent,
  'tb-time-series-chart-basic-config': TimeSeriesChartBasicConfigComponent,
  'tb-status-widget-basic-config': StatusWidgetBasicConfigComponent,
  'tb-pie-chart-basic-config': PieChartBasicConfigComponent,
  'tb-bar-chart-basic-config': BarChartBasicConfigComponent,
  'tb-polar-area-chart-basic-config': PolarAreaChartBasicConfigComponent,
  'tb-radar-chart-basic-config': RadarChartBasicConfigComponent,
  'tb-digital-simple-gauge-basic-config': DigitalSimpleGaugeBasicConfigComponent,
  'tb-mobile-app-qr-code-basic-config': MobileAppQrCodeBasicConfigComponent
};
