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

import * as echarts from 'echarts/core';
import AxisModel from 'echarts/types/src/coord/cartesian/AxisModel';
import { estimateLabelUnionRect } from 'echarts/lib/coord/axisHelper';
import {
  DataZoomComponent,
  DataZoomComponentOption,
  GridComponent,
  GridComponentOption,
  MarkLineComponent,
  MarkLineComponentOption,
  PolarComponent,
  PolarComponentOption,
  RadarComponent,
  RadarComponentOption,
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
  PieSeriesOption,
  RadarChart,
  RadarSeriesOption
} from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer, SVGRenderer } from 'echarts/renderers';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import GlobalModel from 'echarts/types/src/model/Global';
import Axis2D from 'echarts/types/src/coord/cartesian/Axis2D';
import SeriesModel from 'echarts/types/src/model/Series';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';
import { measureSymbolOffset } from '@home/components/widget/lib/chart/chart.models';

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
        PolarComponent,
        RadarComponent,
        LineChart,
        BarChart,
        PieChart,
        RadarChart,
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
  | PolarComponentOption
  | RadarComponentOption
  | LineSeriesOption
  | CustomSeriesOption
  | BarSeriesOption
  | PieSeriesOption
  | RadarSeriesOption
>;

export type ECharts = echarts.ECharts;

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
