///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { WidgetFontComponent } from '@home/components/widget/lib/settings/common/widget-font.component';
import { ValueSourceComponent } from '@home/components/widget/lib/settings/common/value-source.component';
import { LegendConfigComponent } from '@home/components/widget/lib/settings/common/legend-config.component';
import {
  ImageCardsSelectComponent,
  ImageCardsSelectOptionDirective
} from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { FontSettingsComponent } from '@home/components/widget/lib/settings/common/font-settings.component';
import { FontSettingsPanelComponent } from '@home/components/widget/lib/settings/common/font-settings-panel.component';
import {
  ColorSettingsComponent,
  ColorSettingsComponentService
} from '@home/components/widget/lib/settings/common/color-settings.component';
import {
  ColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/color-settings-panel.component';
import { CssUnitSelectComponent } from '@home/components/widget/lib/settings/common/css-unit-select.component';
import { DateFormatSelectComponent } from '@home/components/widget/lib/settings/common/date-format-select.component';
import {
  DateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/date-format-settings-panel.component';
import { BackgroundSettingsComponent } from '@home/components/widget/lib/settings/common/background-settings.component';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';
import {
  CountWidgetSettingsComponent
} from '@home/components/widget/lib/settings/common/count-widget-settings.component';
import { ColorRangeListComponent } from '@home/components/widget/lib/settings/common/color-range-list.component';
import { ColorRangePanelComponent } from '@home/components/widget/lib/settings/common/color-range-panel.component';
import {
  ColorRangeSettingsComponent, ColorRangeSettingsComponentService
} from '@home/components/widget/lib/settings/common/color-range-settings.component';
import {
  GetValueActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings.component';
import {
  GetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings-panel.component';
import {
  DeviceKeyAutocompleteComponent
} from '@home/components/widget/lib/settings/control/device-key-autocomplete.component';
import {
  SetValueActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings.component';
import {
  SetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings-panel.component';
import { CssSizeInputComponent } from '@home/components/widget/lib/settings/common/css-size-input.component';
import { WidgetActionComponent } from '@home/components/widget/lib/settings/common/action/widget-action.component';
import {
  CustomActionPrettyResourcesTabsComponent
} from '@home/components/widget/lib/settings/common/action/custom-action-pretty-resources-tabs.component';
import {
  CustomActionPrettyEditorComponent
} from '@home/components/widget/lib/settings/common/action/custom-action-pretty-editor.component';
import {
  MobileActionEditorComponent
} from '@home/components/widget/lib/settings/common/action/mobile-action-editor.component';
import {
  WidgetActionSettingsComponent
} from '@home/components/widget/lib/settings/common/action/widget-action-settings.component';
import {
  WidgetActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/widget-action-settings-panel.component';
import {
  WidgetButtonAppearanceComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-appearance.component';
import {
  WidgetButtonCustomStyleComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-custom-style.component';
import {
  WidgetButtonCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-custom-style-panel.component';
import {
  TimeSeriesChartAxisSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings.component';
import {
  TimeSeriesChartThresholdsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-thresholds-panel.component';
import {
  TimeSeriesChartThresholdRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-row.component';
import { DataKeyInputComponent } from '@home/components/widget/lib/settings/common/data-key-input.component';
import { EntityAliasInputComponent } from '@home/components/widget/lib/settings/common/entity-alias-input.component';
import {
  TimeSeriesChartThresholdSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings-panel.component';
import {
  TimeSeriesNoAggregationBarWidthSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-no-aggregation-bar-width-settings.component';
import {
  TimeSeriesChartYAxesPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-y-axes-panel.component';
import {
  TimeSeriesChartYAxisRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-y-axis-row.component';
import {
  TimeSeriesChartAxisSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-panel.component';
import {
  TimeSeriesChartAnimationSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-animation-settings.component';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';
import {
  AutoDateFormatSettingsComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings.component';
import {
  TimeSeriesChartFillSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-fill-settings.component';
import {
  TimeSeriesChartThresholdSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-threshold-settings.component';
import {
  TimeSeriesChartStateRowComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-state-row.component';
import {
  TimeSeriesChartStatesPanelComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-states-panel.component';
import {
  TimeSeriesChartAxisSettingsButtonComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-axis-settings-button.component';

@NgModule({
  declarations: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    CssSizeInputComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    AutoDateFormatSettingsComponent,
    AutoDateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    GetValueActionSettingsComponent,
    GetValueActionSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    SetValueActionSettingsComponent,
    SetValueActionSettingsPanelComponent,
    WidgetActionComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    MobileActionEditorComponent,
    WidgetActionSettingsComponent,
    WidgetActionSettingsPanelComponent,
    WidgetButtonAppearanceComponent,
    WidgetButtonCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    TimeSeriesChartAnimationSettingsComponent,
    TimeSeriesChartFillSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule
  ],
  exports: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    CssSizeInputComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    AutoDateFormatSettingsComponent,
    AutoDateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent,
    ColorRangeListComponent,
    ColorRangePanelComponent,
    ColorRangeSettingsComponent,
    GetValueActionSettingsComponent,
    GetValueActionSettingsPanelComponent,
    DeviceKeyAutocompleteComponent,
    SetValueActionSettingsComponent,
    SetValueActionSettingsPanelComponent,
    WidgetActionComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    MobileActionEditorComponent,
    WidgetActionSettingsComponent,
    WidgetActionSettingsPanelComponent,
    WidgetButtonAppearanceComponent,
    WidgetButtonCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    TimeSeriesChartAnimationSettingsComponent,
    TimeSeriesChartFillSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent
  ],
  providers: [
    ColorSettingsComponentService,
    ColorRangeSettingsComponentService
  ]
})
export class WidgetSettingsCommonModule {
}
