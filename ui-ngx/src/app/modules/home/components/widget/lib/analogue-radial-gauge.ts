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
import {
  AnalogueRadialGaugeSettings
} from '@home/components/widget/lib/analogue-radial-gauge.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbAnalogueGauge } from '@home/components/widget/lib/analogue-gauge.models';
import RadialGauge = CanvasGauges.RadialGauge;
import RadialGaugeOptions = CanvasGauges.RadialGaugeOptions;
import BaseGauge = CanvasGauges.BaseGauge;

// @dynamic
export class TbAnalogueRadialGauge extends TbAnalogueGauge<AnalogueRadialGaugeSettings, RadialGaugeOptions>{

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected prepareGaugeOptions(settings: AnalogueRadialGaugeSettings, gaugeData: RadialGaugeOptions) {
    gaugeData.ticksAngle = settings.ticksAngle || 270;
    gaugeData.startAngle = settings.startAngle || 45;

    // colors

    gaugeData.colorNeedleCircleOuter = '#f0f0f0';
    gaugeData.colorNeedleCircleOuterEnd = '#ccc';
    gaugeData.colorNeedleCircleInner = '#e8e8e8'; // tinycolor(keyColor).lighten(30).toRgbString(),//'#e8e8e8',
    gaugeData.colorNeedleCircleInnerEnd = '#f5f5f5';

    // needle
    gaugeData.needleCircleSize = settings.needleCircleSize || 10;
    gaugeData.needleCircleInner = true;
    gaugeData.needleCircleOuter = true;

    // custom animations
    gaugeData.animationTarget = 'needle'; // 'needle' or 'plate'
  }

  protected createGauge(gaugeData: RadialGaugeOptions): BaseGauge {
    return new RadialGauge(gaugeData);
  }

}
