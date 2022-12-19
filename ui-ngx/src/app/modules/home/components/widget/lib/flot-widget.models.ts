///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

// tslint:disable-next-line:no-reference
/// <reference path="../../../../../../../src/typings/jquery.flot.typings.d.ts" />

import { DataKey, Datasource, DatasourceData, FormattedData, JsonSettingsSchema } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { ComparisonDuration } from '@shared/models/time/time.models';

export declare type ChartType = 'line' | 'pie' | 'bar' | 'state' | 'graph';

export declare type TbFlotSettings = TbFlotBaseSettings & TbFlotGraphSettings & TbFlotBarSettings & TbFlotPieSettings;

export declare type TooltipValueFormatFunction = (value: any, latestData: FormattedData) => string;

export declare type TbFlotTicksFormatterFunction = (t: number, a?: TbFlotPlotAxis) => string;

export interface TbFlotSeries extends DatasourceData, JQueryPlotSeriesOptions {
  dataKey: TbFlotDataKey;
  xaxisIndex?: number;
  yaxisIndex?: number;
  yaxis?: number;
}

export interface TbFlotDataKey extends DataKey {
  settings?: TbFlotKeySettings;
  tooltipValueFormatFunction?: TooltipValueFormatFunction;
}

export interface TbFlotPlotAxis extends JQueryPlotAxis, TbFlotAxisOptions {
  options: TbFlotAxisOptions;
}

export interface TbFlotAxisOptions extends JQueryPlotAxisOptions {
  tickUnits?: string;
  hidden?: boolean;
  keysInfo?: Array<{hidden: boolean}>;
  ticksFormatterFunction?: TbFlotTicksFormatterFunction;
}

export interface TbFlotPlotDataSeries extends JQueryPlotDataSeries {
  datasource?: Datasource;
  dataKey?: TbFlotDataKey;
  percent?: number;
}

export interface TbFlotPlotItem extends jquery.flot.item {
  series: TbFlotPlotDataSeries;
}

export interface TbFlotHoverInfo {
  seriesHover: Array<TbFlotSeriesHoverInfo>;
  time?: any;
}

export interface TbFlotSeriesHoverInfo {
  hoverIndex: number;
  units: string;
  decimals: number;
  label: string;
  color: string;
  index: number;
  tooltipValueFormatFunction: TooltipValueFormatFunction;
  value: any;
  time: any;
  distance: number;
}

export interface TbFlotThresholdMarking {
  lineWidth?: number;
  color?: string;
  [key: string]: any;
}

export interface TbFlotThresholdKeySettings {
  yaxis: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotGridSettings {
  color: string;
  backgroundColor: string;
  tickColor: string;
  outlineWidth: number;
  verticalLines: boolean;
  horizontalLines: boolean;
  minBorderMargin?: number;
  margin?: number;
}

export interface TbFlotXAxisSettings {
  showLabels: boolean;
  title: string;
  color: boolean;
}

export interface TbFlotSecondXAxisSettings {
  axisPosition: TbFlotXAxisPosition;
  showLabels: boolean;
  title: string;
}

export interface TbFlotYAxisSettings {
  min: number;
  max: number;
  showLabels: boolean;
  title: string;
  color: string;
  ticksFormatter: string;
  tickDecimals: number;
  tickSize: number;
}

export interface TbFlotBaseSettings {
  stack: boolean;
  enableSelection: FlotSelection;
  shadowSize: number;
  fontColor: string;
  fontSize: number;
  tooltipIndividual: boolean;
  tooltipCumulative: boolean;
  tooltipValueFormatter: string;
  hideZeros: boolean;
  grid: TbFlotGridSettings;
  xaxis: TbFlotXAxisSettings;
  yaxis: TbFlotYAxisSettings;
}

export interface TbFlotComparisonSettings {
  comparisonEnabled: boolean;
  timeForComparison: ComparisonDuration;
  xaxisSecond: TbFlotSecondXAxisSettings;
  comparisonCustomIntervalValue?: number;
}

export interface TbFlotThresholdsSettings {
  thresholdsLineWidth: number;
}

export interface TbFlotCustomLegendSettings {
  customLegendEnabled: boolean;
  dataKeysListForLabels: Array<TbFlotLabelPatternSettings>;
}

export interface TbFlotLabelPatternSettings {
  name: string;
  type: DataKeyType;
  settings?: any;
}

export interface TbFlotGraphSettings extends TbFlotBaseSettings,
                                             TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  smoothLines: boolean;
}

export declare type BarAlignment = 'left' | 'right' | 'center';

export declare type FlotSelection = 'enable' | 'disable' | 'mobile' | 'desktop';

export interface TbFlotBarSettings extends TbFlotBaseSettings,
                                           TbFlotThresholdsSettings, TbFlotComparisonSettings, TbFlotCustomLegendSettings {
  defaultBarWidth: number;
  barAlignment: BarAlignment;
}

export interface TbFlotPieSettings {
  radius: number;
  innerRadius: number;
  tilt: number;
  animatedPie: boolean;
  stroke: {
    color: string;
    width: number;
  };
  showTooltip: boolean;
  showLabels: boolean;
  fontColor: string;
  fontSize: number;
}

export declare type TbFlotYAxisPosition = 'left' | 'right';
export declare type TbFlotXAxisPosition = 'top' | 'bottom';

export declare type TbFlotThresholdValueSource = 'predefinedValue' | 'entityAttribute';

export interface TbFlotKeyThreshold {
  thresholdValueSource: TbFlotThresholdValueSource;
  thresholdEntityAlias: string;
  thresholdAttribute: string;
  thresholdValue: number;
  lineWidth: number;
  color: string;
}

export interface TbFlotKeyComparisonSettings {
  showValuesForComparison: boolean;
  comparisonValuesLabel: string;
  color: string;
}

export interface TbFlotKeySettings {
  excludeFromStacking: boolean;
  hideDataByDefault: boolean;
  disableDataHiding: boolean;
  removeFromLegend: boolean;
  showLines: boolean;
  fillLines: boolean;
  showPoints: boolean;
  showPointShape: string;
  pointShapeFormatter: string;
  showPointsLineWidth: number;
  showPointsRadius: number;
  lineWidth: number;
  tooltipValueFormatter: string;
  showSeparateAxis: boolean;
  axisMin: number;
  axisMax: number;
  axisTitle: string;
  axisTickDecimals: number;
  axisTickSize: number;
  axisPosition: TbFlotYAxisPosition;
  axisTicksFormatter: string;
  thresholds: TbFlotKeyThreshold[];
  comparisonSettings: TbFlotKeyComparisonSettings;
}

export interface TbFlotLatestKeySettings {
  useAsThreshold: boolean;
  thresholdLineWidth: number;
  thresholdColor: string;
}
