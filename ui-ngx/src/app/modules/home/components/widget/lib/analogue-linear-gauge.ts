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

import * as CanvasGauges from 'canvas-gauges';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbAnalogueGauge } from '@home/components/widget/lib/analogue-gauge.models';
import {
  AnalogueLinearGaugeSettings
} from '@home/components/widget/lib/analogue-linear-gauge.models';
import { isDefined } from '@core/utils';
import tinycolor from 'tinycolor2';
import LinearGaugeOptions = CanvasGauges.LinearGaugeOptions;
import LinearGauge = CanvasGauges.LinearGauge;
import BaseGauge = CanvasGauges.BaseGauge;

// @dynamic
export class TbAnalogueLinearGauge extends TbAnalogueGauge<AnalogueLinearGaugeSettings, LinearGaugeOptions>{

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected prepareGaugeOptions(settings: AnalogueLinearGaugeSettings, gaugeData: LinearGaugeOptions) {
    const dataKey = this.ctx.data[0].dataKey;
    const keyColor = settings.defaultColor || dataKey.color;

    const barStrokeColor = tinycolor(keyColor).darken().setAlpha(0.6).toRgbString();
    const progressColorStart = tinycolor(keyColor).setAlpha(0.05).toRgbString();
    const progressColorEnd = tinycolor(keyColor).darken().toRgbString();

    gaugeData.barStrokeWidth = (isDefined(settings.barStrokeWidth) && settings.barStrokeWidth !== null) ? settings.barStrokeWidth : 2.5;
    gaugeData.colorBarStroke = settings.colorBarStroke || barStrokeColor;
    gaugeData.colorBar = settings.colorBar || '#fff';
    gaugeData.colorBarEnd = settings.colorBarEnd || '#ddd';
    gaugeData.colorBarProgress = settings.colorBarProgress || progressColorStart;
    gaugeData.colorBarProgressEnd = settings.colorBarProgressEnd || progressColorEnd;
  }

  protected createGauge(gaugeData: LinearGaugeOptions): BaseGauge {
    return new LinearGauge(gaugeData);
  }

}
