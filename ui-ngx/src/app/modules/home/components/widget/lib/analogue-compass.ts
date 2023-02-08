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

import { WidgetContext } from '@home/models/widget-component.models';
import * as CanvasGauges from 'canvas-gauges';
import {
  AnalogueCompassSettings
} from '@home/components/widget/lib/analogue-compass.models';
import { deepClone, isDefined } from '@core/utils';
import { getFontFamily } from '@home/components/widget/lib/settings.models';
import { TbBaseGauge } from '@home/components/widget/lib/analogue-gauge.models';
import RadialGaugeOptions = CanvasGauges.RadialGaugeOptions;
import BaseGauge = CanvasGauges.BaseGauge;
import RadialGauge = CanvasGauges.RadialGauge;

// @dynamic
export class TbAnalogueCompass extends TbBaseGauge<AnalogueCompassSettings, RadialGaugeOptions> {

  constructor(ctx: WidgetContext, canvasId: string) {
    super(ctx, canvasId);
  }

  protected createGaugeOptions(gaugeElement: HTMLElement, settings: AnalogueCompassSettings): RadialGaugeOptions {

    const majorTicks = (settings.majorTicks && settings.majorTicks.length > 0) ? deepClone(settings.majorTicks) :
      ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    majorTicks.push(majorTicks[0]);

    return {
      renderTo: gaugeElement,

      // Generic options
      minValue: 0,
      maxValue: 360,
      majorTicks,
      minorTicks: settings.minorTicks || 22,
      ticksAngle: 360,
      startAngle: 180,
      strokeTicks: settings.showStrokeTicks || false,
      highlights: [],
      valueBox: false,

      // needle
      needleCircleSize: settings.needleCircleSize || 15,
      needleType: 'line',
      needleStart: 75,
      needleEnd: 99,
      needleWidth: 3,
      needleCircleOuter: false,

      // borders
      borders: settings.showBorder || false,
      borderInnerWidth: 0,
      borderMiddleWidth: 0,
      borderOuterWidth: settings.borderOuterWidth || 10,
      borderShadowWidth: 0,

      // colors
      colorPlate: settings.colorPlate || '#222',
      colorMajorTicks: settings.colorMajorTicks || '#f5f5f5',
      colorMinorTicks: settings.colorMinorTicks || '#ddd',
      colorNeedle: settings.colorNeedle || '#f08080',
      colorNeedleEnd: settings.colorNeedle || '#f08080',
      colorNeedleCircleInner: settings.colorNeedleCircle || '#e8e8e8',
      colorNeedleCircleInnerEnd: settings.colorNeedleCircle || '#e8e8e8',
      colorBorderOuter: settings.colorBorder || '#ccc',
      colorBorderOuterEnd: settings.colorBorder || '#ccc',
      colorNeedleShadowDown: '#222',

      // fonts
      fontNumbers: getFontFamily(settings.majorTickFont),
      fontNumbersSize: settings.majorTickFont && settings.majorTickFont.size ? settings.majorTickFont.size : 20,
      fontNumbersStyle: settings.majorTickFont && settings.majorTickFont.style ? settings.majorTickFont.style : 'normal',
      fontNumbersWeight: settings.majorTickFont && settings.majorTickFont.weight ? settings.majorTickFont.weight : '500',
      colorNumbers: settings.majorTickFont && settings.majorTickFont.color ? settings.majorTickFont.color : '#ccc',

      // animations
      animation: settings.animation !== false && !this.ctx.isMobile,
      animationDuration: (isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
      animationRule: settings.animationRule || 'cycle',
      animationTarget: settings.animationTarget || 'needle'
    };
  }

  protected createGauge(gaugeData: RadialGaugeOptions): BaseGauge {
    return new RadialGauge(gaugeData);
  }
}
