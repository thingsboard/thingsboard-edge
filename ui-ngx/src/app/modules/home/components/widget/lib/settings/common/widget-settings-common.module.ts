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
  ColorRangeSettingsComponent,
  ColorRangeSettingsComponentService
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
  ChartAnimationSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/chart-animation-settings.component';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';
import {
  AutoDateFormatSettingsComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings.component';
import {
  ChartFillSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/chart-fill-settings.component';
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
import {
  TimeSeriesChartGridSettingsComponent
} from '@home/components/widget/lib/settings/common/chart/time-series-chart-grid-settings.component';
import {
  StatusWidgetStateSettingsComponent
} from '@home/components/widget/lib/settings/common/indicator/status-widget-state-settings.component';
import { ChartBarSettingsComponent } from '@home/components/widget/lib/settings/common/chart/chart-bar-settings.component';
import { AdvancedRangeComponent } from '@home/components/widget/lib/settings/common/advanced-range.component';
import { GradientComponent } from '@home/components/widget/lib/settings/common/gradient.component';
import {
  ValueSourceDataKeyComponent
} from '@home/components/widget/lib/settings/common/value-source-data-key.component';
import {
  ScadaSymbolObjectSettingsComponent
} from '@home/components/widget/lib/settings/common/scada/scada-symbol-object-settings.component';
import {
  WidgetButtonToggleCustomStyleComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style.component';
import {
  WidgetButtonToggleCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style-panel.component';
import {
  DynamicFormPropertiesComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-properties.component';
import {
  DynamicFormPropertyRowComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-property-row.component';
import {
  DynamicFormPropertyPanelComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-property-panel.component';
import { DynamicFormComponent } from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form.component';
import {
  DynamicFormSelectItemsComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-items.component';
import {
  DynamicFormSelectItemRowComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-select-item-row.component';
import {
  DynamicFormArrayComponent
} from '@home/components/widget/lib/settings/common/dynamic-form/dynamic-form-array.component';

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
    ValueSourceDataKeyComponent,
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
    WidgetButtonToggleCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    WidgetButtonToggleCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    ChartAnimationSettingsComponent,
    ChartFillSettingsComponent,
    ChartBarSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    TimeSeriesChartGridSettingsComponent,
    StatusWidgetStateSettingsComponent,
    ScadaSymbolObjectSettingsComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent,
    AdvancedRangeComponent,
    GradientComponent,
    DynamicFormPropertiesComponent,
    DynamicFormPropertyRowComponent,
    DynamicFormPropertyPanelComponent,
    DynamicFormSelectItemsComponent,
    DynamicFormSelectItemRowComponent,
    DynamicFormComponent,
    DynamicFormArrayComponent
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
    ValueSourceDataKeyComponent,
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
    WidgetButtonToggleCustomStyleComponent,
    WidgetButtonCustomStylePanelComponent,
    WidgetButtonToggleCustomStylePanelComponent,
    TimeSeriesChartAxisSettingsComponent,
    TimeSeriesChartThresholdsPanelComponent,
    TimeSeriesChartThresholdRowComponent,
    TimeSeriesChartThresholdSettingsPanelComponent,
    TimeSeriesNoAggregationBarWidthSettingsComponent,
    TimeSeriesChartYAxesPanelComponent,
    TimeSeriesChartYAxisRowComponent,
    TimeSeriesChartAxisSettingsPanelComponent,
    TimeSeriesChartAxisSettingsButtonComponent,
    ChartAnimationSettingsComponent,
    ChartFillSettingsComponent,
    ChartBarSettingsComponent,
    TimeSeriesChartThresholdSettingsComponent,
    TimeSeriesChartStatesPanelComponent,
    TimeSeriesChartStateRowComponent,
    TimeSeriesChartGridSettingsComponent,
    StatusWidgetStateSettingsComponent,
    ScadaSymbolObjectSettingsComponent,
    DataKeyInputComponent,
    EntityAliasInputComponent,
    AdvancedRangeComponent,
    GradientComponent,
    DynamicFormPropertiesComponent,
    DynamicFormPropertyRowComponent,
    DynamicFormPropertyPanelComponent,
    DynamicFormSelectItemsComponent,
    DynamicFormSelectItemRowComponent,
    DynamicFormComponent,
    DynamicFormArrayComponent
  ],
  providers: [
    ColorSettingsComponentService,
    ColorRangeSettingsComponentService
  ]
})
export class WidgetSettingsCommonModule {
}
