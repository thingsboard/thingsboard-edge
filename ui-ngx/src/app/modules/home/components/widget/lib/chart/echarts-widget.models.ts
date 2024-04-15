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

import * as echarts from 'echarts/core';
import AxisModel from 'echarts/types/src/coord/cartesian/AxisModel';
import { estimateLabelUnionRect } from 'echarts/lib/coord/axisHelper';
import { isDefinedAndNotNull, isFunction, isNumber } from '@core/utils';
import {
  DataZoomComponent,
  DataZoomComponentOption,
  GridComponent,
  GridComponentOption,
  MarkLineComponent,
  MarkLineComponentOption,
  TooltipComponent,
  TooltipComponentOption,
  VisualMapComponent,
  VisualMapComponentOption
} from 'echarts/components';
import {
  BarChart,
  BarSeriesOption,
  CustomChart,
  CustomSeriesOption,
  LineChart,
  LineSeriesOption,
  PieChart,
  PieSeriesOption
} from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer, SVGRenderer } from 'echarts/renderers';
import { DataEntry, DataKey, DataSet, Datasource, FormattedData } from '@shared/models/widget.models';
import {
  calculateAggIntervalWithWidgetTimeWindow,
  Interval,
  IntervalMath,
  WidgetTimewindow
} from '@shared/models/time/time.models';
import { CallbackDataParams, TimeAxisBandWidthCalculator } from 'echarts/types/dist/shared';
import { Renderer2 } from '@angular/core';
import { DateFormatProcessor, DateFormatSettings, Font } from '@shared/models/widget-settings.models';
import GlobalModel from 'echarts/types/src/model/Global';
import Axis2D from 'echarts/types/src/coord/cartesian/Axis2D';
import SeriesModel from 'echarts/types/src/model/Series';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';
import { TimeAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';

class EChartsModule {
  private initialized = false;

  init() {
    if (!this.initialized) {
      echarts.use([
        TooltipComponent,
        GridComponent,
        VisualMapComponent,
        DataZoomComponent,
        MarkLineComponent,
        LineChart,
        BarChart,
        PieChart,
        CustomChart,
        LabelLayout,
        CanvasRenderer,
        SVGRenderer
      ]);
      this.initialized = true;
    }
  }
}

export const echartsModule = new EChartsModule();

export type EChartsOption = echarts.ComposeOption<
  | TooltipComponentOption
  | GridComponentOption
  | VisualMapComponentOption
  | DataZoomComponentOption
  | MarkLineComponentOption
  | LineSeriesOption
  | CustomSeriesOption
  | BarSeriesOption
  | PieSeriesOption
>;

export type ECharts = echarts.ECharts;

export type EChartsDataItem = [number, any, number, number];

export type NamedDataSet = {name: string; value: EChartsDataItem}[];

export type EChartsTooltipValueFormatFunction = (value: any, latestData: FormattedData, units?: string, decimals?: number) => string;

export type EChartsSeriesItem = {
  id: string;
  datasource: Datasource;
  dataKey: DataKey;
  data: NamedDataSet;
  dataSet?: DataSet;
  enabled: boolean;
  units?: string;
  decimals?: number;
  latestData?: FormattedData;
  tooltipValueFormatFunction?: EChartsTooltipValueFormatFunction;
  comparisonItem?: boolean;
};

export enum EChartsShape {
  emptyCircle = 'emptyCircle',
  circle = 'circle',
  rect = 'rect',
  roundRect = 'roundRect',
  triangle = 'triangle',
  diamond = 'diamond',
  pin = 'pin',
  arrow = 'arrow',
  none = 'none'
}

export const echartsShapes = Object.keys(EChartsShape) as EChartsShape[];

export const echartsShapeTranslations = new Map<EChartsShape, string>(
  [
    [EChartsShape.emptyCircle, 'widgets.time-series-chart.shape-empty-circle'],
    [EChartsShape.circle, 'widgets.time-series-chart.shape-circle'],
    [EChartsShape.rect, 'widgets.time-series-chart.shape-rect'],
    [EChartsShape.roundRect, 'widgets.time-series-chart.shape-round-rect'],
    [EChartsShape.triangle, 'widgets.time-series-chart.shape-triangle'],
    [EChartsShape.diamond, 'widgets.time-series-chart.shape-diamond'],
    [EChartsShape.pin, 'widgets.time-series-chart.shape-pin'],
    [EChartsShape.arrow, 'widgets.time-series-chart.shape-arrow'],
    [EChartsShape.none, 'widgets.time-series-chart.shape-none']
  ]
);

type EChartsShapeOffsetFunction = (size: number) => number;

export const timeSeriesChartShapeOffsetFunctions = new Map<EChartsShape, EChartsShapeOffsetFunction>(
  [
    [EChartsShape.emptyCircle, size => size / 2 + 1],
    [EChartsShape.circle, size => size / 2],
    [EChartsShape.rect, size => size / 2],
    [EChartsShape.roundRect, size => size / 2],
    [EChartsShape.triangle, size => size / 2],
    [EChartsShape.diamond, size => size / 2],
    [EChartsShape.pin, size => size],
    [EChartsShape.arrow, () => 0],
    [EChartsShape.none, () => 0],
  ]
);


export const timeAxisBandWidthCalculator: TimeAxisBandWidthCalculator = (model) => {
  let interval: number;
  const axisOption = model.option;
  const seriesDataIndices = axisOption.axisPointer?.seriesDataIndices;
  if (seriesDataIndices?.length) {
    const seriesDataIndex = seriesDataIndices[0];
    const series = model.ecModel.getSeriesByIndex(seriesDataIndex.seriesIndex);
    if (series) {
      const values = series.getData().getValues(seriesDataIndex.dataIndex);
      const start = values[2];
      const end = values[3];
      if (typeof start === 'number' && typeof end === 'number') {
        interval = Math.max(end - start, 1);
      }
    }
  }
  if (!interval) {
    const tbTimeWindow: WidgetTimewindow = (axisOption as any).tbTimeWindow;
    if (isDefinedAndNotNull(tbTimeWindow)) {
      if (axisOption.axisPointer?.value && typeof axisOption.axisPointer?.value === 'number') {
        const intervalArray = calculateAggIntervalWithWidgetTimeWindow(tbTimeWindow, axisOption.axisPointer.value);
        const start = intervalArray[0];
        const end = intervalArray[1];
        interval = Math.max(end - start, 1);
      } else {
        interval = IntervalMath.numberValue(tbTimeWindow.interval);
      }
    }
  }
  if (interval) {
    const timeScale = model.axis.scale;
    const axisExtent = model.axis.getExtent();
    const dataExtent = timeScale.getExtent();
    const size = Math.abs(axisExtent[1] - axisExtent[0]);
    return interval * (size / (dataExtent[1] - dataExtent[0]));
  }
};

export const getAxis = (chart: ECharts, mainType: string, axisId: string): Axis2D => {
  const model: GlobalModel = (chart as any).getModel();
  const models = model.queryComponents({mainType, id: axisId});
  if (models?.length) {
    const axisModel = models[0] as AxisModel;
    return axisModel.axis;
  }
  return null;
};

export const calculateAxisSize = (chart: ECharts, mainType: string, axisId: string): number => {
  const axis = getAxis(chart, mainType, axisId);
  return _calculateAxisSize(axis);
};

export const measureAxisNameSize = (chart: ECharts, mainType: string, axisId: string, name: string): number => {
  const axis = getAxis(chart, mainType, axisId);
  if (axis) {
    return axis.model.getModel('nameTextStyle').getTextRect(name).height;
  }
  return 0;
};

const _calculateAxisSize = (axis: Axis2D): number => {
  let size = 0;
  if (axis && axis.model.option.show) {
    const labelUnionRect = estimateLabelUnionRect(axis);
    if (labelUnionRect) {
      const margin = axis.model.get(['axisLabel', 'margin']);
      const dimension = axis.isHorizontal() ? 'height' : 'width';
      size += labelUnionRect[dimension] + margin;
    }
    if (!axis.scale.isBlank() && axis.model.get(['axisTick', 'show'])) {
      const tickLength = axis.model.get(['axisTick', 'length']);
      size += tickLength;
    }
  }
  return size;
};

const measureSymbolOffset = (symbol: string, symbolSize: any): number => {
  if (isNumber(symbolSize)) {
    if (symbol) {
      const offsetFunction = timeSeriesChartShapeOffsetFunctions.get(symbol as EChartsShape);
      if (offsetFunction) {
        return offsetFunction(symbolSize);
      } else {
        return symbolSize / 2;
      }
    }
  } else {
    return 0;
  }
};

export const measureThresholdOffset = (chart: ECharts, axisId: string, thresholdId: string, value: any): [number, number] => {
  const offset: [number, number] = [0,0];
  const axis = getAxis(chart, 'yAxis', axisId);
  if (axis && !axis.scale.isBlank()) {
    const extent = axis.scale.getExtent();
    const model: GlobalModel = (chart as any).getModel();
    const models = model.queryComponents({mainType: 'series', id: thresholdId});
    if (models?.length) {
      const lineSeriesModel = models[0] as SeriesModel<LineSeriesOption>;
      const markLineModel = lineSeriesModel.getModel('markLine');
      const dataOption = markLineModel.get('data');
      for (const dataItemOption of dataOption) {
        const dataItem = dataItemOption as MarkLine2DDataItemOption;
        const start = dataItem[0];
        const startOffset = measureSymbolOffset(start.symbol, start.symbolSize);
        offset[0] = Math.max(offset[0], startOffset);
        const end = dataItem[1];
        const endOffset = measureSymbolOffset(end.symbol, end.symbolSize);
        offset[1] = Math.max(offset[1], endOffset);
      }
      const labelPosition = markLineModel.get(['label', 'position']);
      if (labelPosition === 'start' || labelPosition === 'end') {
        const labelModel = markLineModel.getModel('label');
        const formatter = markLineModel.get(['label', 'formatter']);
        let textWidth = 0;
        if (Array.isArray(value)) {
          for (const val of value) {
            if (val >= extent[0] && val <= extent[1]) {
              const textVal = typeof formatter === 'string' ? formatter : formatter({value: val} as CallbackDataParams);
              textWidth = Math.max(textWidth, labelModel.getTextRect(textVal).width);
            }
          }
        } else {
          if (value >= extent[0] && value <= extent[1]) {
            const textVal = typeof formatter === 'string' ? formatter : formatter({value} as CallbackDataParams);
            textWidth = labelModel.getTextRect(textVal).width;
          }
        }
        if (!textWidth) {
          return offset;
        }
        const distanceOpt = markLineModel.get(['label', 'distance']);
        let distance = 5;
        if (distanceOpt) {
          distance = typeof distanceOpt === 'number' ? distanceOpt : distanceOpt[0];
        }
        const paddingOpt = markLineModel.get(['label', 'padding']);
        let leftPadding = 0;
        let rightPadding = 0;
        if (paddingOpt) {
          if (Array.isArray(paddingOpt)) {
            if (paddingOpt.length === 4) {
              leftPadding = paddingOpt[3];
              rightPadding = paddingOpt[1];
            } else if (paddingOpt.length === 2) {
              leftPadding = rightPadding = paddingOpt[1];
            }
          } else {
            leftPadding = rightPadding = paddingOpt;
          }
        }
        const textOffset = distance + textWidth + leftPadding + rightPadding;
        if (labelPosition === 'start') {
          offset[0] = Math.max(offset[0], textOffset);
        } else {
          offset[1] = Math.max(offset[1], textOffset);
        }
      }
    }
  }
  return offset;
};

export const getAxisExtent = (chart: ECharts, axisId: string): [number, number] => {
  const axis = getAxis(chart, 'yAxis', axisId);
  if (axis) {
    return axis.scale.getExtent();
  }
  return [0,0];
};

let componentBlurredKey: string;

const isBlurred = (model: SeriesModel): boolean => {
  if (!componentBlurredKey) {
    const innerKeys = Object.keys(model).filter(k => k.startsWith('__ec_inner_'));
    for (const k of innerKeys) {
      const obj = model[k];
      if (obj.hasOwnProperty('isBlured')) {
        componentBlurredKey = k;
        break;
      }
    }
  }
  if (componentBlurredKey) {
    const obj = model[componentBlurredKey];
    return !!obj?.isBlured;
  } else {
    return false;
  }
};

export const getFocusedSeriesIndex = (chart: ECharts): number => {
  const model: GlobalModel = (chart as any).getModel();
  const models = model.queryComponents({mainType: 'series'});
  if (models) {
    let hasBlurred = false;
    let notBlurredIndex = -1;
    for (const _model of models) {
      const seriesModel = _model as SeriesModel;
      const blurred = isBlurred(seriesModel);
      if (!blurred) {
        notBlurredIndex = seriesModel.seriesIndex;
      }
      hasBlurred = blurred || hasBlurred;
    }
    if (hasBlurred) {
      return notBlurredIndex;
    }
  }
  return -1;
};

export const toNamedData = (data: DataSet, valueConverter?: (value: any) => any): NamedDataSet => {
  if (!data?.length) {
    return [];
  } else {
    return data.map(d => {
      const ts = isDefinedAndNotNull(d[2]) ? d[2][0] : d[0];
      return {
        name: ts + '',
        value: toEChartsDataItem(d, valueConverter)
      };
    });
  }
};

const minDataTs = (dataSet: NamedDataSet): number => dataSet.length ? dataSet.map(data =>
  Number(data.name)).reduce((a, b) => Math.min(a, b)) : undefined;

const maxDataTs = (dataSet: NamedDataSet): number => dataSet.length ? dataSet.map(data =>
  Number(data.name)).reduce((a, b) => Math.max(a, b)) : undefined;

export const adjustTimeAxisExtentToData = (timeAxisOption: TimeAxisBaseOption,
                                           dataItems: EChartsSeriesItem[],
                                           defaultMin: number,
                                           defaultMax: number): void => {
  let min: number;
  let max: number;
  for (const item of dataItems) {
    if (item.enabled) {
      const minTs = minDataTs(item.data);
      if (typeof minTs !== 'undefined') {
        min = (typeof min !== 'undefined') ? Math.min(min, minTs) : minTs;
      }
      const maxTs = maxDataTs(item.data);
      if (typeof maxTs !== 'undefined') {
        max = (typeof max !== 'undefined') ? Math.max(max, maxTs) : maxTs;
      }
    }
  }
  timeAxisOption.min = (typeof min !== 'undefined') ? min : defaultMin;
  timeAxisOption.max = (typeof max !== 'undefined') ? max : defaultMax;
};

const toEChartsDataItem = (entry: DataEntry, valueConverter?: (value: any) => any): EChartsDataItem => {
  const value = valueConverter ? valueConverter(entry[1]) : entry[1];
  const item: EChartsDataItem = [entry[0], value, entry[0], entry[0]];
  if (isDefinedAndNotNull(entry[2])) {
    item[2] = entry[2][0];
    item[3] = entry[2][1];
  }
  return item;
};

export enum EChartsTooltipTrigger {
  point = 'point',
  axis = 'axis'
}

export const tooltipTriggerTranslationMap = new Map<EChartsTooltipTrigger, string>(
  [
    [ EChartsTooltipTrigger.point, 'tooltip.trigger-point' ],
    [ EChartsTooltipTrigger.axis, 'tooltip.trigger-axis' ]
  ]
);

export interface EChartsTooltipWidgetSettings {
  showTooltip: boolean;
  tooltipTrigger?: EChartsTooltipTrigger;
  tooltipShowFocusedSeries?: boolean;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  tooltipValueFormatter?: string | EChartsTooltipValueFormatFunction;
  tooltipShowDate: boolean;
  tooltipDateInterval?: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
}

export const createTooltipValueFormatFunction =
  (tooltipValueFormatter: string | EChartsTooltipValueFormatFunction): EChartsTooltipValueFormatFunction => {
  let tooltipValueFormatFunction: EChartsTooltipValueFormatFunction;
  if (isFunction(tooltipValueFormatter)) {
    tooltipValueFormatFunction = tooltipValueFormatter as EChartsTooltipValueFormatFunction;
  } else if (typeof tooltipValueFormatter === 'string' && tooltipValueFormatter.length) {
    try {
      tooltipValueFormatFunction =
        new Function('value', 'latestData', tooltipValueFormatter) as EChartsTooltipValueFormatFunction;
    } catch (e) {}
  }
  return tooltipValueFormatFunction;
};

export const echartsTooltipFormatter = (renderer: Renderer2,
                                        tooltipDateFormat: DateFormatProcessor,
                                        settings: EChartsTooltipWidgetSettings,
                                        params: CallbackDataParams[] | CallbackDataParams,
                                        valueFormatFunction: EChartsTooltipValueFormatFunction,
                                        focusedSeriesIndex: number,
                                        series?: EChartsSeriesItem[],
                                        interval?: Interval): null | HTMLElement => {

  const tooltipParams = mapTooltipParams(params, series, focusedSeriesIndex);
  if (!tooltipParams.items.length && !tooltipParams.comparisonItems.length) {
    return null;
  }

  const tooltipElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(tooltipElement, 'display', 'flex');
  renderer.setStyle(tooltipElement, 'flex-direction', 'column');
  renderer.setStyle(tooltipElement, 'align-items', 'flex-start');
  renderer.setStyle(tooltipElement, 'gap', '16px');

  buildItemsTooltip(tooltipElement, tooltipParams.items, renderer, tooltipDateFormat, settings, valueFormatFunction, interval);
  buildItemsTooltip(tooltipElement, tooltipParams.comparisonItems, renderer, tooltipDateFormat, settings, valueFormatFunction, interval);

  return tooltipElement;
};

interface TooltipItem {
  param: CallbackDataParams;
  dataItem: EChartsSeriesItem;
}

interface TooltipParams {
  items: TooltipItem[];
  comparisonItems: TooltipItem[];
}

const buildItemsTooltip = (tooltipElement: HTMLElement,
                           items: TooltipItem[],
                           renderer: Renderer2,
                           tooltipDateFormat: DateFormatProcessor,
                           settings: EChartsTooltipWidgetSettings,
                           valueFormatFunction: EChartsTooltipValueFormatFunction,
                           interval?: Interval) => {
  if (items.length) {
    const tooltipItemsElement: HTMLElement = renderer.createElement('div');
    renderer.setStyle(tooltipItemsElement, 'display', 'flex');
    renderer.setStyle(tooltipItemsElement, 'flex-direction', 'column');
    renderer.setStyle(tooltipItemsElement, 'align-items', 'flex-start');
    renderer.setStyle(tooltipItemsElement, 'gap', '4px');
    renderer.appendChild(tooltipElement, tooltipItemsElement);
    if (settings.tooltipShowDate) {
      renderer.appendChild(tooltipItemsElement,
        constructEchartsTooltipDateElement(renderer, tooltipDateFormat, settings, items[0].param, interval));
    }
    for (const item of items) {
      renderer.appendChild(tooltipItemsElement,
        constructEchartsTooltipSeriesElement(renderer, settings, item, valueFormatFunction));
    }
  }
};

const mapTooltipParams = (params: CallbackDataParams[] | CallbackDataParams,
                          series?: EChartsSeriesItem[],
                          focusedSeriesIndex?: number): TooltipParams => {
  const result: TooltipParams = {
    items: [],
    comparisonItems: []
  };
  if (!params || Array.isArray(params) && !params[0]) {
    return result;
  }
  const firstParam = Array.isArray(params) ? params[0] : params;
  if (!firstParam.value) {
    return result;
  }
  let seriesParams: CallbackDataParams = null;
  if (Array.isArray(params) && focusedSeriesIndex > -1) {
    seriesParams = params.find(param => param.seriesIndex === focusedSeriesIndex);
  } else if (!Array.isArray(params)) {
    seriesParams = params;
  }
  if (seriesParams) {
    appendTooltipItem(result, seriesParams, series);
  } else if (Array.isArray(params)) {
    for (seriesParams of params) {
      appendTooltipItem(result, seriesParams, series);
    }
  }
  return result;
};

const appendTooltipItem = (tooltipParams: TooltipParams, seriesParams: CallbackDataParams, series?: EChartsSeriesItem[]) => {
  const dataItem = series?.find(s => s.id === seriesParams.seriesId);
  const tooltipItem: TooltipItem = {
    param: seriesParams,
    dataItem
  };
  if (dataItem?.comparisonItem) {
    tooltipParams.comparisonItems.push(tooltipItem);
  } else {
    tooltipParams.items.push(tooltipItem);
  }
};

const constructEchartsTooltipDateElement = (renderer: Renderer2,
                                            tooltipDateFormat: DateFormatProcessor,
                                            settings: EChartsTooltipWidgetSettings,
                                            param: CallbackDataParams,
                                            interval?: Interval): HTMLElement => {
  const dateElement: HTMLElement = renderer.createElement('div');
  let dateText: string;
  const startTs = param.value[2];
  const endTs = param.value[3];
  if (settings.tooltipDateInterval && startTs && endTs && (endTs - 1) > startTs) {
    const startDateText = tooltipDateFormat.update(startTs, interval);
    const endDateText = tooltipDateFormat.update(endTs - 1, interval);
    if (startDateText === endDateText) {
      dateText = startDateText;
    } else {
      dateText = startDateText + ' - ' + endDateText;
    }
  } else {
    const ts = param.value[0];
    dateText = tooltipDateFormat.update(ts, interval);
  }
  renderer.appendChild(dateElement, renderer.createText(dateText));
  renderer.setStyle(dateElement, 'font-family', settings.tooltipDateFont.family);
  renderer.setStyle(dateElement, 'font-size', settings.tooltipDateFont.size + settings.tooltipDateFont.sizeUnit);
  renderer.setStyle(dateElement, 'font-style', settings.tooltipDateFont.style);
  renderer.setStyle(dateElement, 'font-weight', settings.tooltipDateFont.weight);
  renderer.setStyle(dateElement, 'line-height', settings.tooltipDateFont.lineHeight);
  renderer.setStyle(dateElement, 'color', settings.tooltipDateColor);
  return dateElement;
};

const constructEchartsTooltipSeriesElement = (renderer: Renderer2,
                                              settings: EChartsTooltipWidgetSettings,
                                              item: TooltipItem,
                                              valueFormatFunction: EChartsTooltipValueFormatFunction): HTMLElement => {
  const labelValueElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(labelValueElement, 'display', 'flex');
  renderer.setStyle(labelValueElement, 'flex-direction', 'row');
  renderer.setStyle(labelValueElement, 'align-items', 'center');
  renderer.setStyle(labelValueElement, 'align-self', 'stretch');
  renderer.setStyle(labelValueElement, 'gap', '12px');
  const labelElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(labelElement, 'display', 'flex');
  renderer.setStyle(labelElement, 'align-items', 'center');
  renderer.setStyle(labelElement, 'gap', '8px');
  renderer.appendChild(labelValueElement, labelElement);
  const circleElement: HTMLElement = renderer.createElement('div');
  renderer.setStyle(circleElement, 'width', '8px');
  renderer.setStyle(circleElement, 'height', '8px');
  renderer.setStyle(circleElement, 'border-radius', '50%');
  renderer.setStyle(circleElement, 'background', item.param.color);
  renderer.appendChild(labelElement, circleElement);
  const labelTextElement: HTMLElement = renderer.createElement('div');
  renderer.appendChild(labelTextElement, renderer.createText(item.param.seriesName));
  renderer.setStyle(labelTextElement, 'font-family', 'Roboto');
  renderer.setStyle(labelTextElement, 'font-size', '12px');
  renderer.setStyle(labelTextElement, 'font-style', 'normal');
  renderer.setStyle(labelTextElement, 'font-weight', '400');
  renderer.setStyle(labelTextElement, 'line-height', '16px');
  renderer.setStyle(labelTextElement, 'letter-spacing', '0.4px');
  renderer.setStyle(labelTextElement, 'color', 'rgba(0, 0, 0, 0.76)');
  renderer.appendChild(labelElement, labelTextElement);
  const valueElement: HTMLElement = renderer.createElement('div');
  let formatFunction = valueFormatFunction;
  let latestData: FormattedData;
  let units = '';
  let decimals = 0;
  if (item.dataItem) {
    if (item.dataItem.tooltipValueFormatFunction) {
      formatFunction = item.dataItem.tooltipValueFormatFunction;
    }
    latestData = item.dataItem.latestData;
    units = item.dataItem.units;
    decimals = item.dataItem.decimals;
  }
  if (!latestData) {
    latestData = {} as FormattedData;
  }
  const value = formatFunction(item.param.value[1], latestData, units, decimals);
  renderer.appendChild(valueElement, renderer.createText(value));
  renderer.setStyle(valueElement, 'flex', '1');
  renderer.setStyle(valueElement, 'text-align', 'end');
  renderer.setStyle(valueElement, 'font-family', settings.tooltipValueFont.family);
  renderer.setStyle(valueElement, 'font-size', settings.tooltipValueFont.size + settings.tooltipValueFont.sizeUnit);
  renderer.setStyle(valueElement, 'font-style', settings.tooltipValueFont.style);
  renderer.setStyle(valueElement, 'font-weight', settings.tooltipValueFont.weight);
  renderer.setStyle(valueElement, 'line-height', settings.tooltipValueFont.lineHeight);
  renderer.setStyle(valueElement, 'color', settings.tooltipValueColor);
  renderer.appendChild(labelValueElement, valueElement);
  return labelValueElement;
};
