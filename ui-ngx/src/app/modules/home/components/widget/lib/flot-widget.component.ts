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

import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { ChartType, TbFlotSettings } from '@home/components/widget/lib/flot-widget.models';
import { TbFlot } from '@home/components/widget/lib/flot-widget';
import {
  defaultLegendConfig,
  LegendConfig,
  LegendData,
  LegendPosition,
  widgetType
} from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-flot-widget',
  templateUrl: './flot-widget.component.html',
  styleUrls: []
})
export class FlotWidgetComponent implements OnInit {

  @ViewChild('flotElement', {static: true}) flotElement: ElementRef;

  @Input()
  ctx: WidgetContext;

  @Input()
  chartType: ChartType;

  displayLegend: boolean;
  legendConfig: LegendConfig;
  legendData: LegendData;
  isLegendFirst: boolean;
  legendContainerLayoutType: string;
  legendStyle: {[klass: string]: any};

  public settings: TbFlotSettings;
  private flot: TbFlot;

  constructor() {
  }

  ngOnInit(): void {
    this.ctx.$scope.flotWidget = this;
    this.settings = this.ctx.settings;
    this.chartType = this.chartType || 'line';
    this.configureLegend();
    this.flot = new TbFlot(this.ctx, this.chartType, $(this.flotElement.nativeElement));
  }

  private configureLegend(): void {

    this.displayLegend = isDefinedAndNotNull(this.settings.showLegend) ? this.settings.showLegend
      : false;

    this.legendContainerLayoutType = 'column';

    if (this.displayLegend) {
      this.legendConfig = this.settings.legendConfig || defaultLegendConfig(widgetType.timeseries);
      if (this.ctx.defaultSubscription) {
        this.legendData = this.ctx.defaultSubscription.legendData;
      } else {
        this.legendData = {
          keys: [],
          data: []
        };
      }
      if (this.legendConfig.position === LegendPosition.top ||
        this.legendConfig.position === LegendPosition.bottom) {
        this.legendContainerLayoutType = 'column';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.top;
      } else {
        this.legendContainerLayoutType = 'row';
        this.isLegendFirst = this.legendConfig.position === LegendPosition.left;
      }
      switch (this.legendConfig.position) {
        case LegendPosition.top:
          this.legendStyle = {
            paddingBottom: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.bottom:
          this.legendStyle = {
            paddingTop: '8px',
            maxHeight: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.left:
          this.legendStyle = {
            paddingRight: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
        case LegendPosition.right:
          this.legendStyle = {
            paddingLeft: '0px',
            maxWidth: '50%',
            overflowY: 'auto'
          };
          break;
      }
    }
  }

  public onLegendKeyHiddenChange(index: number) {
    for (const id of Object.keys(this.ctx.subscriptions)) {
      const subscription = this.ctx.subscriptions[id];
      subscription.updateDataVisibility(index);
    }
  }

  public onDataUpdated() {
    this.flot.update();
  }

  public onLatestDataUpdated() {
    this.flot.latestDataUpdate();
  }

  public onResize() {
    this.flot.resize();
  }

  public onEditModeChanged() {
    this.flot.checkMouseEvents();
  }

  public onDestroy() {
    this.flot.destroy();
  }

}
