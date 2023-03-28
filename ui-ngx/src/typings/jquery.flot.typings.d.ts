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


interface JQueryPlot extends jquery.flot.plot {
  destroy(): void;
  highlight(series: jquery.flot.dataSeries | number, datapoint: jquery.flot.item | number): void;
  clearSelection(): void;
}

interface JQueryPlotPoint extends jquery.flot.point {
  pageX: number;
  pageY: number;
  x2: number;
}

interface JQueryPlotDataSeries extends jquery.flot.dataSeries, JQueryPlotSeriesOptions {
  datapoints?: jquery.flot.datapoints;
}

interface JQueryPlotOptions extends jquery.flot.plotOptions {
  title?: string;
  subtitile?: string;
  shadowSize?: number;
  HtmlText?: boolean;
  selection?: JQueryPlotSelection;
  xaxis?: JQueryPlotAxisOptions;
  series?: JQueryPlotSeriesOptions;
  crosshair?: JQueryPlotCrosshairOptions;
}

interface JQueryPlotAxisOptions extends jquery.flot.axisOptions {
  label?: string;
  labelFont?: any;
}

interface JQueryPlotAxis extends jquery.flot.axis, JQueryPlotAxisOptions {
  options: JQueryPlotAxisOptions;
}

interface JQueryPlotSeriesOptions extends jquery.flot.seriesOptions {
  stack?: boolean;
  curvedLines?: JQueryPlotCurvedLinesOptions;
  pie?: JQueryPlotPieOptions;
}

declare type JQueryPlotCrosshairMode = 'x' | 'y' | 'xy' | null;

interface JQueryPlotCrosshairOptions {
  mode?: JQueryPlotCrosshairMode;
  color?: string;
  lineWidth?: number;
}

interface JQueryPlotCurvedLinesOptions {
  active?: boolean;
  apply?: boolean;
  monotonicFit?: boolean;
  tension?: number;
  nrSplinePoints?: number;
  legacyOverride?: any;
}

interface JQueryPlotPieOptions {
  show?: boolean;
  radius?: any;
  innerRadius?: any;
  startAngle?: number;
  tilt?: number;
  offset?: {
    top?: number;
    left?: number;
  };
  stroke?: {
    color?: string;
    width?: number;
  };
  shadow?: {
    top?: number;
    left?: number;
    alpha?: number;
  };
  label?: {
    show?: boolean;
    formatter?: (label: string, slice?: any) => string;
    radius?: any;
    background?: {
      color?: string;
      opacity?: number;
    };
    threshold?: number;
  };
  combine?: {
    threshold?: number;
    color?: string;
    label?: string;
  };
  highlight?: number;
}

declare type JQueryPlotSelectionMode = 'x' | 'y' | 'xy' | null;
declare type JQueryPlotSelectionShape = 'round' | 'mitter' | 'bevel';

interface JQueryPlotSelection {
  mode?: JQueryPlotSelectionMode;
  color?: string;
  shape?: JQueryPlotSelectionShape;
  minSize?: number;
}

interface JQueryPlotSelectionRanges {
  [axis: string]: {
    from: number;
    to: number;
  };
}
