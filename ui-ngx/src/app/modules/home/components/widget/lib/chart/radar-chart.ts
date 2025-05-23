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

import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { radarChartDefaultSettings, RadarChartSettings } from '@home/components/widget/lib/chart/radar-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { Renderer2 } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  ChartFillType,
  createChartTextStyle,
  createRadialOpacityGradient,
  toAnimationOption
} from '@home/components/widget/lib/chart/chart.models';
import { isDefinedAndNotNull, plainColorFromVariable } from '@core/utils';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { AreaStyleOption, SeriesLabelOption } from 'echarts/types/src/util/types';
import { RadarIndicatorOption } from 'echarts/types/src/coord/radar/RadarModel';
import { DataKey } from '@shared/models/widget.models';

export class TbRadarChart extends TbLatestChart<RadarChartSettings> {

  constructor(ctx: WidgetContext,
              inputSettings: DeepPartial<RadarChartSettings>,
              chartElement: HTMLElement,
              renderer: Renderer2,
              translate: TranslateService,
              autoResize = true) {

    super(ctx, inputSettings, chartElement, renderer, translate, autoResize);
  }

  protected defaultSettings(): RadarChartSettings {
    return radarChartDefaultSettings;
  }

  protected prepareLatestChartOption() {

    const axisNameStyle = createChartTextStyle(this.settings.axisLabelFont,
      '#000', false, 'axis.label');
    const axisTickLabelStyle = createChartTextStyle(this.settings.axisTickLabelFont,
      this.settings.axisTickLabelColor, false, 'axis.tickLabel');

    this.latestChartOption.radar = [{
      shape: this.settings.shape,
      radius: '85%',
      indicator: [{}],
      axisName: {
        show: this.settings.axisShowLabel,
        fontStyle: axisNameStyle.fontStyle,
        fontWeight: axisNameStyle.fontWeight,
        fontFamily: axisNameStyle.fontFamily,
        fontSize: axisNameStyle.fontSize
      },
      axisLabel: {
        show: this.settings.axisShowTickLabels,
        color: axisTickLabelStyle.color,
        fontStyle: axisTickLabelStyle.fontStyle,
        fontWeight: axisTickLabelStyle.fontWeight,
        fontFamily: axisTickLabelStyle.fontFamily,
        fontSize: axisTickLabelStyle.fontSize,
        formatter: (value: any) => this.valueFormatter.format(value)
      }
    }];

    let labelStyle: ComponentStyle = {};
    if (this.settings.showLabel) {
      labelStyle = createChartTextStyle(this.settings.labelFont, this.settings.labelColor, false, 'series.label');
    }

    const labelOption: SeriesLabelOption = {
      show: this.settings.showLabel,
      position: this.settings.labelPosition,
      formatter: (params) => {
        let result = '';
        if (isDefinedAndNotNull(params.value)) {
          result = this.valueFormatter.format(params.value);
        }
        return `{value|${result}}`;
      },
      rich: {
        value: labelStyle
      }
    };

    let areaStyleOption: AreaStyleOption;
    if (this.settings.fillAreaSettings.type !== ChartFillType.none) {
      areaStyleOption = {};
      if (this.settings.fillAreaSettings.type === ChartFillType.opacity) {
        areaStyleOption.opacity = this.settings.fillAreaSettings.opacity;
      } else if (this.settings.fillAreaSettings.type === ChartFillType.gradient) {
        areaStyleOption.opacity = 1;
        areaStyleOption.color = createRadialOpacityGradient(
          plainColorFromVariable(this.settings.color), this.settings.fillAreaSettings.gradient);
      }
    }

    this.latestChartOption.series = [
      {
        type: 'radar',
        data: [{
          id: 1,
          itemStyle: {
            color: plainColorFromVariable(this.settings.color)
          },
          label: labelOption,
          symbol: this.settings.showPoints ? this.settings.pointShape : 'none',
          symbolSize: this.settings.pointSize,
          lineStyle: {
            width: this.settings.showLine ? this.settings.lineWidth : 0,
            type: this.settings.lineType
          },
          areaStyle: areaStyleOption,
          value: []
        }],
        emphasis: {
          focus: 'self'
        },
        ...toAnimationOption(this.ctx, this.settings.animation)
      }
    ];
  }

  protected doUpdateSeriesData() {
    const indicator: RadarIndicatorOption[] = [];
    const value: number[] = [];
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        indicator.push({
          name: dataItem.dataKey.label,
          color: dataItem.dataKey.color
        });
        value.push(dataItem.value);
      }
    }
    if (!indicator.length) {
      indicator.push({});
    }
    this.latestChartOption.radar[0].indicator = indicator;
    this.latestChartOption.series[0].data[0].value = value;
  }

  protected forceRedrawOnResize(): boolean {
    return true;
  }

  public keyEnter(dataKey: DataKey): void {}

  public keyLeave(dataKey: DataKey): void {}

  public toggleKey(dataKey: DataKey): void {
    const enable = dataKey.hidden;
    const dataItem = this.dataItems.find(d => d.dataKey === dataKey);
    if (dataItem) {
      dataItem.enabled = enable;
      this.updateSeriesData();
      dataKey.hidden = !enable;
    }
  }
}
